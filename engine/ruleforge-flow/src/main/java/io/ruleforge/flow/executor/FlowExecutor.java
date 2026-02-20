package io.ruleforge.flow.executor;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.PriorityVector;
import java.time.Instant;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.flow.graph.*;

import java.time.Duration;
import java.util.*;

/**
 * Executes a flow graph by traversing nodes starting from the start node.
 * Uses iterative BFS/DFS respecting edge conditions.
 */
public final class FlowExecutor {

    private final String ruleId;

    public FlowExecutor(String ruleId) {
        this.ruleId = ruleId;
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
                    // Follow matching outgoing edges
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        if (shouldFollowEdge(edge, conditionResult)) {
                            queue.add(edge.toId());
                        }
                    }
                }
                case ActionFlowNode actionNode -> {
                    // Collect outputs and assert them into working memory
                    for (Map.Entry<String, Object> entry : actionNode.outputs().entrySet()) {
                        collectedOutput.put(entry.getKey(), entry.getValue());
                        workingMemory.assertFact(entry.getKey(), entry.getValue(), FactType.ANY);
                    }
                    // Continue to outgoing edges
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case GateFlowNode gateNode -> {
                    // Check if gate conditions are met (simplified: always pass)
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case ForkFlowNode forkNode -> {
                    // Add all outgoing branches
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case JoinFlowNode joinNode -> {
                    // Continue after join
                    for (FlowEdge edge : graph.getOutgoingEdges(nodeId)) {
                        queue.add(edge.toId());
                    }
                }
                case SubRuleFlowNode subRuleNode -> {
                    // Sub-rule invocation (stub: log and continue)
                    collectedOutput.put("_subRuleInvoked", subRuleNode.ruleId());
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

    private boolean evaluateExpression(String expression, WorkingMemory workingMemory) {
        if (expression == null || expression.isBlank() || expression.equals("true")) return true;
        if (expression.equals("false")) return false;

        // Try to resolve simple fact path lookups like "input.approved"
        var fact = workingMemory.getFact(expression);
        if (fact != null) {
            Object val = fact.value();
            if (val instanceof Boolean b) return b;
            if (val instanceof Number n) return n.doubleValue() != 0;
            return val != null;
        }

        // For compound expressions, default to true (full RFL evaluation requires engine integration)
        return true;
    }

    private boolean shouldFollowEdge(FlowEdge edge, boolean conditionResult) {
        String condition = edge.condition();
        if (condition == null || condition.equals("true")) return true;
        if (condition.equals("false")) return !conditionResult; // false edge when condition is false
        // Named conditions: "yes"/"no" style
        return conditionResult;
    }
}
