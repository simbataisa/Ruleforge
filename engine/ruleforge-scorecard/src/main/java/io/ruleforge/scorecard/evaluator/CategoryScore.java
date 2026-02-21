package io.ruleforge.scorecard.evaluator;

import java.util.Map;

/**
 * Snapshot of scoring results for a single {@link io.ruleforge.scorecard.model.ScorecardCategory}.
 *
 * <p>Captured during evaluation and included in the {@link ScorecardEvaluator} output
 * so that callers can inspect per-category and per-attribute breakdowns for audit
 * or explainability purposes.
 *
 * @param categoryName     the name of the evaluated category
 * @param rawScore         the sum of all attribute points within this category
 * @param weightedScore    the raw score multiplied by the category's weight
 *                         (relevant for {@link io.ruleforge.scorecard.model.ScoringStrategy#WEIGHTED_AVERAGE};
 *                         equals {@code rawScore} for other strategies)
 * @param attributeScores  map of attribute name to the points awarded for that attribute
 */
public record CategoryScore(
        String categoryName,
        double rawScore,
        double weightedScore,
        Map<String, Double> attributeScores
) {
    public CategoryScore {
        attributeScores = attributeScores == null ? Map.of() : Map.copyOf(attributeScores);
    }
}
