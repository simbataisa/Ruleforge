package io.ruleforge.ensemble.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.ensemble.model.EnsembleDefinition;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compiled representation of an ensemble rule. Holds the parsed
 * {@link EnsembleDefinition} and delegates evaluation to
 * {@link io.ruleforge.ensemble.EnsembleEngine}, which has access to the
 * sub-rule registry via the {@link io.ruleforge.core.spi.EvaluationContext}.
 *
 * <p>The {@link #evaluate(WorkingMemory)} method returns a stub result because
 * full sub-rule execution requires a registry that is only available via
 * {@code EvaluationContext} at engine-dispatch time.
 */
public final class CompiledEnsemble extends CompiledRule {

    private final EnsembleDefinition ensembleDefinition;

    public CompiledEnsemble(String ruleId,
                             RuleMetadata metadata,
                             long compiledAt,
                             EnsembleDefinition ensembleDefinition) {
        super(ruleId, RuleType.ENSEMBLE, metadata, compiledAt);
        this.ensembleDefinition = ensembleDefinition;
    }

    public EnsembleDefinition ensembleDefinition() {
        return ensembleDefinition;
    }

    /**
     * Returns a stub result. Full ensemble evaluation requires sub-rule registry
     * access available only through {@link io.ruleforge.core.spi.EvaluationContext},
     * so the real execution is performed in
     * {@link io.ruleforge.ensemble.EnsembleEngine#evaluate}.
     */
    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("ensembleReady", true);
        output.put("memberCount", ensembleDefinition.members().size());
        output.put("strategy", ensembleDefinition.strategy().name());

        RuleResultMetadata meta = new RuleResultMetadata(
                Duration.ZERO,
                ensembleDefinition.members().size(),
                0,
                0,
                "ENSEMBLE"
        );

        return new RuleResult(output, meta, null, null);
    }
}
