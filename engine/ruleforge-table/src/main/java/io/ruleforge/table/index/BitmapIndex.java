package io.ruleforge.table.index;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bitmap-style index for IS_NULL and IS_NOT_NULL operators: O(1) lookup.
 * <p>
 * Maintains two pre-computed sets: one for rows that expect null, one for non-null.
 */
public final class BitmapIndex implements ConditionIndex {

    private final String operator;
    private final Set<Integer> matchingRows = new LinkedHashSet<>();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    public BitmapIndex(String operator) {
        this.operator = operator;
    }

    @Override
    public Set<Integer> lookup(Object value) {
        Set<Integer> result = new HashSet<>(wildcardRows);
        boolean isNull = (value == null);
        boolean matches = switch (operator) {
            case "IS_NULL" -> isNull;
            case "IS_NOT_NULL" -> !isNull;
            default -> false;
        };
        if (matches) {
            result.addAll(matchingRows);
        }
        return result;
    }

    @Override
    public void build(List<IndexEntry> entries) {
        matchingRows.clear();
        wildcardRows.clear();
        for (IndexEntry entry : entries) {
            if (entry.value() == null) {
                // A null cell value in IS_NULL/IS_NOT_NULL means wildcard (always matches)
                wildcardRows.add(entry.rowNumber());
            } else {
                matchingRows.add(entry.rowNumber());
            }
        }
    }
}
