package io.ruleforge.flow.graph;

/**
 * Sealed interface representing a node in a flow graph.
 */
public sealed interface FlowNode permits
        ConditionFlowNode,
        ActionFlowNode,
        SubRuleFlowNode,
        GateFlowNode,
        ForkFlowNode,
        JoinFlowNode {

    String id();
    String type();
}
