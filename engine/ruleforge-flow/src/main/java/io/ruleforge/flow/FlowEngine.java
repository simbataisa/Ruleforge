package io.ruleforge.flow;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.flow.compiler.CompiledFlowRule;
import io.ruleforge.flow.compiler.FlowCompiler;
import io.ruleforge.flow.executor.FlowExecutor;
import io.ruleforge.flow.validator.FlowValidator;

/**
 * Engine for evaluating rule flow rules.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for rule type {@link RuleType#RULE_FLOW}.
 * Supports DAG-based flow orchestration with conditional branching, parallel
 * fork/join, sub-rule delegation, and action nodes.
 * <p>
 * Compilation delegates to {@link FlowCompiler} which parses the flow definition
 * into a {@link io.ruleforge.flow.graph.FlowGraph}. Evaluation delegates to
 * {@link FlowExecutor} which traverses the graph from the start node.
 */
public final class FlowEngine implements RuleTypeEngine {

    private final FlowCompiler compiler = new FlowCompiler();
    private final FlowValidator validator = new FlowValidator();

    @Override
    public RuleType supportedType() {
        return RuleType.RULE_FLOW;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledFlowRule flowRule)) {
            throw new RuleEvaluationException(
                    rule.ruleId(),
                    "Expected CompiledFlowRule but got " + rule.getClass().getSimpleName());
        }
        return new FlowExecutor(flowRule.ruleId(), ctx.getSubRuleInvoker(), ctx)
                .execute(flowRule.graph(), workingMemory);
    }

    @Override
    public ValidationResult validate(RuleDefinition definition) {
        return validator.validate(definition);
    }
}
