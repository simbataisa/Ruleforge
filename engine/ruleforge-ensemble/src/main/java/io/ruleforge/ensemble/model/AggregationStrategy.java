package io.ruleforge.ensemble.model;

/**
 * Strategies for aggregating results from multiple member rules in an ensemble.
 */
public enum AggregationStrategy {

    /**
     * Select the output value that appears most frequently across all member results.
     * Each member has equal voting power.
     */
    MAJORITY_VOTE,

    /**
     * Select the output value with the highest total weight across all member results.
     * Each member's vote is multiplied by its assigned weight.
     */
    WEIGHTED_VOTE,

    /**
     * Compute the arithmetic mean of all member numeric output values.
     */
    AVERAGE,

    /**
     * Compute the weighted average of all member numeric output values,
     * using each member's assigned weight.
     */
    WEIGHTED_AVERAGE,

    /**
     * Return the output from the first member rule that produced a result.
     */
    FIRST_MATCH,

    /**
     * Return the maximum numeric value across all member outputs.
     */
    MAX,

    /**
     * Return the minimum numeric value across all member outputs.
     */
    MIN
}
