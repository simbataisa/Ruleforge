package io.ruleforge.flow.graph;

/**
 * A directed edge between two nodes in a flow graph.
 */
public record FlowEdge(String fromId, String toId, String condition) {

    public FlowEdge(String fromId, String toId) {
        this(fromId, toId, "true");
    }

    /**
     * Returns true if this edge should be followed given a condition evaluation result.
     * Matches when condition is null, "true", or matches the provided boolean value.
     */
    public boolean matches(boolean conditionResult) {
        if (condition == null || condition.equals("true")) return true;
        if (condition.equals("false")) return false;
        return conditionResult;
    }
}
