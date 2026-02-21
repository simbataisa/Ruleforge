package io.ruleforge.cep;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates event field values over a time window and evaluates threshold conditions.
 * Supports SUM, AVG, MIN, MAX, COUNT aggregation functions.
 */
public final class AggregationPattern implements PatternMatcher {

    @Override
    public PatternType patternType() {
        return PatternType.AGGREGATION;
    }

    @Override
    public PatternMatchResult match(List<Map<String, Object>> events,
                                     List<EventSchema> eventSchemas,
                                     Map<String, Object> parameters) {
        if (events.isEmpty()) {
            return new PatternMatchResult(false, List.of(), Map.of());
        }

        String aggregationField = parameters != null
                ? String.valueOf(parameters.getOrDefault("field", "value"))
                : "value";
        String aggregationFunction = parameters != null
                ? String.valueOf(parameters.getOrDefault("function", "COUNT"))
                : "COUNT";
        String thresholdOperator = parameters != null
                ? String.valueOf(parameters.getOrDefault("thresholdOperator", "GTE"))
                : "GTE";
        double thresholdValue = parameters != null && parameters.containsKey("thresholdValue")
                ? ((Number) parameters.get("thresholdValue")).doubleValue()
                : 0.0;

        // Filter events matching schemas
        List<Map<String, Object>> matchingEvents = filterEvents(events, eventSchemas);

        // Extract numeric values
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> event : matchingEvents) {
            Object val = event.get(aggregationField);
            if (val instanceof Number n) {
                values.add(n.doubleValue());
            }
        }

        // Compute aggregation
        double result = computeAggregation(aggregationFunction, values, matchingEvents.size());

        // Evaluate threshold
        boolean thresholdMet = evaluateThreshold(result, thresholdOperator, thresholdValue);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("aggregationFunction", aggregationFunction);
        output.put("aggregationField", aggregationField);
        output.put("result", result);
        output.put("eventCount", matchingEvents.size());

        return new PatternMatchResult(thresholdMet, matchingEvents, output);
    }

    private List<Map<String, Object>> filterEvents(List<Map<String, Object>> events,
                                                     List<EventSchema> schemas) {
        if (schemas.isEmpty()) {
            return events;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> event : events) {
            for (EventSchema schema : schemas) {
                Object eventType = event.get("eventType");
                if (eventType != null && schema.eventType().equals(eventType.toString())) {
                    filtered.add(event);
                    break;
                }
            }
        }
        return filtered;
    }

    private double computeAggregation(String function, List<Double> values, int totalCount) {
        return switch (function.toUpperCase()) {
            case "SUM" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "AVG" -> {
                if (values.isEmpty()) yield 0.0;
                DoubleSummaryStatistics stats = values.stream()
                        .mapToDouble(Double::doubleValue).summaryStatistics();
                yield stats.getAverage();
            }
            case "MIN" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case "MAX" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case "COUNT" -> totalCount;
            default -> totalCount;
        };
    }

    private boolean evaluateThreshold(double result, String operator, double threshold) {
        return switch (operator.toUpperCase()) {
            case "EQ" -> Double.compare(result, threshold) == 0;
            case "NEQ" -> Double.compare(result, threshold) != 0;
            case "GT" -> result > threshold;
            case "GTE" -> result >= threshold;
            case "LT" -> result < threshold;
            case "LTE" -> result <= threshold;
            default -> result >= threshold;
        };
    }
}
