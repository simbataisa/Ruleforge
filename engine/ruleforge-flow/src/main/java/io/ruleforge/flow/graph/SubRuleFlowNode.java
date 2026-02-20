package io.ruleforge.flow.graph;

/**
 * A sub-rule node that delegates evaluation to another rule.
 */
public record SubRuleFlowNode(String id, String ruleId) implements FlowNode {

    @Override
    public String type() {
        return "SUB_RULE";
    }
}
