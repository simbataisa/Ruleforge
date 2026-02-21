package io.ruleforge.cep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects when events of a given type occur a specified number of times
 * within the time window.
 */
public final class FrequencyPattern implements PatternMatcher {

    @Override
    public PatternType patternType() {
        return PatternType.FREQUENCY;
    }

    @Override
    public PatternMatchResult match(List<Map<String, Object>> events,
                                     List<EventSchema> eventSchemas,
                                     Map<String, Object> parameters) {
        if (events.isEmpty() || eventSchemas.isEmpty()) {
            return new PatternMatchResult(false, List.of(), Map.of());
        }

        int minOccurrences = parameters != null && parameters.containsKey("minOccurrences")
                ? ((Number) parameters.get("minOccurrences")).intValue()
                : 1;
        int maxOccurrences = parameters != null && parameters.containsKey("maxOccurrences")
                ? ((Number) parameters.get("maxOccurrences")).intValue()
                : Integer.MAX_VALUE;

        // Count events matching each schema
        EventSchema targetSchema = eventSchemas.getFirst();
        List<Map<String, Object>> matchingEvents = new ArrayList<>();

        for (Map<String, Object> event : events) {
            Object eventType = event.get("eventType");
            if (eventType != null && targetSchema.eventType().equals(eventType.toString())) {
                if (matchesFilters(event, targetSchema)) {
                    matchingEvents.add(event);
                }
            }
        }

        int count = matchingEvents.size();
        boolean matched = count >= minOccurrences && count <= maxOccurrences;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("eventType", targetSchema.eventType());
        output.put("occurrences", count);
        output.put("minOccurrences", minOccurrences);
        output.put("maxOccurrences", maxOccurrences);

        return new PatternMatchResult(matched, matched ? matchingEvents : List.of(), output);
    }

    private boolean matchesFilters(Map<String, Object> event, EventSchema schema) {
        for (EventSchema.FilterCondition filter : schema.filterConditions()) {
            Object fieldValue = event.get(filter.field());
            if (fieldValue == null) {
                return false;
            }
            if ("EQ".equalsIgnoreCase(filter.operator())
                    && !fieldValue.toString().equals(filter.value().toString())) {
                return false;
            }
        }
        return true;
    }
}
