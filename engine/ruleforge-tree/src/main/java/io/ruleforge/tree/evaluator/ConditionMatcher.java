package io.ruleforge.tree.evaluator;

import io.ruleforge.core.memory.WorkingMemory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility that evaluates a condition (field value, operator, comparison value) against working memory.
 * <p>
 * Supports operators: EQ, NEQ, GT, GTE, LT, LTE, BETWEEN, IN, NOT_IN, CONTAINS,
 * STARTS_WITH, ENDS_WITH, REGEX, IS_NULL, IS_NOT_NULL.
 * <p>
 * Performs automatic numeric coercion when both operands can be interpreted as numbers.
 */
public final class ConditionMatcher {

    private ConditionMatcher() {
        // utility class
    }

    /**
     * Evaluates whether the field value from working memory matches the given operator and comparison value.
     *
     * @param workingMemory the current working memory
     * @param field         the dot-path of the field to evaluate
     * @param operator      the comparison operator
     * @param value         the primary comparison value
     * @param secondValue   the secondary comparison value (used by BETWEEN)
     * @return true if the condition matches
     */
    public static boolean matches(WorkingMemory workingMemory, String field, String operator,
                                  Object value, Object secondValue) {
        Object fieldValue = workingMemory.get(field);
        return evaluate(fieldValue, operator, value, secondValue);
    }

    /**
     * Evaluates a condition directly against a field value.
     *
     * @param fieldValue  the actual field value from working memory
     * @param operator    the comparison operator
     * @param value       the primary comparison value
     * @param secondValue the secondary comparison value (used by BETWEEN)
     * @return true if the condition is satisfied
     */
    @SuppressWarnings("unchecked")
    public static boolean evaluate(Object fieldValue, String operator, Object value, Object secondValue) {
        return switch (operator.toUpperCase()) {
            case "EQ", "==" -> nullSafeEquals(fieldValue, value);
            case "NEQ", "!=" -> !nullSafeEquals(fieldValue, value);
            case "GT", ">" -> fieldValue != null && value != null && compare(fieldValue, value) > 0;
            case "GTE", ">=" -> fieldValue != null && value != null && compare(fieldValue, value) >= 0;
            case "LT", "<" -> fieldValue != null && value != null && compare(fieldValue, value) < 0;
            case "LTE", "<=" -> fieldValue != null && value != null && compare(fieldValue, value) <= 0;
            case "BETWEEN" -> evaluateBetween(fieldValue, value, secondValue);
            case "IN" -> evaluateIn(fieldValue, value, false);
            case "NOT_IN" -> evaluateIn(fieldValue, value, true);
            case "CONTAINS" -> evaluateContains(fieldValue, value);
            case "STARTS_WITH" -> evaluateStartsWith(fieldValue, value);
            case "ENDS_WITH" -> evaluateEndsWith(fieldValue, value);
            case "REGEX", "MATCHES" -> evaluateRegex(fieldValue, value);
            case "IS_NULL" -> fieldValue == null;
            case "IS_NOT_NULL" -> fieldValue != null;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    // ------------------------------------------------------------------
    // Operator implementations
    // ------------------------------------------------------------------

    private static boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Numeric equality with coercion
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        // Cross-type numeric comparison (string "42" == int 42)
        Double numA = toDoubleOrNull(a);
        Double numB = toDoubleOrNull(b);
        if (numA != null && numB != null) {
            return Double.compare(numA, numB) == 0;
        }
        return Objects.equals(a.toString(), b.toString());
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object a, Object b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Cannot compare null values with relational operator");
        }
        // Numeric comparison
        Double numA = toDoubleOrNull(a);
        Double numB = toDoubleOrNull(b);
        if (numA != null && numB != null) {
            return Double.compare(numA, numB);
        }
        // String comparison
        if (a instanceof String sa && b instanceof String sb) {
            return sa.compareTo(sb);
        }
        // Comparable fallback
        if (a instanceof Comparable ca && b.getClass().isAssignableFrom(a.getClass())) {
            return ca.compareTo(b);
        }
        // Last resort: compare string representations
        return a.toString().compareTo(b.toString());
    }

    private static boolean evaluateBetween(Object fieldValue, Object low, Object high) {
        if (fieldValue == null || low == null || high == null) return false;
        return compare(fieldValue, low) >= 0 && compare(fieldValue, high) <= 0;
    }

    @SuppressWarnings("unchecked")
    private static boolean evaluateIn(Object fieldValue, Object collection, boolean negated) {
        if (fieldValue == null) return negated;
        Collection<?> items;
        if (collection instanceof Collection<?> c) {
            items = c;
        } else if (collection instanceof Object[] arr) {
            items = List.of(arr);
        } else {
            // Single value: treat as a one-element set
            boolean match = nullSafeEquals(fieldValue, collection);
            return negated != match;
        }

        boolean found = false;
        for (Object item : items) {
            if (nullSafeEquals(fieldValue, item)) {
                found = true;
                break;
            }
        }
        return negated != found;
    }

    private static boolean evaluateContains(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) return false;
        // Collection contains
        if (fieldValue instanceof Collection<?> c) {
            for (Object item : c) {
                if (nullSafeEquals(item, value)) return true;
            }
            return false;
        }
        // String contains
        return fieldValue.toString().contains(value.toString());
    }

    private static boolean evaluateStartsWith(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) return false;
        return fieldValue.toString().startsWith(value.toString());
    }

    private static boolean evaluateEndsWith(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) return false;
        return fieldValue.toString().endsWith(value.toString());
    }

    private static boolean evaluateRegex(Object fieldValue, Object pattern) {
        if (fieldValue == null || pattern == null) return false;
        return Pattern.matches(pattern.toString(), fieldValue.toString());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Attempts to convert a value to a Double. Returns null if not convertible.
     */
    private static Double toDoubleOrNull(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
