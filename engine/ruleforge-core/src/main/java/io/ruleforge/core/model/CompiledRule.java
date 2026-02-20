package io.ruleforge.core.model;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.RuleResult;

/**
 * Abstract base for compiled (optimised) rule representations.
 * <p>
 * A {@link CompiledRule} is produced by {@link RuleUnit#compile()} and is the
 * unit of execution inside the engine runtime.
 */
public abstract class CompiledRule {

    private final String ruleId;
    private final RuleType type;
    private final RuleMetadata metadata;
    private final long compiledAt;

    protected CompiledRule(String ruleId,
                           RuleType type,
                           RuleMetadata metadata,
                           long compiledAt) {
        this.ruleId = ruleId;
        this.type = type;
        this.metadata = metadata;
        this.compiledAt = compiledAt;
    }

    public String ruleId() {
        return ruleId;
    }

    public RuleType type() {
        return type;
    }

    public RuleMetadata metadata() {
        return metadata;
    }

    /**
     * Epoch millis when this rule was compiled.
     */
    public long compiledAt() {
        return compiledAt;
    }

    /**
     * Evaluate this compiled rule against a working memory.
     *
     * @param workingMemory the current working memory
     * @return the rule result
     */
    public abstract RuleResult evaluate(WorkingMemory workingMemory);
}
