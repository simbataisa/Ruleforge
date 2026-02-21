package io.ruleforge.cep;

import java.util.List;
import java.util.Map;

/**
 * Base interface for CEP pattern matchers. Each pattern type (sequence, aggregation,
 * frequency) has its own implementation.
 */
public interface PatternMatcher {

    /**
     * Returns the pattern type this matcher handles.
     */
    PatternType patternType();

    /**
     * Evaluates a buffered set of events against the pattern definition.
     *
     * @param events        the events within the current time window
     * @param eventSchemas  the expected event schemas
     * @param parameters    additional pattern parameters
     * @return a match result, or null if no match
     */
    PatternMatchResult match(List<Map<String, Object>> events,
                              List<EventSchema> eventSchemas,
                              Map<String, Object> parameters);

    /**
     * Result of a pattern match evaluation.
     *
     * @param matched       whether the pattern was matched
     * @param matchedEvents the events that contributed to the match
     * @param output        computed output values (e.g. aggregation results)
     */
    record PatternMatchResult(
            boolean matched,
            List<Map<String, Object>> matchedEvents,
            Map<String, Object> output
    ) {
        public PatternMatchResult {
            matchedEvents = matchedEvents != null ? List.copyOf(matchedEvents) : List.of();
            output = output != null ? Map.copyOf(output) : Map.of();
        }
    }
}
