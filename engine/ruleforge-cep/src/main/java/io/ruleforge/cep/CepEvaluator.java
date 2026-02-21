package io.ruleforge.cep;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.EvaluationContext;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a compiled CEP rule by running its pattern matcher against events
 * from working memory and then executing any configured actions.
 */
public final class CepEvaluator {

    /**
     * Evaluates a compiled CEP rule.
     *
     * @param compiled      the compiled CEP rule
     * @param workingMemory the current working memory (events stored under "cep.events")
     * @param ctx           the evaluation context
     * @return the rule result
     */
    @SuppressWarnings("unchecked")
    public RuleResult evaluate(CompiledCep compiled, WorkingMemory workingMemory, EvaluationContext ctx) {
        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(compiled.ruleId());

        // Retrieve buffered events from working memory
        Object eventsObj = workingMemory.get("cep.events");
        List<Map<String, Object>> events = eventsObj instanceof List<?>
                ? (List<Map<String, Object>>) eventsObj
                : List.of();

        CepDefinition cepDef = compiled.cepDefinition();

        // Run pattern matcher
        PatternMatcher.PatternMatchResult matchResult = compiled.patternMatcher().match(
                events, cepDef.eventSchemas(), compiled.patternParameters());

        // Build output
        Map<String, Object> output = new LinkedHashMap<>(matchResult.output());
        output.put("patternMatched", matchResult.matched());
        output.put("patternType", cepDef.patternType().name());
        output.put("matchedEventCount", matchResult.matchedEvents().size());

        // If matched, include action specifications in output
        if (matchResult.matched() && !cepDef.actions().isEmpty()) {
            output.put("triggeredActions", cepDef.actions().stream()
                    .map(a -> {
                        Map<String, Object> actionInfo = new LinkedHashMap<>();
                        actionInfo.put("actionType", a.actionType());
                        actionInfo.put("parameters", a.parameters());
                        return actionInfo;
                    })
                    .toList());
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();

        RuleResultMetadata resultMetadata = new RuleResultMetadata(
                elapsed, events.size(), matchResult.matched() ? 1 : 0, 0, "CEP"
        );

        return new RuleResult(output, resultMetadata, List.of(), trace);
    }
}
