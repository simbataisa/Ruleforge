package io.ruleforge.core.memory;

/**
 * Enumerates the types of changes that can occur to facts in working memory.
 */
public enum ChangeType {

    /** A new fact was added to working memory. */
    ASSERT,

    /** An existing fact was updated in working memory. */
    MODIFY,

    /** A fact was removed from working memory. */
    RETRACT
}
