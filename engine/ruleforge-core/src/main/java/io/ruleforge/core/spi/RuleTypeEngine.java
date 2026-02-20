package io.ruleforge.core.spi;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.result.RuleResult;

/**
 * SPI for type-specific rule engines (decision table, decision tree, scorecard, etc.).
 * Each rule type has its own engine implementation that handles compilation and evaluation.
 */
public interface RuleTypeEngine {

    /**
     * The rule type this engine handles.
     */
    RuleType supportedType();

    /**
     * Compile a rule definition into an optimized compiled form.
     */
    CompiledRule compile(RuleDefinition definition);

    /**
     * Evaluate a compiled rule against the current working memory.
     */
    RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx);

    /**
     * Validate a rule definition without compiling.
     */
    ValidationResult validate(RuleDefinition definition);
}
