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
import io.ruleforge.core.spi.SubRuleInvoker;
import io.ruleforge.ensemble.compiler.CompiledEnsemble;
import io.ruleforge.ensemble.compiler.EnsembleCompiler;
import io.ruleforge.ensemble.model.AggregationStrategy;
import io.ruleforge.ensemble.model.EnsembleDefinition;
import io.ruleforge.ensemble.model.MemberRule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
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
        String outputField = definition.outputField();

        SubRuleInvoker invoker = ctx.getSubRuleInvoker();
        if (invoker == null) {
            // Fallback: return readiness metadata when no invoker is available
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("ensembleReady", true);
            output.put("memberCount", members.size());
            output.put("strategy", strategy.name());
            output.put("outputField", outputField);
            Duration executionTime = Duration.ofNanos(System.nanoTime() - startNanos);
            RuleResultMetadata metadata = new RuleResultMetadata(
                    executionTime, members.size(), 0, 0, "ENSEMBLE");
            return new RuleResult(output, metadata, List.of(), null);
        }

        // Invoke each member rule and collect results
        List<MemberOutput> memberOutputs = new ArrayList<>();
        int rulesFired = 0;
        for (MemberRule member : members) {
            try {
                RuleResult memberResult = invoker.invoke(member.ruleId(), workingMemory);
                Object value = memberResult.output().get(outputField);
                memberOutputs.add(new MemberOutput(member, value, memberResult));
                if (memberResult.hasOutput()) {
                    rulesFired++;
                }
            } catch (Exception e) {
                // Member failure: skip this member but continue with others
                memberOutputs.add(new MemberOutput(member, null, null));
            }
        }

        // Aggregate results based on strategy
        Object aggregatedValue = aggregate(strategy, memberOutputs, rule.ruleId());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputField, aggregatedValue);
        output.put("strategy", strategy.name());
        output.put("memberCount", members.size());
        output.put("membersFired", rulesFired);

        Duration executionTime = Duration.ofNanos(System.nanoTime() - startNanos);
        RuleResultMetadata metadata = new RuleResultMetadata(
                executionTime,
                members.size(),
                rulesFired,
                0,
                "ENSEMBLE"
        );

        return new RuleResult(output, metadata, List.of(), null);
    }

    private Object aggregate(AggregationStrategy strategy, List<MemberOutput> outputs, String ruleId) {
        return switch (strategy) {
            case MAJORITY_VOTE -> aggregateMajorityVote(outputs);
            case WEIGHTED_VOTE -> aggregateWeightedVote(outputs);
            case AVERAGE -> aggregateAverage(outputs);
            case WEIGHTED_AVERAGE -> aggregateWeightedAverage(outputs);
            case FIRST_MATCH -> aggregateFirstMatch(outputs);
            case MAX -> aggregateMax(outputs, ruleId);
            case MIN -> aggregateMin(outputs, ruleId);
        };
    }

    private Object aggregateMajorityVote(List<MemberOutput> outputs) {
        Map<Object, Integer> votes = new HashMap<>();
        for (MemberOutput mo : outputs) {
            if (mo.value != null) {
                votes.merge(mo.value, 1, Integer::sum);
            }
        }
        return votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Object aggregateWeightedVote(List<MemberOutput> outputs) {
        Map<Object, Double> votes = new HashMap<>();
        for (MemberOutput mo : outputs) {
            if (mo.value != null) {
                votes.merge(mo.value, mo.member.weight(), Double::sum);
            }
        }
        return votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Object aggregateAverage(List<MemberOutput> outputs) {
        double sum = 0;
        int count = 0;
        for (MemberOutput mo : outputs) {
            if (mo.value instanceof Number n) {
                sum += n.doubleValue();
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    private Object aggregateWeightedAverage(List<MemberOutput> outputs) {
        double weightedSum = 0;
        double totalWeight = 0;
        for (MemberOutput mo : outputs) {
            if (mo.value instanceof Number n) {
                weightedSum += n.doubleValue() * mo.member.weight();
                totalWeight += mo.member.weight();
            }
        }
        return totalWeight > 0 ? weightedSum / totalWeight : null;
    }

    private Object aggregateFirstMatch(List<MemberOutput> outputs) {
        for (MemberOutput mo : outputs) {
            if (mo.value != null) {
                return mo.value;
            }
        }
        return null;
    }

    private Object aggregateMax(List<MemberOutput> outputs, String ruleId) {
        double max = Double.NEGATIVE_INFINITY;
        boolean found = false;
        for (MemberOutput mo : outputs) {
            if (mo.value instanceof Number n) {
                max = Math.max(max, n.doubleValue());
                found = true;
            }
        }
        return found ? max : null;
    }

    private Object aggregateMin(List<MemberOutput> outputs, String ruleId) {
        double min = Double.POSITIVE_INFINITY;
        boolean found = false;
        for (MemberOutput mo : outputs) {
            if (mo.value instanceof Number n) {
                min = Math.min(min, n.doubleValue());
                found = true;
            }
        }
        return found ? min : null;
    }

    private record MemberOutput(MemberRule member, Object value, RuleResult result) {}


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
