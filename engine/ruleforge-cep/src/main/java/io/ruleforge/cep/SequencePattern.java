package io.ruleforge.cep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects a specific sequence of events occurring in order.
 * Events must appear in the order defined by the event schemas
 * within the time window.
 */
public final class SequencePattern implements PatternMatcher {

    @Override
    public PatternType patternType() {
        return PatternType.SEQUENCE;
    }

    @Override
    public PatternMatchResult match(List<Map<String, Object>> events,
                                     List<EventSchema> eventSchemas,
                                     Map<String, Object> parameters) {
        if (eventSchemas.isEmpty() || events.isEmpty()) {
            return new PatternMatchResult(false, List.of(), Map.of());
        }

        List<Map<String, Object>> matchedEvents = new ArrayList<>();
        int schemaIndex = 0;

        for (Map<String, Object> event : events) {
            if (schemaIndex >= eventSchemas.size()) {
                break;
            }

            EventSchema expectedSchema = eventSchemas.get(schemaIndex);
            if (matchesSchema(event, expectedSchema)) {
                matchedEvents.add(event);
                schemaIndex++;
            }
        }

        boolean allMatched = schemaIndex >= eventSchemas.size();
        return new PatternMatchResult(
                allMatched,
                allMatched ? matchedEvents : List.of(),
                allMatched ? Map.of("sequenceLength", matchedEvents.size()) : Map.of()
        );
    }

    private boolean matchesSchema(Map<String, Object> event, EventSchema schema) {
        Object eventType = event.get("eventType");
        if (eventType == null || !schema.eventType().equals(eventType.toString())) {
            return false;
        }

        for (EventSchema.FilterCondition filter : schema.filterConditions()) {
            Object fieldValue = event.get(filter.field());
            if (!evaluateCondition(fieldValue, filter.operator(), filter.value())) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluateCondition(Object fieldValue, String operator, Object expected) {
        if (fieldValue == null) {
            return "IS_NULL".equalsIgnoreCase(operator);
        }
        return switch (operator.toUpperCase()) {
            case "EQ" -> fieldValue.toString().equals(expected.toString());
            case "NEQ" -> !fieldValue.toString().equals(expected.toString());
            case "GT" -> compareAsDouble(fieldValue, expected) > 0;
            case "GTE" -> compareAsDouble(fieldValue, expected) >= 0;
            case "LT" -> compareAsDouble(fieldValue, expected) < 0;
            case "LTE" -> compareAsDouble(fieldValue, expected) <= 0;
            case "CONTAINS" -> fieldValue.toString().contains(expected.toString());
            case "IS_NOT_NULL" -> true;
            default -> fieldValue.toString().equals(expected.toString());
        };
    }

    private double compareAsDouble(Object a, Object b) {
        double da = a instanceof Number n ? n.doubleValue() : Double.parseDouble(a.toString());
        double db = b instanceof Number n ? n.doubleValue() : Double.parseDouble(b.toString());
        return Double.compare(da, db);
    }
}
