package io.ruleforge.hierarchy;

/**
 * Defines how a child level's rule overrides its parent's rule
 * when resolving the effective rule at a given hierarchy position.
 */
public enum OverrideMode {

    /**
     * Child completely replaces the parent's definition.
     */
    REPLACE,

    /**
     * Child's actions are merged with the parent's. Child values take
     * precedence for keys that exist in both.
     */
    MERGE,

    /**
     * Child's rows/actions are prepended before the parent's.
     */
    PREPEND,

    /**
     * Child's rows/actions are appended after the parent's.
     */
    APPEND,

    /**
     * Specific rows in the parent are overridden by matching rows in the child.
     * Rows are matched by row number.
     */
    OVERRIDE_ROWS
}
