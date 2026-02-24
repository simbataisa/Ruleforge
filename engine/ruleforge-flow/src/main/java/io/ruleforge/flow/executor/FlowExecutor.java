package io.ruleforge.flow.executor;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.expression.ExpressionEvaluator;
import io.ruleforge.core.expression.Expression;
import io.ruleforge.core.expression.RflParser;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CallableAction;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.PriorityVector;
import java.time.Instant;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.SubRuleInvoker;
import io.ruleforge.flow.graph.*;

import java.time.Duration;
import java.util.*;

/**
 * Executes a flow graph by traversing nodes starting from the start node.
 * Uses iterative BFS/DFS respecting edge conditions.
 */
public final class FlowExecutor {

    private final String ruleId;
    private final SubRuleInvoker subRuleInvoker;
    private final EvaluationContext evaluationContext;
    private final ExpressionEvaluator expressionEvaluator;
    private final RflParser rflParser;

    public FlowExecutor(String ruleId) {
        this(ruleId, null, null);
    }

    public FlowExecutor(String ruleId, SubRuleInvoker subRuleInvoker) {
        this(ruleId, subRuleInvoker, null);
    }

    public FlowExecutor(String ruleId, SubRuleInvoker subRuleInvoker, EvaluationContext evaluationContext) {
        this.ruleId = ruleId;
        this.subRuleInvoker = subRuleInvoker;
        this.evaluationContext = evaluationContext;
        this.expressionEvaluator = new ExpressionEvaluator();
        this.rflParser = new RflParser();
    }

    public RuleResult execute(FlowGraph graph, WorkingMemory workingMemory) {
        long start = System.currentTimeMillis();

        FlowNode startNode = graph.getStartNode();
        if (startNode == null) {
            throw new RuleEvaluationException(ruleId, "Start node not found: " + graph.getStartNodeId());
        }

        Map<String, Object> collectedOutput = new LinkedHashMap<>();
        Set<String> visited = new LinkedHashSet<>();
        int iterations = 0;

        // BFS traversal
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startNode.id());

        while (!queue.isEmpty() && iterations < graph.getMaxIterations()) {
            String nodeId = queue.poll();
            if (!visited.add(nodeId)) continue;
            iterations++;

            FlowNode node = graph.getNode(nodeId);
            if (node == null) continue;

            switch (node) {
                case ConditionFlowNode conditionNode -> {
                    boolean conditionResult = evaluateExpression(conditionNode.expression(), workingMemory);
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        if (shouldFollowEdge(edge, conditionResult)) {
                            queue.add(edge.toId());
                        }
                    }
                }
                case ActionFlowNode actionNode -> {
                    for (Map.Entry<String, Object> entry : actionNode.outputs().entrySet()) {
                        collectedOutput.put(entry.getKey(), entry.getValue());
                        if (workingMemory.hasFact(entry.getKey())) {
                            workingMemory.modifyFact(entry.getKey(), entry.getValue());
                        } else {
                            workingMemory.assertFact(entry.getKey(), entry.getValue(), FactType.ANY);
                        }
                    }
                    // Execute callable actions
                    executeCallableActions(actionNode.callableActions(), workingMemory, collectedOutput);
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case GateFlowNode gateNode -> {
                    boolean gateResult = evaluateGate(gateNode, graph, visited, workingMemory);
                    if (gateResult) {
                        for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                            queue.add(edge.toId());
                        }
                    }
                }
                case ForkFlowNode forkNode -> {
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case JoinFlowNode joinNode -> {
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case SubRuleFlowNode subRuleNode -> {
                    executeSubRule(subRuleNode, workingMemory, collectedOutput);
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        var metadata = new RuleResultMetadata(
                Duration.ofMillis(elapsed),
                iterations,
                collectedOutput.isEmpty() ? 0 : 1,
                0,
                "FLOW"
        );

        var matchedRules = collectedOutput.isEmpty() ? List.<MatchedRule>of() : List.of(
                new MatchedRule(ruleId, "flow", 0,
                        new PriorityVector(0, 0, 1.0, Instant.now(), 0),
                        Map.of(), Map.copyOf(collectedOutput))
        );

        return new RuleResult(collectedOutput.isEmpty() ? Map.of() : Map.copyOf(collectedOutput),
                metadata, matchedRules, null);
    }

    private void executeSubRule(SubRuleFlowNode subRuleNode, WorkingMemory workingMemory,
                                Map<String, Object> collectedOutput) {
        if (subRuleInvoker == null) {
            collectedOutput.put("_subRuleInvoked", subRuleNode.ruleId());
            return;
        }
        RuleResult subResult = subRuleInvoker.invoke(subRuleNode.ruleId(), workingMemory);
        if (subResult != null && subResult.hasOutput()) {
            for (Map.Entry<String, Object> entry : subResult.output().entrySet()) {
                collectedOutput.put(entry.getKey(), entry.getValue());
                if (workingMemory.hasFact(entry.getKey())) {
                    workingMemory.modifyFact(entry.getKey(), entry.getValue());
                } else {
                    workingMemory.assertFact(entry.getKey(), entry.getValue(), FactType.ANY);
                }
            }
        }
    }

    private boolean evaluateGate(GateFlowNode gateNode, FlowGraph graph,
                                  Set<String> visited, WorkingMemory workingMemory) {
        List<FlowEdge> incomingEdges = graph.getIncomingEdges(gateNode.id());
        if (incomingEdges.isEmpty()) {
            return true;
        }

        String gateType = gateNode.gateType();
        if ("AND".equalsIgnoreCase(gateType)) {
            // All incoming source nodes must have been visited
            for (FlowEdge edge : incomingEdges) {
                if (!visited.contains(edge.fromId())) {
                    return false;
                }
            }
            return true;
        } else {
            // OR gate: at least one incoming source node visited
            for (FlowEdge edge : incomingEdges) {
                if (visited.contains(edge.fromId())) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean evaluateExpression(String expression, WorkingMemory workingMemory) {
        if (expression == null || expression.isBlank() || expression.equals("true")) return true;
        if (expression.equals("false")) return false;

        try {
            Expression ast = rflParser.parse(expression);
            return expressionEvaluator.evaluateAsBoolean(ast, workingMemory);
        } catch (Exception e) {
            // Fallback: try simple fact path lookup
            var fact = workingMemory.getFact(expression);
            if (fact != null) {
                Object val = fact.value();
                if (val instanceof Boolean b) return b;
                if (val instanceof Number n) return n.doubleValue() != 0;
                return val != null;
            }
            return true;
        }
    }

    private boolean shouldFollowEdge(FlowEdge edge, boolean conditionResult) {
        String condition = edge.condition();
        if (condition == null || condition.equals("true")) return true;
        if (condition.equals("false")) return !conditionResult;
        return conditionResult;
    }

    private void executeCallableActions(List<CallableAction> actions, WorkingMemory workingMemory,
                                         Map<String, Object> collectedOutput) {
        if (actions == null || actions.isEmpty() || evaluationContext == null) return;

        for (CallableAction action : actions) {
            ActionExecutor executor = evaluationContext.getActionExecutor(action.protocol());
            if (executor == null) continue;

            try {
                Map<String, Object> result = executor.execute(
                        action.target(), action.parameters(), evaluationContext);

                if (result != null && !result.isEmpty()) {
                    if (action.outputMapping() != null && !action.outputMapping().isEmpty()) {
                        collectedOutput.put(action.outputMapping(), result);
                        if (workingMemory.hasFact(action.outputMapping())) {
                            workingMemory.modifyFact(action.outputMapping(), result);
                        } else {
                            workingMemory.assertFact(action.outputMapping(), result, FactType.ANY);
                        }
                    } else {
                        for (var entry : result.entrySet()) {
                            collectedOutput.put(entry.getKey(), entry.getValue());
                            if (workingMemory.hasFact(entry.getKey())) {
                                workingMemory.modifyFact(entry.getKey(), entry.getValue());
                            } else {
                                workingMemory.assertFact(entry.getKey(), entry.getValue(), FactType.ANY);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                collectedOutput.put("_actionError_" + action.actionId(), e.getMessage());
            }
        }
    }
}
