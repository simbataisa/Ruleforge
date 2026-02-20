package io.ruleforge.core;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.EvaluationOptions;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.result.RuleResult;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for the RuleForge rule execution engine.
 * Provides methods for evaluating rules, compiling rule definitions,
 * and validating rule structures.
 */
public interface RuleForgeEngine {

    /** Evaluate a single rule with input data */
    RuleResult evaluate(String ruleId, int version, Map<String, Object> input);

    /** Evaluate a single rule with input data and options */
    RuleResult evaluate(String ruleId, int version, Map<String, Object> input, EvaluationOptions options);

    /** Evaluate with full Working Memory control */
    RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory);

    /** Bulk evaluate */
    List<RuleResult> evaluateBulk(String ruleId, int version, List<Map<String, Object>> inputs);

    /** Compile a rule definition */
    CompiledRule compile(RuleDefinition definition);

    /** Validate without compiling */
    ValidationResult validate(RuleDefinition definition);

    /** Builder */
    static RuleForgeEngineBuilder builder() {
        return new RuleForgeEngineBuilder();
    }
}
