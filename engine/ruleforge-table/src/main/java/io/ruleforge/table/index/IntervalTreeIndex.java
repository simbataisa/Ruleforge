package io.ruleforge.table.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Interval-based index for BETWEEN operator: O(log n + k) lookup where k is
 * the number of matching intervals.
 * <p>
 * Uses a sorted list of intervals with binary-search to find all intervals that
 * contain the queried value, which is efficient for typical decision table sizes.
 */
public final class IntervalTreeIndex implements ConditionIndex {

    private final List<Interval> intervals = new ArrayList<>();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    @Override
    public Set<Integer> lookup(Object value) {
        if (value == null) {
            return Set.copyOf(wildcardRows);
        }
        double query = toDouble(value);
        Set<Integer> result = new HashSet<>(wildcardRows);

        // Binary search for the first interval whose low <= query
        // Then scan forward to find all overlapping intervals
        int lo = 0;
        int hi = intervals.size() - 1;
        // Find leftmost interval with low <= query
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (intervals.get(mid).low <= query) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        // All intervals [0..hi] have low <= query; check which also have high >= query
        for (int i = 0; i <= hi && i < intervals.size(); i++) {
            Interval iv = intervals.get(i);
            if (iv.low <= query && iv.high >= query) {
                result.add(iv.rowNumber);
            }
        }

        return result;
    }

    @Override
    public void build(List<IndexEntry> entries) {
        intervals.clear();
        wildcardRows.clear();
        for (IndexEntry entry : entries) {
            if (entry.value() == null && entry.secondValue() == null) {
                wildcardRows.add(entry.rowNumber());
                continue;
            }
            double low = toDouble(entry.value());
            double high = toDouble(entry.secondValue());
            if (low > high) {
                double tmp = low;
                low = high;
                high = tmp;
            }
            intervals.add(new Interval(entry.rowNumber(), low, high));
        }
        intervals.sort(Comparator.comparingDouble(iv -> iv.low));
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return Double.parseDouble(s.trim());
        }
        throw new IllegalArgumentException("Cannot convert to double for BETWEEN index: " + value);
    }

    private record Interval(int rowNumber, double low, double high) {}
}
