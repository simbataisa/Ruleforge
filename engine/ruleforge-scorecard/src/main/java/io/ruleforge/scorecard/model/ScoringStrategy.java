package io.ruleforge.scorecard.model;

/**
 * Strategy for combining category scores into a final scorecard total.
 *
 * <ul>
 *   <li>{@link #ADDITIVE} - sum all category raw scores plus the base score.</li>
 *   <li>{@link #WEIGHTED_AVERAGE} - compute weighted average of category raw scores
 *       using each category's weight, then add the base score.</li>
 *   <li>{@link #MULTIPLICATIVE} - multiply running total by {@code (1 + categoryRawScore)}
 *       for each category, starting from the base score.</li>
 * </ul>
 */
public enum ScoringStrategy {
    ADDITIVE,
    WEIGHTED_AVERAGE,
    MULTIPLICATIVE
}
