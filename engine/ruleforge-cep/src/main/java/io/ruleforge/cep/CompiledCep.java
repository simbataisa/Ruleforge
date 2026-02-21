package io.ruleforge.cep;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A compiled CEP rule. Holds the CEP definition and the instantiated pattern matcher.
 */
public final class CompiledCep extends CompiledRule {

    private final CepDefinition cepDefinition;
    private final PatternMatcher patternMatcher;
    private final Map<String, Object> patternParameters;
    private final String ruleName;
    private final int ruleVersion;

    public CompiledCep(String ruleId,
                        String ruleName,
                        int ruleVersion,
                        RuleMetadata metadata,
                        long compiledAt,
                        CepDefinition cepDefinition,
                        PatternMatcher patternMatcher,
                        Map<String, Object> patternParameters) {
        super(ruleId, RuleType.CEP_RULE, metadata, compiledAt);
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
        this.cepDefinition = cepDefinition;
        this.patternMatcher = patternMatcher;
        this.patternParameters = patternParameters != null
                ? new LinkedHashMap<>(patternParameters)
                : new LinkedHashMap<>();
    }

    public CepDefinition cepDefinition() {
        return cepDefinition;
    }

    public PatternMatcher patternMatcher() {
        return patternMatcher;
    }

    public Map<String, Object> patternParameters() {
        return patternParameters;
    }

    public String ruleName() {
        return ruleName;
    }

    public int ruleVersion() {
        return ruleVersion;
    }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(ruleId());

        // Retrieve buffered events from working memory
        Object eventsObj = workingMemory.get("cep.events");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = eventsObj instanceof List<?>
                ? (List<Map<String, Object>>) eventsObj
                : List.of();

        // Match the pattern
        PatternMatcher.PatternMatchResult matchResult = patternMatcher.match(
                events, cepDefinition.eventSchemas(), patternParameters);

        Map<String, Object> output = new LinkedHashMap<>(matchResult.output());
        output.put("patternMatched", matchResult.matched());
        output.put("patternType", cepDefinition.patternType().name());

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();

        RuleResultMetadata resultMetadata = new RuleResultMetadata(
                elapsed, events.size(), matchResult.matched() ? 1 : 0, 0, "CEP"
        );

        return new RuleResult(output, resultMetadata, List.of(), trace);
    }
}
