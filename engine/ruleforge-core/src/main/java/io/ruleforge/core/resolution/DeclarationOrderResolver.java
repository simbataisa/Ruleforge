package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.Comparator;
import java.util.List;

/**
 * Resolves conflicts by declaration order (first declared wins).
 * This is the ultimate tiebreaker.
 */
public class DeclarationOrderResolver implements ConflictResolutionStrategy {

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        if (conflictSet.size() <= 1) {
            return conflictSet;
        }
        return conflictSet.stream()
            .sorted(Comparator.comparingInt(r -> r.priority().declarationOrder()))
            .limit(1)
            .toList();
    }

    @Override
    public String name() {
        return "DECLARATION_ORDER";
    }
}
