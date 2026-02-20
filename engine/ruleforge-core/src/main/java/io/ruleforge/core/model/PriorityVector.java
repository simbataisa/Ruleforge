package io.ruleforge.core.model;

import java.time.Instant;

/**
 * Multi-dimensional priority vector used for conflict resolution.
 * <p>
 * Comparison order: salience desc, specificity desc, weight desc, recency desc, declarationOrder asc.
 *
 * @param salience         Priority hint, range -10000 to 10000. Higher fires first.
 * @param specificity      Auto-computed condition count.
 * @param weight           Relative weight, 0.0 to 1.0.
 * @param recency          Fact modification time.
 * @param declarationOrder Tiebreaker — lower value wins.
 */
public record PriorityVector(
        int salience,
        int specificity,
        double weight,
        Instant recency,
        int declarationOrder
) implements Comparable<PriorityVector> {

    @Override
    public int compareTo(PriorityVector other) {
        // salience descending
        int cmp = Integer.compare(other.salience, this.salience);
        if (cmp != 0) return cmp;

        // specificity descending
        cmp = Integer.compare(other.specificity, this.specificity);
        if (cmp != 0) return cmp;

        // weight descending
        cmp = Double.compare(other.weight, this.weight);
        if (cmp != 0) return cmp;

        // recency descending (more recent first)
        cmp = other.recency.compareTo(this.recency);
        if (cmp != 0) return cmp;

        // declarationOrder ascending (lower fires first)
        return Integer.compare(this.declarationOrder, other.declarationOrder);
    }
}
