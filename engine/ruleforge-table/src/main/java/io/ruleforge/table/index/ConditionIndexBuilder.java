package io.ruleforge.table.index;

/**
 * Factory that selects the appropriate {@link ConditionIndex} implementation
 * based on the operator string.
 */
public final class ConditionIndexBuilder {

    private ConditionIndexBuilder() {
        // utility class
    }

    /**
     * Creates a new, empty {@link ConditionIndex} appropriate for the given operator.
     *
     * @param operator the comparison operator (EQ, GT, GTE, LT, LTE, BETWEEN, IN, CONTAINS,
     *                 STARTS_WITH, REGEX, IS_NULL, IS_NOT_NULL)
     * @return a new index instance (not yet built)
     */
    public static ConditionIndex create(String operator) {
        return switch (operator.toUpperCase()) {
            case "EQ", "IN", "NEQ", "NOT_IN" -> new HashMapIndex();
            case "GT", "GTE", "LT", "LTE" -> new SortedArrayIndex(operator.toUpperCase());
            case "BETWEEN" -> new IntervalTreeIndex();
            case "CONTAINS", "STARTS_WITH" -> new TrieIndex(operator.toUpperCase());
            case "REGEX" -> new RegexIndex();
            case "IS_NULL", "IS_NOT_NULL" -> new BitmapIndex(operator.toUpperCase());
            default -> new HashMapIndex(); // fallback to hash equality
        };
    }
}
