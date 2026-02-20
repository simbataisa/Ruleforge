package io.ruleforge.table.index;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trie-based index for CONTAINS and STARTS_WITH operators: O(m) lookup where m
 * is the length of the query string.
 * <p>
 * For STARTS_WITH, each indexed prefix is stored in the trie, and lookup walks
 * the trie character-by-character from the query string, collecting all row numbers
 * found along the path.
 * <p>
 * For CONTAINS, this falls back to a linear scan with {@link String#contains},
 * since a full substring trie (e.g., suffix tree) would be excessive for typical
 * decision table sizes.
 */
public final class TrieIndex implements ConditionIndex {

    private final String operator;
    private final TrieNode root = new TrieNode();
    private final Set<Integer> wildcardRows = new LinkedHashSet<>();

    // For CONTAINS, we keep a simple list since trie-based substring matching is overkill
    private final List<ContainsEntry> containsEntries = new java.util.ArrayList<>();

    public TrieIndex(String operator) {
        this.operator = operator;
    }

    @Override
    public Set<Integer> lookup(Object value) {
        if (value == null) {
            return Set.copyOf(wildcardRows);
        }
        String query = value.toString().toLowerCase();
        Set<Integer> result = new HashSet<>(wildcardRows);

        if ("STARTS_WITH".equals(operator)) {
            // Walk the trie along the query string; every node we pass through that
            // marks end-of-prefix is a match (the query starts with that prefix).
            TrieNode node = root;
            for (int i = 0; i < query.length() && node != null; i++) {
                char c = query.charAt(i);
                node = node.children.get(c);
                if (node != null && !node.rowNumbers.isEmpty()) {
                    result.addAll(node.rowNumbers);
                }
            }
        } else {
            // CONTAINS: linear scan
            for (ContainsEntry entry : containsEntries) {
                if (query.contains(entry.pattern)) {
                    result.add(entry.rowNumber);
                }
            }
        }

        return result;
    }

    @Override
    public void build(List<IndexEntry> entries) {
        root.children.clear();
        root.rowNumbers.clear();
        wildcardRows.clear();
        containsEntries.clear();

        for (IndexEntry entry : entries) {
            if (entry.value() == null) {
                wildcardRows.add(entry.rowNumber());
                continue;
            }
            String val = entry.value().toString().toLowerCase();
            if ("STARTS_WITH".equals(operator)) {
                insertTrie(val, entry.rowNumber());
            } else {
                containsEntries.add(new ContainsEntry(val, entry.rowNumber()));
            }
        }
    }

    private void insertTrie(String word, int rowNumber) {
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.rowNumbers.add(rowNumber);
    }

    private static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        final Set<Integer> rowNumbers = new LinkedHashSet<>();
    }

    private record ContainsEntry(String pattern, int rowNumber) {}
}
