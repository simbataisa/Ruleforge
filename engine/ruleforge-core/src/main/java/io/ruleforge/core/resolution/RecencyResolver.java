package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.time.Instant;
import java.util.List;

/**
 * Resolves conflicts by selecting rules activated by the most recently modified facts.
 */
public class RecencyResolver implements ConflictResolutionStrategy {

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        if (conflictSet.size() <= 1) {
            return conflictSet;
        }
        Instant mostRecent = conflictSet.stream()
            .map(r -> r.priority().recency())
            .filter(r -> r != null)
            .max(Instant::compareTo)
            .orElse(Instant.MIN);
        return conflictSet.stream()
            .filter(r -> r.priority().recency() != null && r.priority().recency().equals(mostRecent))
            .toList();
    }

    @Override
    public String name() {
        return "RECENCY";
    }
}
