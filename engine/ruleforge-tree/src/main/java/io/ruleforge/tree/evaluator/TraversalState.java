package io.ruleforge.tree.evaluator;

import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.TraceNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable state accumulated during decision tree traversal.
 * <p>
 * Tracks accumulated score, collected output values, matched rules, trace nodes for
 * explainability, and the path of matched conditions. This state is built up as the
 * {@link TreeTraverser} walks through the compiled tree and is consumed when building
 * the final {@link io.ruleforge.core.result.RuleResult}.
 */
public final class TraversalState {

    private double accumulatedScore;
    private final Map<String, Object> collectedOutputs;
    private final List<MatchedRule> matchedRules;
    private final List<TraceNode> traceNodes;
    private final List<String> matchedConditions;
    private boolean terminated;

    /**
     * Creates a new empty traversal state.
     */
    public TraversalState() {
        this.accumulatedScore = 0.0;
        this.collectedOutputs = new LinkedHashMap<>();
        this.matchedRules = new ArrayList<>();
        this.traceNodes = new ArrayList<>();
        this.matchedConditions = new ArrayList<>();
        this.terminated = false;
    }

    /**
     * Adds a score to the accumulated total.
     */
    public void addScore(double score) {
        this.accumulatedScore += score;
    }

    /**
     * Returns the current accumulated score.
     */
    public double getAccumulatedScore() {
        return accumulatedScore;
    }

    /**
     * Merges the given outputs into the collected outputs map. Later values overwrite earlier ones.
     */
    public void addOutputs(Map<String, Object> outputs) {
        if (outputs != null) {
            this.collectedOutputs.putAll(outputs);
        }
    }

    /**
     * Returns the collected outputs as an unmodifiable view.
     */
    public Map<String, Object> getCollectedOutputs() {
        return Map.copyOf(collectedOutputs);
    }

    /**
     * Returns the mutable internal collected outputs map.
     */
    public Map<String, Object> getMutableCollectedOutputs() {
        return collectedOutputs;
    }

    /**
     * Adds a matched rule record.
     */
    public void addMatchedRule(MatchedRule matchedRule) {
        this.matchedRules.add(matchedRule);
    }

    /**
     * Returns a copy of the matched rules list.
     */
    public List<MatchedRule> getMatchedRules() {
        return List.copyOf(matchedRules);
    }

    /**
     * Adds a trace node for explainability.
     */
    public void addTraceNode(TraceNode node) {
        this.traceNodes.add(node);
    }

    /**
     * Returns a copy of the trace nodes list.
     */
    public List<TraceNode> getTraceNodes() {
        return List.copyOf(traceNodes);
    }

    /**
     * Records a matched condition description.
     */
    public void addMatchedCondition(String condition) {
        this.matchedConditions.add(condition);
    }

    /**
     * Returns a copy of the matched conditions list.
     */
    public List<String> getMatchedConditions() {
        return List.copyOf(matchedConditions);
    }

    /**
     * Marks the traversal as terminated (early termination triggered).
     */
    public void terminate() {
        this.terminated = true;
    }

    /**
     * Returns whether early termination has been triggered.
     */
    public boolean isTerminated() {
        return terminated;
    }
}
