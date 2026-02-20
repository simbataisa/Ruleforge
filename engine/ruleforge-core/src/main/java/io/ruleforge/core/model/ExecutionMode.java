package io.ruleforge.core.model;

/**
 * Defines how the engine processes a set of matching rules.
 */
public enum ExecutionMode {

    /** Stop at first matching rule. */
    FIRST_MATCH,

    /** Fire all matching rules. */
    ALL_MATCH,

    /** Score all, return highest. */
    BEST_MATCH,

    /** Fire in priority order, each sees previous results. */
    SEQUENTIAL,

    /** Fire concurrently, aggregate. */
    PARALLEL,

    /** Rete-style fire/modify/re-evaluate cycle. */
    FORWARD_CHAIN,

    /** Fire through hierarchy until result produced. */
    CASCADING
}
