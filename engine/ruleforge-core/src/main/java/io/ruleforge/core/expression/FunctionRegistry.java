package io.ruleforge.core.expression;

import io.ruleforge.core.spi.EvaluationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Registry of named functions callable from RFL expressions.
 *
 * <p>Ships with a comprehensive set of built-in math, string, date,
 * logical and array functions. Custom functions can be registered at
 * runtime via {@link #register(String, BiFunction)}.
 */
public class FunctionRegistry {

    private final Map<String, BiFunction<Object[], EvaluationContext, Object>> functions = new HashMap<>();

    public FunctionRegistry() {
        registerBuiltins();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Registers a named function. Overwrites any previously registered
     * function with the same name.
     */
    public void register(String name, BiFunction<Object[], EvaluationContext, Object> fn) {
        functions.put(name.toUpperCase(), fn);
    }

    /**
     * Invokes a registered function by name.
     *
     * @throws RflParseException if the function is not registered
     */
    public Object invoke(String name, Object[] args, EvaluationContext ctx) {
        BiFunction<Object[], EvaluationContext, Object> fn = functions.get(name.toUpperCase());
        if (fn == null) {
            throw new RflParseException("Unknown function: " + name, 0);
        }
        return fn.apply(args, ctx);
    }

    /** Returns {@code true} if a function with the given name exists. */
    public boolean hasFunction(String name) {
        return functions.containsKey(name.toUpperCase());
    }

    // ------------------------------------------------------------------
    // Built-in function registration
    // ------------------------------------------------------------------

    private void registerBuiltins() {
        // ---- Math ----
        register("ABS", (args, ctx) -> {
            requireArgs("ABS", args, 1);
            return Math.abs(toDouble(args[0]));
        });

        register("ROUND", (args, ctx) -> {
            requireArgs("ROUND", args, 1, 2);
            double value = toDouble(args[0]);
            int decimals = args.length > 1 ? toInt(args[1]) : 0;
            double factor = Math.pow(10, decimals);
            return Math.round(value * factor) / factor;
        });

        register("MIN", (args, ctx) -> {
            requireArgs("MIN", args, 2);
            return Math.min(toDouble(args[0]), toDouble(args[1]));
        });

        register("MAX", (args, ctx) -> {
            requireArgs("MAX", args, 2);
            return Math.max(toDouble(args[0]), toDouble(args[1]));
        });

        register("FLOOR", (args, ctx) -> {
            requireArgs("FLOOR", args, 1);
            return Math.floor(toDouble(args[0]));
        });

        register("CEIL", (args, ctx) -> {
            requireArgs("CEIL", args, 1);
            return Math.ceil(toDouble(args[0]));
        });

        register("SUM", (args, ctx) -> {
            requireArgs("SUM", args, 1);
            return sumCollection(args[0]);
        });

        // ---- String ----
        register("UPPER", (args, ctx) -> {
            requireArgs("UPPER", args, 1);
            return toString(args[0]).toUpperCase();
        });

        register("LOWER", (args, ctx) -> {
            requireArgs("LOWER", args, 1);
            return toString(args[0]).toLowerCase();
        });

        register("TRIM", (args, ctx) -> {
            requireArgs("TRIM", args, 1);
            return toString(args[0]).trim();
        });

        register("LENGTH", (args, ctx) -> {
            requireArgs("LENGTH", args, 1);
            Object val = args[0];
            if (val instanceof Collection<?> c) return c.size();
            if (val instanceof String s) return s.length();
            return toString(val).length();
        });

        register("SUBSTRING", (args, ctx) -> {
            requireArgs("SUBSTRING", args, 2, 3);
            String s = toString(args[0]);
            int start = toInt(args[1]);
            if (args.length > 2) {
                int end = toInt(args[2]);
                return s.substring(start, Math.min(end, s.length()));
            }
            return s.substring(start);
        });

        register("CONCAT", (args, ctx) -> {
            requireArgs("CONCAT", args, 2);
            return toString(args[0]) + toString(args[1]);
        });

        // ---- Date ----
        register("NOW", (args, ctx) -> Instant.now());

        register("DATE_DIFF", (args, ctx) -> {
            requireArgs("DATE_DIFF", args, 3);
            Instant d1 = toInstant(args[0]);
            Instant d2 = toInstant(args[1]);
            String unit = toString(args[2]).toUpperCase();
            ChronoUnit chronoUnit = switch (unit) {
                case "DAYS" -> ChronoUnit.DAYS;
                case "HOURS" -> ChronoUnit.HOURS;
                case "MINUTES" -> ChronoUnit.MINUTES;
                case "SECONDS" -> ChronoUnit.SECONDS;
                default -> throw new RflParseException("Unknown date unit: " + unit, 0);
            };
            return chronoUnit.between(d1, d2);
        });

        register("DATE_ADD", (args, ctx) -> {
            requireArgs("DATE_ADD", args, 3);
            Instant date = toInstant(args[0]);
            long amount = toLong(args[1]);
            String unit = toString(args[2]).toUpperCase();
            ChronoUnit chronoUnit = switch (unit) {
                case "DAYS" -> ChronoUnit.DAYS;
                case "HOURS" -> ChronoUnit.HOURS;
                case "MINUTES" -> ChronoUnit.MINUTES;
                case "SECONDS" -> ChronoUnit.SECONDS;
                default -> throw new RflParseException("Unknown date unit: " + unit, 0);
            };
            return date.plus(amount, chronoUnit);
        });

        // ---- Logical ----
        register("IF", (args, ctx) -> {
            requireArgs("IF", args, 3);
            boolean condition = toBool(args[0]);
            return condition ? args[1] : args[2];
        });

        register("COALESCE", (args, ctx) -> {
            for (Object arg : args) {
                if (arg != null) return arg;
            }
            return null;
        });

        // ---- Array / collection ----
        register("COUNT", (args, ctx) -> {
            requireArgs("COUNT", args, 1);
            Object val = args[0];
            if (val instanceof Collection<?> c) return c.size();
            if (val instanceof Object[] a) return a.length;
            return 0;
        });

        register("AVG", (args, ctx) -> {
            requireArgs("AVG", args, 1);
            List<?> list = toList(args[0]);
            if (list.isEmpty()) return 0.0;
            double sum = 0;
            for (Object item : list) {
                sum += toDouble(item);
            }
            return sum / list.size();
        });

        register("FIRST", (args, ctx) -> {
            requireArgs("FIRST", args, 1);
            List<?> list = toList(args[0]);
            return list.isEmpty() ? null : list.get(0);
        });

        register("LAST", (args, ctx) -> {
            requireArgs("LAST", args, 1);
            List<?> list = toList(args[0]);
            return list.isEmpty() ? null : list.get(list.size() - 1);
        });
    }

    // ------------------------------------------------------------------
    // Type coercion helpers
    // ------------------------------------------------------------------

    private static void requireArgs(String name, Object[] args, int exact) {
        if (args.length != exact) {
            throw new RflParseException(
                    name + " expects " + exact + " argument(s) but got " + args.length, 0);
        }
    }

    private static void requireArgs(String name, Object[] args, int min, int max) {
        if (args.length < min || args.length > max) {
            throw new RflParseException(
                    name + " expects " + min + "-" + max + " argument(s) but got " + args.length, 0);
        }
    }

    private static double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) return Double.parseDouble(s);
        throw new RflParseException("Cannot convert to number: " + val, 0);
    }

    private static int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) return Integer.parseInt(s);
        throw new RflParseException("Cannot convert to int: " + val, 0);
    }

    private static long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        throw new RflParseException("Cannot convert to long: " + val, 0);
    }

    private static boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        if (val == null) return false;
        if (val instanceof Number n) return n.doubleValue() != 0;
        if (val instanceof String s) return !s.isEmpty();
        return true;
    }

    private static String toString(Object val) {
        return val == null ? "" : val.toString();
    }

    private static Instant toInstant(Object val) {
        if (val instanceof Instant i) return i;
        if (val instanceof LocalDate ld) return ld.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        if (val instanceof String s) return Instant.parse(s);
        throw new RflParseException("Cannot convert to Instant: " + val, 0);
    }

    @SuppressWarnings("unchecked")
    private static List<?> toList(Object val) {
        if (val instanceof List<?> l) return l;
        if (val instanceof Collection<?> c) return List.copyOf(c);
        if (val instanceof Object[] a) return List.of(a);
        throw new RflParseException("Cannot convert to list: " + val, 0);
    }

    private static double sumCollection(Object val) {
        List<?> list = toList(val);
        double sum = 0;
        for (Object item : list) {
            sum += toDouble(item);
        }
        return sum;
    }
}
