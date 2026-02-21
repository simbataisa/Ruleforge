package io.ruleforge.ensemble;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.ensemble.compiler.CompiledEnsemble;
import io.ruleforge.ensemble.compiler.EnsembleCompiler;
import io.ruleforge.ensemble.model.AggregationStrategy;
import io.ruleforge.ensemble.model.EnsembleDefinition;
import io.ruleforge.ensemble.model.MemberRule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule engine for {@link RuleType#ENSEMBLE} rules.
 *
 * <p>Implements the {@link RuleTypeEngine} SPI. Compilation delegates to
 * {@link EnsembleCompiler}. Evaluation returns a Phase 3 stub result because
 * full sub-rule execution requires a rule registry accessible via the
 * {@link EvaluationContext} — a Phase 4 enhancement.
 *
 * <p>The stub result communicates ensemble readiness metadata to callers:
 * {@code ensembleReady=true}, {@code memberCount}, and {@code strategy}.
 */
public final class EnsembleEngine implements RuleTypeEngine {

    private final EnsembleCompiler compiler = new EnsembleCompiler();

    @Override
    public RuleType supportedType() {
        return RuleType.ENSEMBLE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledEnsemble ensemble)) {
            throw new RuleEvaluationException(
                    rule.ruleId(),
                    "Expected CompiledEnsemble but got " + rule.getClass().getSimpleName());
        }

        long startNanos = System.nanoTime();

        EnsembleDefinition definition = ensemble.ensembleDefinition();
        List<MemberRule> members = definition.members();
        AggregationStrategy strategy = definition.strategy();

        // Phase 3 stub: sub-rule execution requires a registry (Phase 4).
        // Return ensemble-readiness metadata so callers can detect ensemble type.
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("ensembleReady", true);
        output.put("memberCount", members.size());
        output.put("strategy", strategy.name());
        output.put("outputField", definition.outputField());

        List<String> memberAliases = new ArrayList<>();
        for (MemberRule member : members) {
            memberAliases.add(member.alias());
        }
        output.put("memberAliases", memberAliases);

        Duration executionTime = Duration.ofNanos(System.nanoTime() - startNanos);
        RuleResultMetadata metadata = new RuleResultMetadata(
                executionTime,
                members.size(),
                0,
                0,
                "ENSEMBLE"
        );

        return new RuleResult(output, metadata, List.of(), null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();

        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            errors.add(new ValidationResult.ValidationError(
                    "definition",
                    "MISSING_PAYLOAD",
                    "Ensemble definition payload must not be null"));
            return ValidationResult.failure(errors);
        }

        // Validate strategy
        Object strategyObj = payload.get("strategy");
        if (strategyObj == null) {
            errors.add(new ValidationResult.ValidationError(
                    "definition.strategy",
                    "MISSING_STRATEGY",
                    "Ensemble definition must specify an aggregation strategy"));
        } else {
            try {
                AggregationStrategy.valueOf(strategyObj.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationResult.ValidationError(
                        "definition.strategy",
                        "INVALID_STRATEGY",
                        "Unknown aggregation strategy: " + strategyObj));
            }
        }

        // Validate members
        Object membersObj = payload.get("members");
        if (membersObj == null) {
            errors.add(new ValidationResult.ValidationError(
                    "definition.members",
                    "MISSING_MEMBERS",
                    "Ensemble definition must specify at least one member rule"));
        } else if (membersObj instanceof List<?> memberList) {
            if (memberList.isEmpty()) {
                errors.add(new ValidationResult.ValidationError(
                        "definition.members",
                        "EMPTY_MEMBERS",
                        "Ensemble definition must contain at least one member rule"));
            } else {
                for (int i = 0; i < memberList.size(); i++) {
                    Object memberObj = memberList.get(i);
                    if (memberObj instanceof Map<?, ?> memberMap) {
                        Object ruleId = memberMap.get("ruleId");
                        if (ruleId == null || ruleId.toString().isBlank()) {
                            errors.add(new ValidationResult.ValidationError(
                                    "definition.members[" + i + "].ruleId",
                                    "MISSING_MEMBER_RULE_ID",
                                    "Member rule at index " + i + " must have a non-blank ruleId"));
                        }
                    } else {
                        errors.add(new ValidationResult.ValidationError(
                                "definition.members[" + i + "]",
                                "INVALID_MEMBER",
                                "Member at index " + i + " must be a map object"));
                    }
                }
            }
        } else {
            errors.add(new ValidationResult.ValidationError(
                    "definition.members",
                    "INVALID_MEMBERS_TYPE",
                    "Ensemble 'members' must be a list"));
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
