package io.ruleforge.dmn.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.dmn.evaluator.DmnEvaluator;
import io.ruleforge.dmn.model.DmnDefinition;

/**
 * Compiled form of a DMN definition. Extends {@link CompiledRule} for integration
 * with the RuleForge engine.
 */
public final class CompiledDmn extends CompiledRule {

    private final DmnDefinition definition;
    private final String ruleName;
    private final int ruleVersion;

    public CompiledDmn(String ruleId,
                       String ruleName,
                       int ruleVersion,
                       RuleMetadata metadata,
                       long compiledAt,
                       DmnDefinition definition) {
        super(ruleId, RuleType.DMN_RULE, metadata, compiledAt);
        this.definition = definition;
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
    }

    public DmnDefinition definition() {
        return definition;
    }

    public String ruleName() {
        return ruleName;
    }

    public int ruleVersion() {
        return ruleVersion;
    }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        DmnEvaluator evaluator = new DmnEvaluator();
        return evaluator.evaluate(this, workingMemory);
    }
}
