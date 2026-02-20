package io.ruleforge.table.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pre-compiled regex index for REGEX operator.
 * <p>
 * Each cell value is compiled into a {@link Pattern} at build time. Lookup
 * tests the query string against all patterns, collecting matching row numbers.
 */
public final class RegexIndex implements ConditionIndex {

    private final List<RegexEntry> patterns = new ArrayList<>();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    @Override
    public Set<Integer> lookup(Object value) {
        if (value == null) {
            return Set.copyOf(wildcardRows);
        }
        String query = value.toString();
        Set<Integer> result = new HashSet<>(wildcardRows);
        for (RegexEntry entry : patterns) {
            if (entry.pattern.matcher(query).matches()) {
                result.add(entry.rowNumber);
            }
        }
        return result;
    }

    @Override
    public void build(List<IndexEntry> entries) {
        patterns.clear();
        wildcardRows.clear();
        for (IndexEntry entry : entries) {
            if (entry.value() == null) {
                wildcardRows.add(entry.rowNumber());
                continue;
            }
            String regex = entry.value().toString();
            Pattern compiled = Pattern.compile(regex);
            patterns.add(new RegexEntry(entry.rowNumber(), compiled));
        }
    }

    private record RegexEntry(int rowNumber, Pattern pattern) {}
}
