package io.ruleforge.table.index;

import java.util.List;
import java.util.Set;

/**
 * Index over a single condition column for fast row lookup.
 * <p>
 * Implementations are built at compile time and queried at evaluation time.
 */
public interface ConditionIndex {

    /**
     * Returns the set of row numbers that match the given input value.
     *
     * @param value the fact value to look up
     * @return set of matching row numbers (never null; empty if none match)
     */
    Set<Integer> lookup(Object value);

    /**
     * Builds (or rebuilds) this index from the given entries.
     *
     * @param entries the index entries extracted from table rows
     */
    void build(List<IndexEntry> entries);

    /**
     * A single entry to be indexed.
     *
     * @param rowNumber   the row this entry belongs to
     * @param value       the primary cell value
     * @param secondValue the secondary cell value (for BETWEEN ranges)
     */
    record IndexEntry(int rowNumber, Object value, Object secondValue) {}
}
