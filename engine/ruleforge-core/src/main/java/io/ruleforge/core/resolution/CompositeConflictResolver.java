package io.ruleforge.core.resolution;

import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.List;

/**
 * Chains multiple conflict resolution strategies in order.
 * Each resolver narrows the conflict set; the chain stops when
 * only one rule remains or all resolvers have been applied.
 *
 * Default chain: Priority → Specificity → Recency → Declaration Order
 */
public class CompositeConflictResolver implements ConflictResolutionStrategy {

    private final List<ConflictResolutionStrategy> chain;

    public CompositeConflictResolver(List<ConflictResolutionStrategy> chain) {
        this.chain = List.copyOf(chain);
    }

    /**
     * Creates the default composite resolver with the standard chain:
     * Priority → Specificity → Recency → Declaration Order
     */
    public static CompositeConflictResolver defaultChain() {
        return new CompositeConflictResolver(List.of(
            new PriorityResolver(),
            new SpecificityResolver(),
            new RecencyResolver(),
            new DeclarationOrderResolver()
        ));
    }

    @Override
    public List<ActivatedRule> resolve(List<ActivatedRule> conflictSet, ExecutionContext ctx) {
        List<ActivatedRule> remaining = conflictSet;
        for (ConflictResolutionStrategy resolver : chain) {
            remaining = resolver.resolve(remaining, ctx);
            if (remaining.size() <= 1) {
                break;
            }
        }
        return remaining;
    }

    @Override
    public String name() {
        return "COMPOSITE[" + String.join(" → ",
            chain.stream().map(ConflictResolutionStrategy::name).toList()) + "]";
    }
}
