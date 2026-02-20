package io.ruleforge.core.expression;

import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.OperatorExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registry of comparison/logical operators used by the expression evaluator.
 *
 * <p>Ships with built-in implementations for all standard RFL operators.
 * Custom operators can be added at runtime via
 * {@link #register(OperatorExtension)}.
 */
public class OperatorRegistry {

    @FunctionalInterface
    private interface BuiltinOperator {
        boolean evaluate(Object left, Object right, EvaluationContext ctx);
    }

    private final Map<String, BuiltinOperator> builtins = new HashMap<>();
    private final Map<String, OperatorExtension> extensions = new HashMap<>();

    public OperatorRegistry() {
        registerBuiltins();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Evaluates an operator against two operands.
     *
     * @param operator the operator name (e.g. "==", "MATCHES")
     * @param left     left operand
     * @param right    right operand
     * @param ctx      evaluation context
     * @return the boolean result
     * @throws RflParseException if the operator is unknown
     */
    public boolean evaluate(String operator, Object left, Object right, EvaluationContext ctx) {
        // Check extensions first so custom operators can override builtins
        OperatorExtension ext = extensions.get(operator);
        if (ext != null) {
            return ext.evaluate(left, right, ctx);
        }
        BuiltinOperator builtin = builtins.get(operator);
        if (builtin != null) {
            return builtin.evaluate(left, right, ctx);
        }
        throw new RflParseException("Unknown operator: " + operator, 0);
    }

    /**
     * Registers a custom operator extension.
     */
    public void register(OperatorExtension extension) {
        extensions.put(extension.name(), extension);
    }

    // ------------------------------------------------------------------
    // Built-in operator registration
    // ------------------------------------------------------------------

    private void registerBuiltins() {
        // Equality
        builtins.put("==", (l, r, ctx) -> nullSafeEquals(l, r));
        builtins.put("EQ", (l, r, ctx) -> nullSafeEquals(l, r));

        builtins.put("!=", (l, r, ctx) -> !nullSafeEquals(l, r));
        builtins.put("NEQ", (l, r, ctx) -> !nullSafeEquals(l, r));

        // Ordering
        builtins.put(">", (l, r, ctx) -> compare(l, r) > 0);
        builtins.put("GT", (l, r, ctx) -> compare(l, r) > 0);

        builtins.put(">=", (l, r, ctx) -> compare(l, r) >= 0);
        builtins.put("GTE", (l, r, ctx) -> compare(l, r) >= 0);

        builtins.put("<", (l, r, ctx) -> compare(l, r) < 0);
        builtins.put("LT", (l, r, ctx) -> compare(l, r) < 0);

        builtins.put("<=", (l, r, ctx) -> compare(l, r) <= 0);
        builtins.put("LTE", (l, r, ctx) -> compare(l, r) <= 0);

        // String pattern operators
        builtins.put("MATCHES", (l, r, ctx) -> {
            if (l == null || r == null) return false;
            return Pattern.matches(r.toString(), l.toString());
        });

        builtins.put("STARTS_WITH", (l, r, ctx) -> {
            if (l == null || r == null) return false;
            return l.toString().startsWith(r.toString());
        });

        builtins.put("ENDS_WITH", (l, r, ctx) -> {
            if (l == null || r == null) return false;
            return l.toString().endsWith(r.toString());
        });

        builtins.put("CONTAINS", (l, r, ctx) -> {
            if (l == null || r == null) return false;
            if (l instanceof Collection<?> col) {
                return col.contains(r);
            }
            return l.toString().contains(r.toString());
        });

        // Collection operators
        builtins.put("IN", (l, r, ctx) -> {
            if (r instanceof Collection<?> col) return col.contains(l);
            return false;
        });

        builtins.put("NOT_IN", (l, r, ctx) -> {
            if (r instanceof Collection<?> col) return !col.contains(l);
            return true;
        });

        // Null checks
        builtins.put("IS_NULL", (l, r, ctx) -> l == null);
        builtins.put("IS_NOT_NULL", (l, r, ctx) -> l != null);
    }

    // ------------------------------------------------------------------
    // Comparison helpers
    // ------------------------------------------------------------------

    private static boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Handle numeric comparison across types (e.g. Integer == Double)
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        return a.equals(b);
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object a, Object b) {
        if (a == null || b == null) {
            throw new RflParseException("Cannot compare null values with ordering operators", 0);
        }
        // Numeric comparison (cross-type safe)
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable ca && b.getClass().isAssignableFrom(a.getClass())) {
            return ca.compareTo(b);
        }
        // Fallback: compare string representations
        return a.toString().compareTo(b.toString());
    }
}
