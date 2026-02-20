package io.ruleforge.flow.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.flow.executor.FlowExecutor;
import io.ruleforge.flow.graph.FlowGraph;

/**
 * Compiled representation of a flow rule. Holds the compiled FlowGraph
 * and delegates evaluation to {@link FlowExecutor}.
 */
public final class CompiledFlowRule extends CompiledRule {

    private final FlowGraph graph;

    public CompiledFlowRule(String ruleId,
                             RuleMetadata metadata,
                             long compiledAt,
                             FlowGraph graph) {
        super(ruleId, RuleType.RULE_FLOW, metadata, compiledAt);
        this.graph = graph;
    }

    public FlowGraph graph() {
        return graph;
    }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        return new FlowExecutor(ruleId()).execute(graph, workingMemory);
    }
}
