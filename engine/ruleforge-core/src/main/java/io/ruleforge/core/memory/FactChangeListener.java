package io.ruleforge.core.memory;

/**
 * Callback interface for observing changes to facts in working memory.
 */
@FunctionalInterface
public interface FactChangeListener {

    /**
     * Invoked whenever a fact is asserted, modified, or retracted.
     *
     * @param fact       the fact that was changed
     * @param changeType the kind of change that occurred
     */
    void onFactChanged(Fact<?> fact, ChangeType changeType);
}
