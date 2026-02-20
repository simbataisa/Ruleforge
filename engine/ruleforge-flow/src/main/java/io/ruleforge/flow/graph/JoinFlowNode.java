package io.ruleforge.flow.graph;

/**
 * A join node that merges parallel branches back into a single path.
 */
public record JoinFlowNode(String id) implements FlowNode {

    @Override
    public String type() {
        return "JOIN";
    }
}
