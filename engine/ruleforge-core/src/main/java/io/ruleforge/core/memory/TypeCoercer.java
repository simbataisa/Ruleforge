package io.ruleforge.core.memory;

import io.ruleforge.core.model.FactType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility for coercing values to the Java type that corresponds to a given {@link FactType}.
 *
 * <p>All conversions rely solely on JDK classes. Null input always yields {@code null}.
 * An {@link IllegalArgumentException} is thrown when the conversion is not possible.
 */
public final class TypeCoercer {

    private TypeCoercer() {
        // utility class
    }

    /**
     * Coerces {@code value} to the Java type associated with {@code targetType}.
     *
     * @param value      the value to coerce (may be {@code null})
     * @param targetType the desired {@link FactType}
     * @param <T>        the expected return type
     * @return the coerced value, or {@code null} if the input is {@code null}
     * @throws IllegalArgumentException if the conversion is not supported
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object value, FactType targetType) {
        if (value == null) {
            return null;
        }

        return (T) switch (targetType) {
            case STRING   -> coerceToString(value);
            case NUMBER   -> coerceToNumber(value);
            case INTEGER  -> coerceToInteger(value);
            case BOOLEAN  -> coerceToBoolean(value);
            case DATE     -> coerceToDate(value);
            case DATETIME -> coerceToDatetime(value);
            case MONEY    -> coerceToMoney(value);
            case ARRAY    -> coerceToArray(value);
            case OBJECT   -> coerceToObject(value);
            case ENUM     -> coerceToString(value);
            case ANY      -> value;
        };
    }

    // ------------------------------------------------------------------ String

    private static String coerceToString(Object value) {
        return value.toString();
    }

    // ------------------------------------------------------------------ Number (Double)

    private static Double coerceToNumber(Object value) {
        if (value instanceof Double d) {
            return d;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Cannot coerce String to NUMBER: \"" + s + "\"", e);
            }
        }
        if (value instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to NUMBER");
    }

    // ------------------------------------------------------------------ Integer (Long)

    private static Long coerceToInteger(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            // Truncate fractional part
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                // Try as long first, fall back to parsing as double and truncating
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e1) {
                try {
                    return (long) Double.parseDouble(s.trim());
                } catch (NumberFormatException e2) {
                    throw new IllegalArgumentException(
                        "Cannot coerce String to INTEGER: \"" + s + "\"", e2);
                }
            }
        }
        if (value instanceof Boolean b) {
            return b ? 1L : 0L;
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to INTEGER");
    }

    // ------------------------------------------------------------------ Boolean

    private static Boolean coerceToBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            String trimmed = s.trim().toLowerCase();
            if ("true".equals(trimmed)) return Boolean.TRUE;
            if ("false".equals(trimmed)) return Boolean.FALSE;
            throw new IllegalArgumentException(
                "Cannot coerce String to BOOLEAN: \"" + s + "\" (expected \"true\" or \"false\")");
        }
        if (value instanceof Number n) {
            // 0 is false, everything else is true
            return n.doubleValue() != 0.0;
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to BOOLEAN");
    }

    // ------------------------------------------------------------------ Date (LocalDate)

    private static LocalDate coerceToDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof String s) {
            try {
                return LocalDate.parse(s.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Cannot coerce String to DATE: \"" + s + "\" (expected ISO format, e.g. 2026-02-20)", e);
            }
        }
        if (value instanceof Instant inst) {
            return inst.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to DATE");
    }

    // ------------------------------------------------------------------ Datetime (Instant)

    private static Instant coerceToDatetime(Object value) {
        if (value instanceof Instant inst) {
            return inst;
        }
        if (value instanceof String s) {
            try {
                return Instant.parse(s.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Cannot coerce String to DATETIME: \"" + s + "\" (expected ISO-8601 instant, e.g. 2026-02-20T10:15:30Z)", e);
            }
        }
        if (value instanceof LocalDate ld) {
            return ld.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }
        if (value instanceof Number n) {
            // Treat as epoch millis
            return Instant.ofEpochMilli(n.longValue());
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to DATETIME");
    }

    // ------------------------------------------------------------------ Money (BigDecimal)

    private static BigDecimal coerceToMoney(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Long l) {
            return BigDecimal.valueOf(l);
        }
        if (value instanceof Integer i) {
            return BigDecimal.valueOf(i);
        }
        if (value instanceof Double d) {
            return BigDecimal.valueOf(d);
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Cannot coerce String to MONEY: \"" + s + "\"", e);
            }
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to MONEY");
    }

    // ------------------------------------------------------------------ Array (List)

    @SuppressWarnings("unchecked")
    private static List<?> coerceToArray(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Object[] array) {
            List<Object> result = new ArrayList<>(array.length);
            Collections.addAll(result, array);
            return result;
        }
        // Wrap a single value in a singleton list
        return List.of(value);
    }

    // ------------------------------------------------------------------ Object (Map)

    private static Map<?, ?> coerceToObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalArgumentException(
            "Cannot coerce " + value.getClass().getSimpleName() + " to OBJECT (expected a Map)");
    }
}
