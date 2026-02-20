package io.ruleforge.flow.graph;

/**
 * A gate node combining multiple incoming edges with AND/OR logic.
 */
public record GateFlowNode(String id, String gateType) implements FlowNode {

    @Override
    public String type() {
        return "GATE";
    }
}
