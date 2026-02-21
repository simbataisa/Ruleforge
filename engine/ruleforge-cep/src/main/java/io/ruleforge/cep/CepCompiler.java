package io.ruleforge.cep;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.model.RuleDefinition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link RuleDefinition} of type {@code CEP_RULE} into a {@link CompiledCep}.
 *
 * <p>Expected keys in {@link RuleDefinition#definition()}:
 * <ul>
 *   <li>{@code "patternType"} - String: SEQUENCE, AGGREGATION, FREQUENCY, ABSENCE, CONJUNCTION</li>
 *   <li>{@code "eventSchemas"} - List of event schema maps</li>
 *   <li>{@code "timeWindow"} - Map with keys: duration (seconds), type, graceTime (seconds)</li>
 *   <li>{@code "actions"} - List of action maps</li>
 *   <li>{@code "inputTopic"} - Kafka input topic</li>
 *   <li>{@code "outputTopic"} - Kafka output topic</li>
 *   <li>{@code "parameters"} - Additional pattern-specific parameters</li>
 * </ul>
 */
public final class CepCompiler {

    private final Map<PatternType, PatternMatcher> matchers;

    public CepCompiler() {
        matchers = new EnumMap<>(PatternType.class);
        matchers.put(PatternType.SEQUENCE, new SequencePattern());
        matchers.put(PatternType.AGGREGATION, new AggregationPattern());
        matchers.put(PatternType.FREQUENCY, new FrequencyPattern());
    }

    @SuppressWarnings("unchecked")
    public CompiledCep compile(RuleDefinition definition) {
        Map<String, Object> def = definition.definition();
        if (def == null) {
            throw new RuleCompilationException(definition.id(), "Definition payload is null");
        }

        PatternType patternType = parsePatternType(definition.id(), def);
        List<EventSchema> eventSchemas = parseEventSchemas(definition.id(), def);
        TimeWindow timeWindow = parseTimeWindow(definition.id(), def);
        List<CepAction> actions = parseActions(definition.id(), def);

        String inputTopic = def.containsKey("inputTopic")
                ? def.get("inputTopic").toString()
                : "rule.execution";
        String outputTopic = def.containsKey("outputTopic")
                ? def.get("outputTopic").toString()
                : "cep.results";

        Map<String, Object> parameters = def.containsKey("parameters") && def.get("parameters") instanceof Map
                ? (Map<String, Object>) def.get("parameters")
                : new LinkedHashMap<>();

        CepDefinition cepDef = new CepDefinition(
                definition.id(), definition.name(), patternType,
                eventSchemas, timeWindow, actions, inputTopic, outputTopic
        );

        PatternMatcher matcher = matchers.get(patternType);
        if (matcher == null) {
            throw new RuleCompilationException(definition.id(),
                    "No pattern matcher available for pattern type: " + patternType);
        }

        return new CompiledCep(
                definition.id(),
                definition.name(),
                definition.version(),
                definition.metadata(),
                System.currentTimeMillis(),
                cepDef,
                matcher,
                parameters
        );
    }

    private PatternType parsePatternType(String ruleId, Map<String, Object> def) {
        Object raw = def.get("patternType");
        if (raw == null) {
            throw new RuleCompilationException(ruleId, "Missing 'patternType' in CEP definition");
        }
        try {
            return PatternType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleCompilationException(ruleId,
                    "Unknown pattern type: " + raw + ". Valid: SEQUENCE, AGGREGATION, FREQUENCY, ABSENCE, CONJUNCTION");
        }
    }

    @SuppressWarnings("unchecked")
    private List<EventSchema> parseEventSchemas(String ruleId, Map<String, Object> def) {
        Object raw = def.get("eventSchemas");
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId, "'eventSchemas' must be a list");
        }

        List<EventSchema> schemas = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> map)) {
                throw new RuleCompilationException(ruleId,
                        "Event schema at index " + i + " must be a map");
            }
            Map<String, Object> schemaMap = (Map<String, Object>) map;

            String eventType = schemaMap.containsKey("eventType")
                    ? schemaMap.get("eventType").toString()
                    : "unknown";

            Map<String, String> fields = new LinkedHashMap<>();
            if (schemaMap.get("fields") instanceof Map<?, ?> fieldsMap) {
                for (Map.Entry<?, ?> entry : fieldsMap.entrySet()) {
                    fields.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }

            List<EventSchema.FilterCondition> filters = new ArrayList<>();
            if (schemaMap.get("filterConditions") instanceof List<?> filterList) {
                for (Object filterItem : filterList) {
                    if (filterItem instanceof Map<?, ?> filterMap) {
                        Map<String, Object> fm = (Map<String, Object>) filterMap;
                        filters.add(new EventSchema.FilterCondition(
                                fm.getOrDefault("field", "").toString(),
                                fm.getOrDefault("operator", "EQ").toString(),
                                fm.get("value")
                        ));
                    }
                }
            }

            schemas.add(new EventSchema(eventType, fields, filters));
        }
        return schemas;
    }

    @SuppressWarnings("unchecked")
    private TimeWindow parseTimeWindow(String ruleId, Map<String, Object> def) {
        Object raw = def.get("timeWindow");
        if (raw == null) {
            return TimeWindow.tumbling(Duration.ofMinutes(5));
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new RuleCompilationException(ruleId, "'timeWindow' must be a map");
        }
        Map<String, Object> twMap = (Map<String, Object>) map;

        long durationSeconds = twMap.containsKey("duration")
                ? ((Number) twMap.get("duration")).longValue()
                : 300;
        String type = twMap.containsKey("type")
                ? twMap.get("type").toString().toUpperCase()
                : "TUMBLING";
        long graceSeconds = twMap.containsKey("graceTime")
                ? ((Number) twMap.get("graceTime")).longValue()
                : 0;

        TimeWindow.WindowType windowType;
        try {
            windowType = TimeWindow.WindowType.valueOf(type);
        } catch (IllegalArgumentException e) {
            windowType = TimeWindow.WindowType.TUMBLING;
        }

        return new TimeWindow(
                Duration.ofSeconds(durationSeconds),
                windowType,
                Duration.ofSeconds(graceSeconds)
        );
    }

    @SuppressWarnings("unchecked")
    private List<CepAction> parseActions(String ruleId, Map<String, Object> def) {
        Object raw = def.get("actions");
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId, "'actions' must be a list");
        }

        List<CepAction> actions = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> actionMap = (Map<String, Object>) map;
                String actionType = actionMap.containsKey("actionType")
                        ? actionMap.get("actionType").toString()
                        : "EMIT_EVENT";
                Map<String, Object> params = actionMap.containsKey("parameters")
                        && actionMap.get("parameters") instanceof Map
                        ? (Map<String, Object>) actionMap.get("parameters")
                        : Map.of();
                actions.add(new CepAction(actionType, params));
            }
        }
        return actions;
    }
}
