package io.ruleforge.core.spi;

import io.ruleforge.core.resolution.ActivatedRule;

import java.util.List;

/**
 * Strategy for resolving conflicts when multiple rules match simultaneously.
 * Implementations narrow the conflict set to determine which rule(s) should fire.
 */
public interface ConflictResolutionStrategy {

    /**
     * Resolve the conflict set, returning the rules that should fire.
     *
     * @param conflictSet all rules that matched the current working memory state
     * @param ctx execution context with runtime information
     * @return narrowed list of rules to fire (may be a subset or reordered)
     */
    List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx);

    /**
     * Human-readable name for this strategy.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
