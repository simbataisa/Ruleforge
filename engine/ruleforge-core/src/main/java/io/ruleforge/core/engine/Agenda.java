package io.ruleforge.core.engine;

import io.ruleforge.core.resolution.ActivatedRule;
import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Priority queue of activated rules. Manages the conflict set and
 * delegates to a {@link ConflictResolutionStrategy} to determine firing order.
 *
 * <p>Rules are added to the agenda when they match the current working memory state.
 * Before execution, the conflict resolution strategy is applied to determine which
 * rules should fire and in what order.
 */
public class Agenda {

    private final List<ActivatedRule> activations = new ArrayList<>();
    private final ConflictResolutionStrategy strategy;

    /**
     * Creates an agenda with the given conflict resolution strategy.
     *
     * @param strategy the strategy used to resolve conflicts among activated rules
     */
    public Agenda(ConflictResolutionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }

    /**
     * Adds a single activated rule to the agenda.
     *
     * @param rule the activated rule to add
     */
    public void addActivation(ActivatedRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        activations.add(rule);
    }

    /**
     * Adds multiple activated rules to the agenda.
     *
     * @param rules the collection of activated rules to add
     */
    public void addActivations(Collection<ActivatedRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        activations.addAll(rules);
    }

    /**
     * Returns {@code true} if there are activated rules waiting to fire.
     */
    public boolean hasActivations() {
        return !activations.isEmpty();
    }

    /**
     * Resolves conflicts among the current activations using the configured strategy.
     * The activations are first sorted by their natural priority ordering, then
     * the conflict resolution strategy is applied to narrow and reorder the set.
     *
     * @param ctx the execution context providing runtime information
     * @return the resolved list of rules to fire, in firing order
     */
    public List<ActivatedRule> resolveConflicts(ExecutionContext ctx) {
        if (activations.isEmpty()) {
            return List.of();
        }
        // Sort by natural priority order (PriorityVector.compareTo) before strategy
        List<ActivatedRule> sorted = new ArrayList<>(activations);
        Collections.sort(sorted);
        return strategy.resolve(sorted, ctx);
    }

    /**
     * Clears all activations from the agenda.
     */
    public void clear() {
        activations.clear();
    }

    /**
     * Returns the number of activated rules currently on the agenda.
     */
    public int size() {
        return activations.size();
    }

    /**
     * Returns an unmodifiable view of the current activations (unsorted).
     */
    public List<ActivatedRule> getActivations() {
        return Collections.unmodifiableList(activations);
    }
}
