package io.ruleforge.scorecard.model;

import java.util.List;

/**
 * The fully-parsed, immutable definition of a scorecard rule.
 *
 * <p>Produced by {@link io.ruleforge.scorecard.compiler.ScorecardCompiler} and stored
 * inside a {@link io.ruleforge.scorecard.compiler.CompiledScorecard} for repeated
 * evaluation without re-parsing.
 *
 * @param ruleId       the unique rule identifier (forwarded from {@link io.ruleforge.core.model.RuleDefinition})
 * @param ruleName     human-readable rule name
 * @param baseScore    starting score before any category contributions are applied
 * @param categories   ordered list of scoring categories
 * @param strategy     the aggregation strategy ({@link ScoringStrategy})
 * @param minimumScore floor applied after aggregation (score is clamped to this value)
 * @param maximumScore ceiling applied after aggregation (score is clamped to this value)
 */
public record ScorecardDefinition(
        String ruleId,
        String ruleName,
        double baseScore,
        List<ScorecardCategory> categories,
        ScoringStrategy strategy,
        double minimumScore,
        double maximumScore
) {}
