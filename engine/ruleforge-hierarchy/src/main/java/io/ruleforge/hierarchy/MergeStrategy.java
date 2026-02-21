package io.ruleforge.hierarchy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the merge logic for combining parent and child rule definitions
 * according to the specified {@link OverrideMode}.
 */
public final class MergeStrategy {

    private MergeStrategy() {}

    /**
     * Merges a child definition into a parent definition according to the given override mode.
     *
     * @param parent       the parent (base) definition map
     * @param child        the child (override) definition map
     * @param overrideMode how the child overrides the parent
     * @return the merged definition
     */
    public static Map<String, Object> merge(Map<String, Object> parent,
                                             Map<String, Object> child,
                                             OverrideMode overrideMode) {
        if (parent == null || parent.isEmpty()) {
            return child != null ? new LinkedHashMap<>(child) : new LinkedHashMap<>();
        }
        if (child == null || child.isEmpty()) {
            return new LinkedHashMap<>(parent);
        }

        return switch (overrideMode) {
            case REPLACE -> new LinkedHashMap<>(child);
            case MERGE -> mergeKeys(parent, child);
            case PREPEND -> prependRows(parent, child);
            case APPEND -> appendRows(parent, child);
            case OVERRIDE_ROWS -> overrideRows(parent, child);
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergeKeys(Map<String, Object> parent,
                                                   Map<String, Object> child) {
        Map<String, Object> result = new LinkedHashMap<>(parent);
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            Object parentVal = result.get(entry.getKey());
            Object childVal = entry.getValue();

            if (parentVal instanceof Map && childVal instanceof Map) {
                result.put(entry.getKey(),
                        mergeKeys((Map<String, Object>) parentVal, (Map<String, Object>) childVal));
            } else {
                result.put(entry.getKey(), childVal);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> prependRows(Map<String, Object> parent,
                                                     Map<String, Object> child) {
        Map<String, Object> result = new LinkedHashMap<>(parent);
        Object parentRows = parent.get("rows");
        Object childRows = child.get("rows");
        if (parentRows instanceof List<?> pList && childRows instanceof List<?> cList) {
            List<Object> merged = new ArrayList<>(cList);
            merged.addAll(pList);
            result.put("rows", merged);
        }
        // Merge non-row keys from child
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            if (!"rows".equals(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> appendRows(Map<String, Object> parent,
                                                    Map<String, Object> child) {
        Map<String, Object> result = new LinkedHashMap<>(parent);
        Object parentRows = parent.get("rows");
        Object childRows = child.get("rows");
        if (parentRows instanceof List<?> pList && childRows instanceof List<?> cList) {
            List<Object> merged = new ArrayList<>(pList);
            merged.addAll(cList);
            result.put("rows", merged);
        }
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            if (!"rows".equals(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> overrideRows(Map<String, Object> parent,
                                                      Map<String, Object> child) {
        Map<String, Object> result = new LinkedHashMap<>(parent);
        Object parentRows = parent.get("rows");
        Object childRows = child.get("rows");

        if (parentRows instanceof List<?> pList && childRows instanceof List<?> cList) {
            // Index child rows by rowNumber
            Map<Integer, Object> childRowIndex = new LinkedHashMap<>();
            for (Object row : cList) {
                if (row instanceof Map<?, ?> rowMap) {
                    Object rowNum = ((Map<String, Object>) rowMap).get("rowNumber");
                    if (rowNum instanceof Number n) {
                        childRowIndex.put(n.intValue(), row);
                    }
                }
            }

            // Replace matching rows
            List<Object> merged = new ArrayList<>();
            for (Object row : pList) {
                if (row instanceof Map<?, ?> rowMap) {
                    Object rowNum = ((Map<String, Object>) rowMap).get("rowNumber");
                    if (rowNum instanceof Number n && childRowIndex.containsKey(n.intValue())) {
                        merged.add(childRowIndex.remove(n.intValue()));
                    } else {
                        merged.add(row);
                    }
                } else {
                    merged.add(row);
                }
            }
            // Add any remaining child rows that didn't match
            merged.addAll(childRowIndex.values());
            result.put("rows", merged);
        }

        for (Map.Entry<String, Object> entry : child.entrySet()) {
            if (!"rows".equals(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
