package io.ruleforge.cep;

import java.util.List;
import java.util.Map;

/**
 * Defines the schema for events in a CEP pattern.
 *
 * @param eventType   the event type name (e.g. "login", "transaction", "alert")
 * @param fields      map of field name to field type (e.g. "amount" -> "NUMBER")
 * @param filterConditions  conditions that must be true for an event to match this schema
 */
public record EventSchema(
        String eventType,
        Map<String, String> fields,
        List<FilterCondition> filterConditions
) {
    public EventSchema {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
        fields = fields != null ? Map.copyOf(fields) : Map.of();
        filterConditions = filterConditions != null ? List.copyOf(filterConditions) : List.of();
    }

    /**
     * A filter condition applied to incoming events.
     *
     * @param field    the event field name
     * @param operator comparison operator (EQ, GT, LT, etc.)
     * @param value    the value to compare against
     */
    public record FilterCondition(String field, String operator, Object value) {}
}
