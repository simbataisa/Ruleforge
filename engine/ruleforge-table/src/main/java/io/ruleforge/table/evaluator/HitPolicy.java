package io.ruleforge.table.evaluator;

/**
 * Hit policy controlling how matched rows are selected and combined.
 */
public enum HitPolicy {

    /** Return the first matching row (by priority, then row order). */
    FIRST,

    /** Return all matching rows with merged outputs. */
    ALL,

    /** Return the highest-priority matching row. */
    PRIORITY,

    /** Aggregate numeric outputs across all matching rows (SUM, MIN, MAX, AVG, COUNT). */
    AGGREGATE,

    /** Return top-N matches ranked by score (priority * weight). */
    RANKED
}
