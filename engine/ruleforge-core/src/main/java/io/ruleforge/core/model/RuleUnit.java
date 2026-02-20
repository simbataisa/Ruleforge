package io.ruleforge.core.model;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.RuleResult;

import java.util.UUID;

/**
 * Abstract base class for all rule units in the engine.
 * <p>
 * Concrete subtypes include decision tables, decision trees, scorecards, etc.
 */
public abstract class RuleUnit {

    private final UUID id;
    private final String name;
    private final String alias;
    private final RuleType type;
    private final int version;
    private final RuleMetadata metadata;
    private final InputSchema inputSchema;
    private final OutputSchema outputSchema;

    protected RuleUnit(UUID id,
                       String name,
                       String alias,
                       RuleType type,
                       int version,
                       RuleMetadata metadata,
                       InputSchema inputSchema,
                       OutputSchema outputSchema) {
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.type = type;
        this.version = version;
        this.metadata = metadata;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    // ---- Getters ----

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }

    public RuleType type() {
        return type;
    }

    public int version() {
        return version;
    }

    public RuleMetadata metadata() {
        return metadata;
    }

    public InputSchema inputSchema() {
        return inputSchema;
    }

    public OutputSchema outputSchema() {
        return outputSchema;
    }

    // ---- Abstract operations ----

    /**
     * Evaluate this rule unit against the supplied working memory.
     *
     * @param workingMemory the working memory containing input facts
     * @return evaluation result
     */
    public abstract RuleResult evaluate(WorkingMemory workingMemory);

    /**
     * Compile this rule unit into an optimised runtime form.
     *
     * @return a compiled rule ready for execution
     */
    public abstract CompiledRule compile();

    /**
     * Validate the structural integrity of this rule unit.
     *
     * @return a {@link ValidationResult} describing any errors or warnings
     */
    public abstract ValidationResult validate();
}
