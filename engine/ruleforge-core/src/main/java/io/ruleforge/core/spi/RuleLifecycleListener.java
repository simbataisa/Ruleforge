package io.ruleforge.core.spi;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.RuleUnit;
import io.ruleforge.core.result.RuleResult;

import java.time.Duration;

/**
 * SPI for observing rule evaluation lifecycle events.
 * Useful for metrics, auditing, logging, and monitoring.
 */
public interface RuleLifecycleListener {

    /**
     * Called before a rule is evaluated.
     */
    default void beforeEvaluation(RuleUnit rule, WorkingMemory wm) {}

    /**
     * Called after a rule is successfully evaluated.
     */
    default void afterEvaluation(RuleUnit rule, RuleResult result, Duration elapsed) {}

    /**
     * Called when a rule evaluation fails with an error.
     */
    default void onError(RuleUnit rule, Throwable error) {}
}
