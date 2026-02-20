package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves conflicts by probabilistic weighted random selection.
 * Useful for A/B testing scenarios where rules have different weights.
 */
public class WeightedRandomResolver implements ConflictResolutionStrategy {

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        if (conflictSet.size() <= 1) {
            return conflictSet;
        }
        double totalWeight = conflictSet.stream()
            .mapToDouble(r -> r.metadata().weight())
            .sum();
        if (totalWeight <= 0) {
            return List.of(conflictSet.getFirst());
        }

        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;
        for (ActivatedRule rule : conflictSet) {
            cumulative += rule.metadata().weight();
            if (random <= cumulative) {
                return List.of(rule);
            }
        }
        return List.of(conflictSet.getLast());
    }

    @Override
    public String name() {
        return "WEIGHTED_RANDOM";
    }
}
