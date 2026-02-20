package io.ruleforge.flow.graph;

/**
 * A condition node that evaluates an expression and routes to outgoing edges.
 */
public record ConditionFlowNode(String id, String expression) implements FlowNode {

    @Override
    public String type() {
        return "CONDITION";
    }
}
