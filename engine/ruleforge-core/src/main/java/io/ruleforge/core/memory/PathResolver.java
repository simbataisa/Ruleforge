package io.ruleforge.core.memory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for resolving dot-separated paths against the fact map, with support
 * for nested {@link Map} traversal and array/list index access.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code "customer"} — returns the top-level fact value</li>
 *   <li>{@code "customer.address.city"} — navigates into nested Maps</li>
 *   <li>{@code "order.items[0].name"} — accesses a list element by index</li>
 * </ul>
 */
public final class PathResolver {

    /** Pattern that matches a segment optionally followed by one or more [index] accessors. */
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)]");

    private PathResolver() {
        // utility class
    }

    /**
     * Resolves a dot-path against the working memory fact map.
     *
     * @param facts   the current fact map (path to Fact)
     * @param dotPath a dot-separated path, e.g. {@code "customer.address.city"}
     * @return the resolved value, or {@code null} if any segment is missing or null
     */
    @SuppressWarnings("unchecked")
    public static Object resolve(Map<String, Fact<?>> facts, String dotPath) {
        if (facts == null || dotPath == null || dotPath.isEmpty()) {
            return null;
        }

        String[] segments = dotPath.split("\\.");

        // The first segment is always the top-level fact key.
        String firstSegment = stripIndex(segments[0]);
        Fact<?> fact = facts.get(firstSegment);
        if (fact == null) {
            return null;
        }

        Object current = fact.value();

        // Apply any index accessors on the first segment itself, e.g. "items[0]"
        current = applyIndices(current, segments[0]);
        if (current == null) {
            return null;
        }

        // Traverse remaining segments
        for (int i = 1; i < segments.length; i++) {
            if (current == null) {
                return null;
            }

            String segment = segments[i];
            String key = stripIndex(segment);

            // Navigate into a Map
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                // Cannot navigate further into a non-Map, non-List value
                return null;
            }

            // Apply any [index] accessors on this segment
            current = applyIndices(current, segment);
        }

        return current;
    }

    /**
     * Strips all {@code [index]} suffixes from a segment, returning only the key name.
     */
    private static String stripIndex(String segment) {
        int bracketPos = segment.indexOf('[');
        return bracketPos < 0 ? segment : segment.substring(0, bracketPos);
    }

    /**
     * Applies all {@code [index]} accessors found in the segment to the given value.
     * For example, if segment is {@code "matrix[0][1]"} and value is a nested list,
     * this will drill into {@code value.get(0).get(1)}.
     *
     * @return the value after all index accesses, or {@code null} if any access fails
     */
    @SuppressWarnings("unchecked")
    private static Object applyIndices(Object value, String segment) {
        Matcher matcher = INDEX_PATTERN.matcher(segment);
        Object current = value;

        while (matcher.find()) {
            if (current == null) {
                return null;
            }

            int index = Integer.parseInt(matcher.group(1));

            if (current instanceof List<?> list) {
                if (index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
            } else if (current instanceof Object[] array) {
                if (index < 0 || index >= array.length) {
                    return null;
                }
                current = array[index];
            } else {
                // Not an indexable type
                return null;
            }
        }

        return current;
    }
}
