package io.ruleforge.table.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O(1) hash-based index for EQ and IN operators.
 * <p>
 * For EQ, each distinct value maps to the set of row numbers that contain it.
 * For IN, each element in the cell's collection is indexed individually.
 */
public final class HashMapIndex implements ConditionIndex {

    private final Map<Object, Set<Integer>> map = new HashMap<>();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    @Override
    public Set<Integer> lookup(Object value) {
        if (value == null) {
            return Set.copyOf(wildcardRows);
        }
        Set<Integer> result = new HashSet<>(wildcardRows);
        Set<Integer> exact = map.get(normalizeKey(value));
        if (exact != null) {
            result.addAll(exact);
        }
        return result;
    }

    @Override
    public void build(List<IndexEntry> entries) {
        map.clear();
        wildcardRows.clear();
        for (IndexEntry entry : entries) {
            if (entry.value() == null) {
                wildcardRows.add(entry.rowNumber());
                continue;
            }
            Object val = entry.value();
            if (val instanceof Collection<?> coll) {
                for (Object element : coll) {
                    addToMap(normalizeKey(element), entry.rowNumber());
                }
            } else {
                addToMap(normalizeKey(val), entry.rowNumber());
            }
        }
    }

    private void addToMap(Object key, int rowNumber) {
        map.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(rowNumber);
    }

    private Object normalizeKey(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return s.toLowerCase();
        }
        return value;
    }
}
