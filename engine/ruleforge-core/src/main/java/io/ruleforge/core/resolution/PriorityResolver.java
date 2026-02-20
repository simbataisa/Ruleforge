package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.List;

/**
 * Resolves conflicts by selecting rules with the highest salience.
 */
public class PriorityResolver implements ConflictResolutionStrategy {

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        if (conflictSet.size() <= 1) {
            return conflictSet;
        }
        int maxSalience = conflictSet.stream()
            .mapToInt(r -> r.metadata().salience())
            .max()
            .orElse(0);
        return conflictSet.stream()
            .filter(r -> r.metadata().salience() == maxSalience)
            .toList();
    }

    @Override
    public String name() {
        return "PRIORITY";
    }
}
