package io.ruleforge.flow.graph;

/**
 * A fork node that splits execution into parallel branches.
 */
public record ForkFlowNode(String id) implements FlowNode {

    @Override
    public String type() {
        return "FORK";
    }
}
