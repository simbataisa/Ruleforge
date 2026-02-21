package io.ruleforge.dmn.feel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates S-FEEL AST nodes against a variable context.
 * <p>
 * Supports arithmetic, comparisons, boolean logic, string functions,
 * lists, contexts, ranges, if-then-else, and built-in functions.
 */
public final class FeelEvaluator {

    private final Map<String, Object> context;

    public FeelEvaluator(Map<String, Object> context) {
        this.context = context != null ? context : Map.of();
    }

    public Object evaluate(FeelNode node) {
        return switch (node) {
            case FeelNode.NumberNode n -> n.value();
            case FeelNode.StringNode s -> s.value();
            case FeelNode.BooleanNode b -> b.value();
            case FeelNode.NullNode ignored -> null;
            case FeelNode.WildcardNode ignored -> Boolean.TRUE;
            case FeelNode.IdentifierNode id -> resolveIdentifier(id.name());
            case FeelNode.BinaryNode bin -> evaluateBinary(bin);
            case FeelNode.UnaryMinusNode um -> evaluateUnaryMinus(um);
            case FeelNode.UnaryComparisonNode uc -> uc; // returned as-is for unary test matching
            case FeelNode.NotNode not -> evaluateNot(not);
            case FeelNode.BetweenNode btw -> evaluateBetween(btw);
            case FeelNode.IfNode ifn -> evaluateIf(ifn);
            case FeelNode.ListNode lst -> evaluateList(lst);
            case FeelNode.ContextNode ctx -> evaluateContext(ctx);
            case FeelNode.RangeNode rng -> rng; // returned as-is for unary test matching
            case FeelNode.PropertyAccessNode pa -> evaluatePropertyAccess(pa);
            case FeelNode.IndexAccessNode ia -> evaluateIndexAccess(ia);
            case FeelNode.FunctionCallNode fn -> evaluateFunction(fn);
        };
    }

    /**
     * Evaluates a unary test against an input value.
     * Returns true if the input matches the test.
     */
    public boolean evaluateUnaryTest(FeelNode testNode, Object inputValue) {
        if (testNode instanceof FeelNode.WildcardNode) {
            return true;
        }

        Object testResult = evaluate(testNode);

        if (testResult instanceof FeelNode.UnaryComparisonNode uc) {
            Object operandVal = evaluate(uc.operand());
            return compareWithOp(uc.operator(), inputValue, operandVal);
        }

        if (testResult instanceof FeelNode.RangeNode rng) {
            return evaluateInRange(inputValue, rng);
        }

        if (testResult instanceof List<?> list) {
            // Test if input is in the list
            for (Object item : list) {
                if (feelEquals(inputValue, item)) return true;
            }
            return false;
        }

        if (testResult instanceof Boolean b && testNode instanceof FeelNode.NotNode) {
            return b;
        }

        return feelEquals(inputValue, testResult);
    }

    // ---------------------------------------------------------------
    // Binary operations
    // ---------------------------------------------------------------

    private Object evaluateBinary(FeelNode.BinaryNode bin) {
        String op = bin.operator();

        // Short-circuit for logical operators
        if ("and".equals(op)) {
            Object left = evaluate(bin.left());
            if (!isTruthy(left)) return false;
            return isTruthy(evaluate(bin.right()));
        }
        if ("or".equals(op)) {
            Object left = evaluate(bin.left());
            if (isTruthy(left)) return true;
            return isTruthy(evaluate(bin.right()));
        }

        Object left = evaluate(bin.left());
        Object right = evaluate(bin.right());

        if ("in".equals(op)) {
            return evaluateIn(left, right);
        }

        // Arithmetic
        if (left instanceof Number nl && right instanceof Number nr) {
            double l = nl.doubleValue();
            double r = nr.doubleValue();
            return switch (op) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "/" -> r == 0 ? null : l / r;
                case "**" -> Math.pow(l, r);
                case "<" -> l < r;
                case ">" -> l > r;
                case "<=" -> l <= r;
                case ">=" -> l >= r;
                case "=" -> Double.compare(l, r) == 0;
                case "!=" -> Double.compare(l, r) != 0;
                default -> null;
            };
        }

        // String concatenation with +
        if ("+".equals(op) && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }

        // Comparison for all types
        return switch (op) {
            case "=" -> feelEquals(left, right);
            case "!=" -> !feelEquals(left, right);
            case "<" -> feelCompare(left, right) < 0;
            case ">" -> feelCompare(left, right) > 0;
            case "<=" -> feelCompare(left, right) <= 0;
            case ">=" -> feelCompare(left, right) >= 0;
            default -> null;
        };
    }

    private Object evaluateUnaryMinus(FeelNode.UnaryMinusNode um) {
        Object val = evaluate(um.operand());
        if (val instanceof Number n) {
            return -n.doubleValue();
        }
        return null;
    }

    private Object evaluateNot(FeelNode.NotNode not) {
        Object val = evaluate(not.operand());
        return !isTruthy(val);
    }

    private Object evaluateBetween(FeelNode.BetweenNode btw) {
        Object val = evaluate(btw.value());
        Object low = evaluate(btw.low());
        Object high = evaluate(btw.high());
        if (val == null || low == null || high == null) return null;
        return feelCompare(val, low) >= 0 && feelCompare(val, high) <= 0;
    }

    private Object evaluateIf(FeelNode.IfNode ifn) {
        Object condition = evaluate(ifn.condition());
        if (isTruthy(condition)) {
            return evaluate(ifn.thenBranch());
        } else {
            return evaluate(ifn.elseBranch());
        }
    }

    private List<Object> evaluateList(FeelNode.ListNode lst) {
        List<Object> result = new ArrayList<>();
        for (FeelNode elem : lst.elements()) {
            result.add(evaluate(elem));
        }
        return result;
    }

    private Map<String, Object> evaluateContext(FeelNode.ContextNode ctx) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : ctx.entries().entrySet()) {
            result.put(entry.getKey(), evaluate(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object evaluatePropertyAccess(FeelNode.PropertyAccessNode pa) {
        Object target = evaluate(pa.target());
        if (target instanceof Map<?, ?> map) {
            return map.get(pa.property());
        }
        return null;
    }

    private Object evaluateIndexAccess(FeelNode.IndexAccessNode ia) {
        Object target = evaluate(ia.target());
        Object index = evaluate(ia.index());
        if (target instanceof List<?> list && index instanceof Number n) {
            int idx = n.intValue();
            // FEEL uses 1-based indexing
            if (idx >= 1 && idx <= list.size()) {
                return list.get(idx - 1);
            }
            // Negative indexing from end
            if (idx < 0 && -idx <= list.size()) {
                return list.get(list.size() + idx);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Built-in functions
    // ---------------------------------------------------------------

    private Object evaluateFunction(FeelNode.FunctionCallNode fn) {
        List<Object> args = new ArrayList<>();
        for (FeelNode arg : fn.arguments()) {
            args.add(evaluate(arg));
        }

        return switch (fn.functionName()) {
            // String functions
            case "string length", "string_length" ->
                    args.getFirst() instanceof String s ? (double) s.length() : null;
            case "substring" -> evalSubstring(args);
            case "upper case", "upper_case" ->
                    args.getFirst() instanceof String s ? s.toUpperCase() : null;
            case "lower case", "lower_case" ->
                    args.getFirst() instanceof String s ? s.toLowerCase() : null;
            case "contains" ->
                    args.size() >= 2 && args.get(0) instanceof String s && args.get(1) instanceof String sub
                            ? s.contains(sub) : null;
            case "starts with", "starts_with" ->
                    args.size() >= 2 && args.get(0) instanceof String s && args.get(1) instanceof String prefix
                            ? s.startsWith(prefix) : null;
            case "ends with", "ends_with" ->
                    args.size() >= 2 && args.get(0) instanceof String s && args.get(1) instanceof String suffix
                            ? s.endsWith(suffix) : null;
            case "matches" ->
                    args.size() >= 2 && args.get(0) instanceof String s && args.get(1) instanceof String pattern
                            ? s.matches(pattern) : null;
            case "replace" -> evalReplace(args);
            case "trim" -> args.getFirst() instanceof String s ? s.trim() : null;

            // Numeric functions
            case "abs" -> args.getFirst() instanceof Number n ? Math.abs(n.doubleValue()) : null;
            case "ceiling" -> args.getFirst() instanceof Number n ? Math.ceil(n.doubleValue()) : null;
            case "floor" -> args.getFirst() instanceof Number n ? Math.floor(n.doubleValue()) : null;
            case "round" -> evalRound(args);
            case "modulo" ->
                    args.size() >= 2 && args.get(0) instanceof Number a && args.get(1) instanceof Number b
                            ? a.doubleValue() % b.doubleValue() : null;
            case "sqrt" -> args.getFirst() instanceof Number n ? Math.sqrt(n.doubleValue()) : null;
            case "log" -> args.getFirst() instanceof Number n ? Math.log(n.doubleValue()) : null;
            case "exp" -> args.getFirst() instanceof Number n ? Math.exp(n.doubleValue()) : null;

            // Number conversion
            case "number" -> args.getFirst() instanceof String s ? parseNumber(s) : null;
            case "string" -> args.getFirst() != null ? String.valueOf(args.getFirst()) : null;

            // List functions
            case "count" -> args.getFirst() instanceof List<?> l ? (double) l.size() : null;
            case "min" -> evalMinMax(args, true);
            case "max" -> evalMinMax(args, false);
            case "sum" -> evalSum(args);
            case "mean" -> evalMean(args);
            case "list contains", "list_contains" -> evalListContains(args);
            case "append" -> evalAppend(args);
            case "flatten" -> evalFlatten(args);
            case "distinct values", "distinct_values" -> evalDistinct(args);
            case "sort" -> evalSort(args);
            case "reverse" -> evalReverse(args);

            // Boolean functions
            case "not" -> args.getFirst() instanceof Boolean b ? !b : null;

            default -> null; // unknown function returns null
        };
    }

    private Object evalSubstring(List<Object> args) {
        if (args.size() < 2 || !(args.get(0) instanceof String s) || !(args.get(1) instanceof Number start)) {
            return null;
        }
        int from = start.intValue();
        // FEEL substring is 1-based
        if (from > 0) from--;
        else if (from < 0) from = s.length() + from;
        if (from < 0) from = 0;
        if (from >= s.length()) return "";

        if (args.size() >= 3 && args.get(2) instanceof Number len) {
            int end = Math.min(from + len.intValue(), s.length());
            return s.substring(from, end);
        }
        return s.substring(from);
    }

    private Object evalReplace(List<Object> args) {
        if (args.size() >= 3 && args.get(0) instanceof String s
                && args.get(1) instanceof String pattern && args.get(2) instanceof String replacement) {
            return s.replaceAll(pattern, replacement);
        }
        return null;
    }

    private Object evalRound(List<Object> args) {
        if (!(args.getFirst() instanceof Number n)) return null;
        int scale = args.size() >= 2 && args.get(1) instanceof Number s ? s.intValue() : 0;
        double factor = Math.pow(10, scale);
        return Math.round(n.doubleValue() * factor) / factor;
    }

    private Object evalMinMax(List<Object> args, boolean isMin) {
        List<Number> numbers = extractNumbers(args);
        if (numbers.isEmpty()) return null;
        return numbers.stream()
                .mapToDouble(Number::doubleValue)
                .reduce(isMin ? Double::min : Double::max)
                .orElse(0.0);
    }

    private Object evalSum(List<Object> args) {
        List<Number> numbers = extractNumbers(args);
        if (numbers.isEmpty()) return null;
        return numbers.stream().mapToDouble(Number::doubleValue).sum();
    }

    private Object evalMean(List<Object> args) {
        List<Number> numbers = extractNumbers(args);
        if (numbers.isEmpty()) return null;
        return numbers.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
    }

    private Object evalListContains(List<Object> args) {
        if (args.size() < 2 || !(args.get(0) instanceof List<?> list)) return null;
        Object item = args.get(1);
        return list.stream().anyMatch(e -> feelEquals(e, item));
    }

    private Object evalAppend(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> list)) return null;
        List<Object> result = new ArrayList<>(list);
        for (int i = 1; i < args.size(); i++) {
            result.add(args.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object evalFlatten(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> list)) return null;
        List<Object> result = new ArrayList<>();
        flattenInto(list, result);
        return result;
    }

    private void flattenInto(List<?> list, List<Object> result) {
        for (Object item : list) {
            if (item instanceof List<?> nested) {
                flattenInto(nested, result);
            } else {
                result.add(item);
            }
        }
    }

    private Object evalDistinct(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> list)) return null;
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            boolean found = false;
            for (Object existing : result) {
                if (feelEquals(item, existing)) { found = true; break; }
            }
            if (!found) result.add(item);
        }
        return result;
    }

    private Object evalSort(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> list)) return null;
        List<Object> result = new ArrayList<>(list);
        result.sort((a, b) -> feelCompare(a, b));
        return result;
    }

    private Object evalReverse(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> list)) return null;
        List<Object> result = new ArrayList<>(list);
        java.util.Collections.reverse(result);
        return result;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Object resolveIdentifier(String name) {
        // Support dot paths in context
        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            Object current = context.get(parts[0]);
            for (int i = 1; i < parts.length && current instanceof Map; i++) {
                current = ((Map<?, ?>) current).get(parts[i]);
            }
            return current;
        }
        return context.get(name);
    }

    private boolean evaluateIn(Object left, Object right) {
        if (right instanceof FeelNode.RangeNode range) {
            return evaluateInRange(left, range);
        }
        if (right instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof FeelNode.RangeNode range) {
                    if (evaluateInRange(left, range)) return true;
                } else if (feelEquals(left, item)) return true;
            }
            return false;
        }
        return feelEquals(left, right);
    }

    private boolean evaluateInRange(Object value, FeelNode.RangeNode range) {
        Object start = evaluate(range.start());
        Object end = evaluate(range.end());
        if (value == null || start == null || end == null) return false;
        int cmpStart = feelCompare(value, start);
        int cmpEnd = feelCompare(value, end);
        boolean startOk = range.startInclusive() ? cmpStart >= 0 : cmpStart > 0;
        boolean endOk = range.endInclusive() ? cmpEnd <= 0 : cmpEnd < 0;
        return startOk && endOk;
    }

    private boolean compareWithOp(String op, Object left, Object right) {
        if (left == null || right == null) return false;
        int cmp = feelCompare(left, right);
        return switch (op) {
            case "<" -> cmp < 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case ">=" -> cmp >= 0;
            case "=" -> cmp == 0;
            case "!=" -> cmp != 0;
            default -> false;
        };
    }

    private boolean feelEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        if (a instanceof String sa && b instanceof String sb) {
            return sa.equals(sb);
        }
        return a.equals(b);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int feelCompare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable ca && b instanceof Comparable cb) {
            return ca.compareTo(cb);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.doubleValue() != 0;
        if (val instanceof String s) return !s.isEmpty();
        return true;
    }

    private Double parseNumber(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Number> extractNumbers(List<Object> args) {
        List<Number> numbers = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Number n) numbers.add(n);
                }
            } else if (arg instanceof Number n) {
                numbers.add(n);
            }
        }
        return numbers;
    }
}
