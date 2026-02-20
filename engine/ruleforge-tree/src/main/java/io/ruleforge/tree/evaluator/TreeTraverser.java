package io.ruleforge.tree.evaluator;

import io.ruleforge.core.expression.ExpressionEvaluator;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.result.TraceNode;
import io.ruleforge.tree.compiler.CompiledTreeNode;
import io.ruleforge.tree.compiler.CompiledTreeNode.CompiledActionNode;
import io.ruleforge.tree.compiler.CompiledTreeNode.CompiledConditionNode;
import io.ruleforge.tree.compiler.CompiledTreeNode.CompiledGateNode;
import io.ruleforge.tree.compiler.CompiledTreeNode.CompiledScoreNode;
import io.ruleforge.tree.compiler.CompiledTreeNode.CompiledSubRuleNode;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core traversal engine for compiled decision trees.
 * <p>
 * Performs depth-first traversal of the {@link CompiledTreeNode} graph, evaluating conditions
 * against working memory and collecting outputs, scores, and trace information.
 * <p>
 * Supports two traversal modes:
 * <ul>
 *   <li><b>Standard mode</b> ({@code scoringMode=false}): follows the first matching branch
 *       at each condition node, terminates at the first action node.</li>
 *   <li><b>Scoring mode</b> ({@code scoringMode=true}): evaluates all branches at each condition
 *       node, accumulates scores from score nodes, and aggregates outputs from all paths.</li>
 * </ul>
 * <p>
 * Early termination can be configured to stop traversal at the first action or score node reached.
 */
public final class TreeTraverser {

    private final String ruleId;
    private final boolean scoringMode;
    private final boolean earlyTermination;
    private final ExpressionEvaluator expressionEvaluator;

    /**
     * Creates a new tree traverser.
     *
     * @param ruleId           the rule identifier for trace and matched rule reporting
     * @param scoringMode      whether to traverse all matching branches and accumulate scores
     * @param earlyTermination whether to stop at the first terminal node
     */
    public TreeTraverser(String ruleId, boolean scoringMode, boolean earlyTermination) {
        this.ruleId = ruleId;
        this.scoringMode = scoringMode;
        this.earlyTermination = earlyTermination;
        this.expressionEvaluator = new ExpressionEvaluator();
    }

    /**
     * Traverses the compiled tree starting from the given root node.
     *
     * @param root          the compiled root node
     * @param workingMemory the working memory containing facts to evaluate against
     * @return the rule result containing outputs, matched rules, and trace
     */
    public RuleResult traverse(CompiledTreeNode root, WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        TraversalState state = new TraversalState();

        traverseNode(root, workingMemory, state, 0);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        // Build outputs: include accumulated score in scoring mode
        Map<String, Object> outputs = new LinkedHashMap<>(state.getCollectedOutputs());
        if (scoringMode) {
            outputs.put("_score", state.getAccumulatedScore());
        }

        // Build trace
        DecisionTrace trace = DecisionTrace.builder(ruleId)
                .addFactChanges(workingMemory.getChangelog())
                .build();

        RuleResultMetadata metadata = new RuleResultMetadata(
                elapsed,
                state.getMatchedRules().size(),
                state.getMatchedRules().size(),
                0,
                scoringMode ? "SCORING" : "FIRST_MATCH"
        );

        return new RuleResult(outputs, metadata, state.getMatchedRules(), trace);
    }

    // ------------------------------------------------------------------
    // Node traversal dispatch
    // ------------------------------------------------------------------

    private void traverseNode(CompiledTreeNode node, WorkingMemory wm, TraversalState state, int depth) {
        if (node == null || state.isTerminated()) {
            return;
        }

        switch (node) {
            case CompiledConditionNode condNode -> traverseCondition(condNode, wm, state, depth);
            case CompiledActionNode actionNode -> traverseAction(actionNode, wm, state, depth);
            case CompiledScoreNode scoreNode -> traverseScore(scoreNode, state, depth);
            case CompiledGateNode gateNode -> traverseGate(gateNode, wm, state, depth);
            case CompiledSubRuleNode subRuleNode -> traverseSubRule(subRuleNode, state, depth);
        }
    }

    // ------------------------------------------------------------------
    // Condition node
    // ------------------------------------------------------------------

    private void traverseCondition(CompiledConditionNode node, WorkingMemory wm,
                                   TraversalState state, int depth) {
        Object fieldValue = wm.get(node.field());
        boolean anyBranchMatched = false;

        for (var branch : node.branches()) {
            if (state.isTerminated()) break;

            long branchStart = System.nanoTime();
            boolean matched = ConditionMatcher.evaluate(
                    fieldValue, node.operator(), branch.value(), branch.secondValue());
            Duration evalTime = Duration.ofNanos(System.nanoTime() - branchStart);

            // Record trace
            state.addTraceNode(new TraceNode.ConditionEval(
                    node.field() + " " + node.operator() + " " + branch.value(),
                    fieldValue,
                    node.operator(),
                    branch.value(),
                    matched,
                    evalTime
            ));

            if (matched) {
                anyBranchMatched = true;
                state.addMatchedCondition(
                        node.field() + " " + node.operator() + " " + branch.value());

                traverseNode(branch.child(), wm, state, depth + 1);

                // In non-scoring mode, stop after first match
                if (!scoringMode) {
                    return;
                }
            }
        }

        // If no branch matched, follow the default branch
        if (!anyBranchMatched && node.defaultBranch() != null) {
            state.addTraceNode(new TraceNode.ConditionEval(
                    node.field() + " " + node.operator() + " [default]",
                    fieldValue,
                    node.operator(),
                    "[default]",
                    true,
                    Duration.ZERO
            ));
            traverseNode(node.defaultBranch(), wm, state, depth + 1);
        }
    }

    // ------------------------------------------------------------------
    // Action node (terminal)
    // ------------------------------------------------------------------

    private void traverseAction(CompiledActionNode node, WorkingMemory wm,
                                TraversalState state, int depth) {
        state.addOutputs(node.outputs());

        // Record as a matched rule
        PriorityVector priority = new PriorityVector(0, depth, 1.0, Instant.now(), 0);
        state.addMatchedRule(new MatchedRule(
                ruleId,
                ruleId,
                0,
                priority,
                Map.of("nodeId", node.nodeId(), "conditions", state.getMatchedConditions()),
                node.outputs()
        ));

        // Record trace
        for (var entry : node.outputs().entrySet()) {
            state.addTraceNode(new TraceNode.ActionExec(
                    "SET_OUTPUT",
                    entry.getKey(),
                    null,
                    entry.getValue(),
                    Duration.ZERO
            ));
        }

        // Write outputs to working memory for downstream use
        for (var entry : node.outputs().entrySet()) {
            if (wm.hasFact(entry.getKey())) {
                wm.modifyFact(entry.getKey(), entry.getValue());
            } else {
                wm.assertFact(entry.getKey(), entry.getValue(),
                        io.ruleforge.core.model.FactType.ANY);
            }
        }

        if (earlyTermination) {
            state.terminate();
        }
    }

    // ------------------------------------------------------------------
    // Score node
    // ------------------------------------------------------------------

    private void traverseScore(CompiledScoreNode node, TraversalState state, int depth) {
        state.addScore(node.score());
        state.addOutputs(node.outputs());

        // Record trace
        state.addTraceNode(new TraceNode.ActionExec(
                "ACCUMULATE_SCORE",
                "_score",
                state.getAccumulatedScore() - node.score(),
                state.getAccumulatedScore(),
                Duration.ZERO
        ));

        PriorityVector priority = new PriorityVector(0, depth, 1.0, Instant.now(), 0);
        state.addMatchedRule(new MatchedRule(
                ruleId,
                ruleId,
                0,
                priority,
                Map.of("nodeId", node.nodeId(), "score", node.score()),
                node.outputs()
        ));

        if (earlyTermination && !scoringMode) {
            state.terminate();
        }
    }

    // ------------------------------------------------------------------
    // Gate node
    // ------------------------------------------------------------------

    private void traverseGate(CompiledGateNode node, WorkingMemory wm,
                              TraversalState state, int depth) {
        long start = System.nanoTime();
        boolean result = expressionEvaluator.evaluateAsBoolean(node.compiledExpression(), wm);
        Duration evalTime = Duration.ofNanos(System.nanoTime() - start);

        state.addTraceNode(new TraceNode.ConditionEval(
                "GATE: " + node.compiledExpression(),
                null,
                "GATE",
                result,
                result,
                evalTime
        ));

        if (result) {
            state.addMatchedCondition("GATE(" + node.nodeId() + ")=true");
            traverseNode(node.trueChild(), wm, state, depth + 1);
        } else {
            state.addMatchedCondition("GATE(" + node.nodeId() + ")=false");
            traverseNode(node.falseChild(), wm, state, depth + 1);
        }
    }

    // ------------------------------------------------------------------
    // Sub-rule node (deferred in Phase 1)
    // ------------------------------------------------------------------

    private void traverseSubRule(CompiledSubRuleNode node, TraversalState state, int depth) {
        // Phase 1: record sub-rule reference for later resolution; do not evaluate inline.
        state.addTraceNode(new TraceNode.RuleInvocation(
                node.subRuleId(),
                "sub-rule:" + node.subRuleId() + "v" + node.subRuleVersion(),
                RuleResult.empty(),
                Duration.ZERO,
                java.util.List.of()
        ));

        state.addMatchedCondition("SUB_RULE(" + node.subRuleId() + "v" + node.subRuleVersion() + ")");
    }
}
