package io.ruleforge.table.evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Intersects result sets from multiple column indexes.
 * <p>
 * Uses smallest-set-first optimization: sorts candidate sets by size ascending,
 * then iterates through elements of the smallest set, retaining only those
 * present in all other sets. This minimizes the number of set membership checks.
 */
public final class IndexIntersector {

    /**
     * Computes the intersection of all candidate sets.
     *
     * @param candidateSets list of sets to intersect (each from a different column index)
     * @return the intersection, or empty set if any input set is empty
     */
    public Set<Integer> intersect(List<Set<Integer>> candidateSets) {
        if (candidateSets == null || candidateSets.isEmpty()) {
            return Set.of();
        }
        if (candidateSets.size() == 1) {
            return new HashSet<>(candidateSets.getFirst());
        }

        // Sort by size ascending for smallest-set-first intersection
        List<Set<Integer>> sorted = new ArrayList<>(candidateSets);
        sorted.sort(Comparator.comparingInt(Set::size));

        Set<Integer> result = new HashSet<>(sorted.getFirst());
        for (int i = 1; i < sorted.size(); i++) {
            if (result.isEmpty()) {
                return result;
            }
            result.retainAll(sorted.get(i));
        }
        return result;
    }
}
