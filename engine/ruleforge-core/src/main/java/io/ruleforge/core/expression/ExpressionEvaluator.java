package io.ruleforge.core.expression;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.EvaluationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tree-walking interpreter for RFL {@link Expression} ASTs.
 *
 * <p>Evaluates an expression against a {@link WorkingMemory} instance,
 * delegating function calls to {@link FunctionRegistry} and comparison
 * operators to {@link OperatorRegistry}.
 */
public class ExpressionEvaluator {

    private final FunctionRegistry functionRegistry;
    private final OperatorRegistry operatorRegistry;

    public ExpressionEvaluator() {
        this(new FunctionRegistry(), new OperatorRegistry());
    }

    public ExpressionEvaluator(FunctionRegistry functionRegistry, OperatorRegistry operatorRegistry) {
        this.functionRegistry = functionRegistry;
        this.operatorRegistry = operatorRegistry;
    }

    // ------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------

    /**
     * Evaluates the expression to an arbitrary value.
     *
     * @param expr the AST to evaluate
     * @param wm   the working memory providing fact bindings
     * @return the computed value (may be {@code null})
     */
    public Object evaluate(Expression expr, WorkingMemory wm) {
        return evaluate(expr, wm, null);
    }

    /**
     * Evaluates the expression to an arbitrary value with an evaluation context.
     */
    public Object evaluate(Expression expr, WorkingMemory wm, EvaluationContext ctx) {
        return doEvaluate(expr, wm, ctx);
    }

    /**
     * Evaluates the expression and coerces the result to a boolean.
     *
     * <p>Truthiness rules:
     * <ul>
     *   <li>{@code null} &rarr; false</li>
     *   <li>{@link Boolean} &rarr; direct value</li>
     *   <li>{@link Number} &rarr; false if 0</li>
     *   <li>{@link String} &rarr; false if empty</li>
     *   <li>anything else &rarr; true</li>
     * </ul>
     */
    public boolean evaluateAsBoolean(Expression expr, WorkingMemory wm) {
        return evaluateAsBoolean(expr, wm, null);
    }

    public boolean evaluateAsBoolean(Expression expr, WorkingMemory wm, EvaluationContext ctx) {
        return isTruthy(doEvaluate(expr, wm, ctx));
    }

    // ------------------------------------------------------------------
    // Core evaluation dispatch
    // ------------------------------------------------------------------

    private Object doEvaluate(Expression expr, WorkingMemory wm, EvaluationContext ctx) {
        return switch (expr) {
            case Expression.Literal lit -> lit.value();

            case Expression.Identifier id -> wm.get(id.name());

            case Expression.UnaryOp unary -> evalUnary(unary, wm, ctx);

            case Expression.BinaryOp bin -> evalBinaryOp(bin, wm, ctx);

            case Expression.Comparison cmp -> evalComparison(cmp, wm, ctx);

            case Expression.LogicalOp logic -> evalLogical(logic, wm, ctx);

            case Expression.PropertyAccess pa -> evalPropertyAccess(pa, wm, ctx);

            case Expression.NullSafeAccess nsa -> evalNullSafeAccess(nsa, wm, ctx);

            case Expression.IndexAccess ia -> evalIndexAccess(ia, wm, ctx);

            case Expression.FunctionCall fc -> evalFunctionCall(fc, wm, ctx);

            case Expression.BetweenExpr bet -> evalBetween(bet, wm, ctx);

            case Expression.InExpr in -> evalIn(in, wm, ctx);

            case Expression.NullCoalesce nc -> evalNullCoalesce(nc, wm, ctx);

            case Expression.Lambda lam -> lam; // return as-is; callers unwrap

            case Expression.ArrayLiteral arr -> evalArrayLiteral(arr, wm, ctx);
        };
    }

    // ------------------------------------------------------------------
    // Node-type evaluators
    // ------------------------------------------------------------------

    private Object evalUnary(Expression.UnaryOp unary, WorkingMemory wm, EvaluationContext ctx) {
        Object operand = doEvaluate(unary.operand(), wm, ctx);
        return switch (unary.operator()) {
            case "NOT" -> !isTruthy(operand);
            case "-" -> {
                if (operand instanceof Number n) {
                    yield negate(n);
                }
                throw new RflParseException("Cannot negate non-numeric value: " + operand, 0);
            }
            default -> throw new RflParseException("Unknown unary operator: " + unary.operator(), 0);
        };
    }

    private Object evalBinaryOp(Expression.BinaryOp bin, WorkingMemory wm, EvaluationContext ctx) {
        Object left = doEvaluate(bin.left(), wm, ctx);
        Object right = doEvaluate(bin.right(), wm, ctx);

        // String concatenation with +
        if ("+".equals(bin.operator()) && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }

        double l = toDouble(left);
        double r = toDouble(right);

        return switch (bin.operator()) {
            case "+" -> coerceResult(l + r, left, right);
            case "-" -> coerceResult(l - r, left, right);
            case "*" -> coerceResult(l * r, left, right);
            case "/" -> {
                if (r == 0) throw new RflParseException("Division by zero", 0);
                yield coerceResult(l / r, left, right);
            }
            case "%" -> {
                if (r == 0) throw new RflParseException("Modulo by zero", 0);
                yield coerceResult(l % r, left, right);
            }
            default -> throw new RflParseException("Unknown binary operator: " + bin.operator(), 0);
        };
    }

    private Object evalComparison(Expression.Comparison cmp, WorkingMemory wm, EvaluationContext ctx) {
        Object left = doEvaluate(cmp.left(), wm, ctx);
        Object right = doEvaluate(cmp.right(), wm, ctx);
        return operatorRegistry.evaluate(cmp.operator(), left, right, ctx);
    }

    private Object evalLogical(Expression.LogicalOp logic, WorkingMemory wm, EvaluationContext ctx) {
        // Short-circuit evaluation
        boolean leftVal = isTruthy(doEvaluate(logic.left(), wm, ctx));
        return switch (logic.operator()) {
            case "OR" -> leftVal || isTruthy(doEvaluate(logic.right(), wm, ctx));
            case "AND" -> leftVal && isTruthy(doEvaluate(logic.right(), wm, ctx));
            default -> throw new RflParseException("Unknown logical operator: " + logic.operator(), 0);
        };
    }

    @SuppressWarnings("unchecked")
    private Object evalPropertyAccess(Expression.PropertyAccess pa, WorkingMemory wm, EvaluationContext ctx) {
        Object obj = doEvaluate(pa.object(), wm, ctx);
        if (obj == null) {
            throw new RflParseException("Cannot access property '" + pa.property() + "' on null", 0);
        }
        return navigateProperty(obj, pa.property());
    }

    private Object evalNullSafeAccess(Expression.NullSafeAccess nsa, WorkingMemory wm, EvaluationContext ctx) {
        Object obj = doEvaluate(nsa.object(), wm, ctx);
        if (obj == null) {
            return null;
        }
        return navigateProperty(obj, nsa.property());
    }

    private Object evalIndexAccess(Expression.IndexAccess ia, WorkingMemory wm, EvaluationContext ctx) {
        Object obj = doEvaluate(ia.object(), wm, ctx);
        Object index = doEvaluate(ia.index(), wm, ctx);
        if (obj == null) return null;

        if (obj instanceof List<?> list) {
            int idx = toInt(index);
            if (idx < 0 || idx >= list.size()) return null;
            return list.get(idx);
        }
        if (obj instanceof Object[] arr) {
            int idx = toInt(index);
            if (idx < 0 || idx >= arr.length) return null;
            return arr[idx];
        }
        if (obj instanceof Map<?, ?> map) {
            return map.get(index instanceof Number n ? n.intValue() : index);
        }
        throw new RflParseException("Cannot index into " + obj.getClass().getSimpleName(), 0);
    }

    private Object evalFunctionCall(Expression.FunctionCall fc, WorkingMemory wm, EvaluationContext ctx) {
        Object[] args = new Object[fc.arguments().size()];
        for (int i = 0; i < fc.arguments().size(); i++) {
            args[i] = doEvaluate(fc.arguments().get(i), wm, ctx);
        }
        return functionRegistry.invoke(fc.name(), args, ctx);
    }

    @SuppressWarnings("unchecked")
    private Object evalBetween(Expression.BetweenExpr bet, WorkingMemory wm, EvaluationContext ctx) {
        Object value = doEvaluate(bet.value(), wm, ctx);
        Object low = doEvaluate(bet.low(), wm, ctx);
        Object high = doEvaluate(bet.high(), wm, ctx);

        if (value instanceof Number && low instanceof Number && high instanceof Number) {
            double v = ((Number) value).doubleValue();
            double lo = ((Number) low).doubleValue();
            double hi = ((Number) high).doubleValue();
            return lo <= v && v <= hi;
        }

        if (value instanceof Comparable cv && low instanceof Comparable cl && high instanceof Comparable ch) {
            return cl.compareTo(value) <= 0 && cv.compareTo(high) <= 0;
        }

        throw new RflParseException("BETWEEN requires comparable operands", 0);
    }

    private Object evalIn(Expression.InExpr in, WorkingMemory wm, EvaluationContext ctx) {
        Object value = doEvaluate(in.value(), wm, ctx);
        List<Object> evaluated = new ArrayList<>();
        for (Expression element : in.elements()) {
            evaluated.add(doEvaluate(element, wm, ctx));
        }
        boolean found = evaluated.stream().anyMatch(e -> nullSafeEquals(value, e));
        return in.negated() ? !found : found;
    }

    private Object evalNullCoalesce(Expression.NullCoalesce nc, WorkingMemory wm, EvaluationContext ctx) {
        Object value = doEvaluate(nc.value(), wm, ctx);
        return value != null ? value : doEvaluate(nc.defaultValue(), wm, ctx);
    }

    private Object evalArrayLiteral(Expression.ArrayLiteral arr, WorkingMemory wm, EvaluationContext ctx) {
        List<Object> result = new ArrayList<>(arr.elements().size());
        for (Expression element : arr.elements()) {
            result.add(doEvaluate(element, wm, ctx));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Object navigateProperty(Object obj, String property) {
        if (obj instanceof Map<?, ?> map) {
            return map.get(property);
        }
        // Could add reflection-based bean property access here if needed.
        throw new RflParseException(
                "Cannot access property '" + property + "' on " + obj.getClass().getSimpleName(), 0);
    }

    private static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof String s) return !s.isEmpty();
        return true;
    }

    private static double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) {
                throw new RflParseException("Cannot convert to number: " + val, 0);
            }
        }
        throw new RflParseException("Cannot convert to number: " + val, 0);
    }

    private static int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        throw new RflParseException("Cannot convert to int: " + val, 0);
    }

    private static Number negate(Number n) {
        if (n instanceof Integer i) return -i;
        if (n instanceof Long l) return -l;
        if (n instanceof Float f) return -f;
        return -n.doubleValue();
    }

    /**
     * Attempts to keep integer precision when both operands are integral
     * and the result has no fractional component.
     */
    private static Object coerceResult(double result, Object left, Object right) {
        boolean bothIntegral = (left instanceof Integer || left instanceof Long)
                && (right instanceof Integer || right instanceof Long);
        if (bothIntegral && result == Math.floor(result) && !Double.isInfinite(result)) {
            long longResult = (long) result;
            if (longResult >= Integer.MIN_VALUE && longResult <= Integer.MAX_VALUE) {
                return (int) longResult;
            }
            return longResult;
        }
        return result;
    }

    private static boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        return a.equals(b);
    }
}
