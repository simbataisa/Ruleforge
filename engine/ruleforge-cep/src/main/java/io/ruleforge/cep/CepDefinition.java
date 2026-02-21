package io.ruleforge.cep;

import java.util.List;

/**
 * Defines a complete CEP rule with event schemas, pattern, time window, and actions.
 *
 * @param cepRuleId      unique identifier for this CEP rule
 * @param name           human-readable name
 * @param patternType    the type of pattern to detect
 * @param eventSchemas   the event schemas involved in the pattern
 * @param timeWindow     the time window for pattern detection
 * @param actions        actions to trigger on pattern match
 * @param inputTopic     the Kafka topic to consume events from
 * @param outputTopic    the Kafka topic to produce results to
 */
public record CepDefinition(
        String cepRuleId,
        String name,
        PatternType patternType,
        List<EventSchema> eventSchemas,
        TimeWindow timeWindow,
        List<CepAction> actions,
        String inputTopic,
        String outputTopic
) {
    public CepDefinition {
        if (patternType == null) {
            throw new IllegalArgumentException("Pattern type must not be null");
        }
        eventSchemas = eventSchemas != null ? List.copyOf(eventSchemas) : List.of();
        actions = actions != null ? List.copyOf(actions) : List.of();
    }
}
