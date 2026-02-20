package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.List;

/**
 * Resolves conflicts by selecting rules with the most conditions (most specific).
 */
public class SpecificityResolver implements ConflictResolutionStrategy {

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        if (conflictSet.size() <= 1) {
            return conflictSet;
        }
        int maxConditions = conflictSet.stream()
            .mapToInt(ActivatedRule::conditionCount)
            .max()
            .orElse(0);
        return conflictSet.stream()
            .filter(r -> r.conditionCount() == maxConditions)
            .toList();
    }

    @Override
    public String name() {
        return "SPECIFICITY";
    }
}
