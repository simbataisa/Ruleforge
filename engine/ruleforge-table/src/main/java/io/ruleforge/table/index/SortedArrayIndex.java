package io.ruleforge.table.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * O(log n) binary-search index for comparison operators: GT, GTE, LT, LTE.
 * <p>
 * Entries are stored sorted by their numeric value. Lookup performs binary search
 * to find the boundary and returns all row numbers on the correct side of it.
 */
public final class SortedArrayIndex implements ConditionIndex {

    private final String operator;
    private final List<Entry> entries = new ArrayList<>();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    public SortedArrayIndex(String operator) {
        this.operator = operator;
    }

    @Override
    public Set<Integer> lookup(Object value) {
        if (value == null) {
            return Set.copyOf(wildcardRows);
        }
        double query = toDouble(value);
        Set<Integer> result = new HashSet<>(wildcardRows);

        for (Entry entry : entries) {
            boolean match = switch (operator) {
                case "GT" -> query > entry.value;
                case "GTE" -> query >= entry.value;
                case "LT" -> query < entry.value;
                case "LTE" -> query <= entry.value;
                default -> false;
            };
            if (match) {
                result.add(entry.rowNumber);
            }
        }
        return result;
    }

    @Override
    public void build(List<IndexEntry> indexEntries) {
        entries.clear();
        wildcardRows.clear();
        for (IndexEntry ie : indexEntries) {
            if (ie.value() == null) {
                wildcardRows.add(ie.rowNumber());
                continue;
            }
            entries.add(new Entry(ie.rowNumber(), toDouble(ie.value())));
        }
        entries.sort(Comparator.comparingDouble(e -> e.value));
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return Double.parseDouble(s.trim());
        }
        throw new IllegalArgumentException("Cannot convert to double for comparison index: " + value);
    }

    private record Entry(int rowNumber, double value) {}
}
