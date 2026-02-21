package io.ruleforge.scorecard.evaluator;

import io.ruleforge.core.memory.Fact;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.scorecard.model.ScorecardAttribute;
import io.ruleforge.scorecard.model.ScorecardCategory;
import io.ruleforge.scorecard.model.ScorecardDefinition;
import io.ruleforge.scorecard.model.ScoringStrategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless evaluator that computes a final scorecard score from a
 * {@link ScorecardDefinition} and a populated {@link WorkingMemory}.
 *
 * <h2>Evaluation algorithm</h2>
 * <ol>
 *   <li>Start with {@code baseScore}.</li>
 *   <li>For each category, resolve each attribute's fact value from working memory
 *       and delegate to {@link ScorecardAttribute#evaluate(double)} to obtain
 *       attribute points.  Sum them to get the {@code categoryRawScore}.</li>
 *   <li>Aggregate category scores according to the chosen {@link ScoringStrategy}:
 *     <ul>
 *       <li><b>ADDITIVE</b>: {@code totalScore += categoryRawScore}</li>
 *       <li><b>WEIGHTED_AVERAGE</b>: accumulate {@code categoryRawScore * weight}
 *           and total weight; divide at the end, then add {@code baseScore}.</li>
 *       <li><b>MULTIPLICATIVE</b>: {@code totalScore *= (1 + categoryRawScore)}</li>
 *     </ul>
 *   </li>
 *   <li>Clamp the result to {@code [minimumScore, maximumScore]}.</li>
 *   <li>Build and return a {@link RuleResult} containing the score, per-category
 *       breakdown, and execution metadata.</li>
 * </ol>
 */
public final class ScorecardEvaluator {

    private ScorecardEvaluator() {
        // utility class — not instantiable
    }

    /**
     * Evaluates the supplied scorecard definition against the current working memory.
     *
     * @param def           the parsed scorecard definition
     * @param workingMemory the fact store for this execution
     * @return the computed {@link RuleResult}
     */
    public static RuleResult evaluate(ScorecardDefinition def, WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();

        List<CategoryScore> categoryScores = new ArrayList<>(def.categories().size());

        double totalScore   = def.baseScore();
        double weightedSum  = 0.0;
        double totalWeight  = 0.0;

        // Initialise MULTIPLICATIVE from base score
        if (def.strategy() == ScoringStrategy.MULTIPLICATIVE) {
            totalScore = def.baseScore();
        }

        for (ScorecardCategory category : def.categories()) {
            Map<String, Double> attributeScores = new LinkedHashMap<>();
            double categoryRawScore = 0.0;

            for (ScorecardAttribute attribute : category.attributes()) {
                double points = resolveAttributePoints(attribute, workingMemory);
                attributeScores.put(attribute.name(), points);
                categoryRawScore += points;
            }

            double weightedScore = categoryRawScore * category.weight();

            categoryScores.add(new CategoryScore(
                    category.name(),
                    categoryRawScore,
                    weightedScore,
                    attributeScores
            ));

            switch (def.strategy()) {
                case ADDITIVE -> totalScore += categoryRawScore;
                case WEIGHTED_AVERAGE -> {
                    weightedSum += weightedScore;
                    totalWeight += category.weight();
                }
                case MULTIPLICATIVE -> totalScore *= (1.0 + categoryRawScore);
            }
        }

        // Finalise WEIGHTED_AVERAGE
        if (def.strategy() == ScoringStrategy.WEIGHTED_AVERAGE) {
            double average = (totalWeight > 0.0) ? (weightedSum / totalWeight) : 0.0;
            totalScore = def.baseScore() + average;
        }

        // Clamp to configured bounds
        totalScore = Math.max(def.minimumScore(), Math.min(def.maximumScore(), totalScore));

        // Build output map
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("score",      totalScore);
        output.put("categories", serializeCategoryScores(categoryScores));
        output.put("strategy",   def.strategy().name());

        Duration executionTime = Duration.ofNanos(System.nanoTime() - startNanos);
        RuleResultMetadata metadata = new RuleResultMetadata(executionTime, 1, 1, 0, "SCORECARD");

        return new RuleResult(output, metadata, List.of(), null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the numeric value for an attribute from working memory and delegates
     * to {@link ScorecardAttribute#evaluate(double)}.
     *
     * <p>The {@link WorkingMemory#getFact(String)} method performs an exact top-level
     * key lookup.  For nested paths (e.g. {@code "input.age"}), the working memory
     * {@link WorkingMemory#get(String)} resolver is used as a fallback so that both
     * flat and nested fact layouts are supported.
     *
     * @param attribute     the attribute to evaluate
     * @param workingMemory the current fact store
     * @return points awarded (or {@code defaultPoints} if fact is absent or non-numeric)
     */
    @SuppressWarnings("unchecked")
    private static double resolveAttributePoints(ScorecardAttribute attribute,
                                                  WorkingMemory workingMemory) {
        String path = attribute.factPath();

        // Attempt direct top-level fact lookup first
        Fact<?> fact = workingMemory.getFact(path);
        if (fact != null && fact.value() instanceof Number n) {
            return attribute.evaluate(n.doubleValue());
        }

        // Fallback: use the PathResolver-backed get() for nested paths
        Object resolved = workingMemory.get(path);
        if (resolved instanceof Number n) {
            return attribute.evaluate(n.doubleValue());
        }

        // Fact absent or non-numeric — use default points
        return attribute.defaultPoints();
    }

    /**
     * Converts the list of {@link CategoryScore}s into a plain
     * {@code List<Map<String,Object>>} suitable for inclusion in the output map.
     */
    private static List<Map<String, Object>> serializeCategoryScores(List<CategoryScore> scores) {
        List<Map<String, Object>> result = new ArrayList<>(scores.size());
        for (CategoryScore cs : scores) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("categoryName",    cs.categoryName());
            entry.put("rawScore",        cs.rawScore());
            entry.put("weightedScore",   cs.weightedScore());
            entry.put("attributeScores", cs.attributeScores());
            result.add(entry);
        }
        return result;
    }
}
