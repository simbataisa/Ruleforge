package io.ruleforge.core.result;

import io.ruleforge.core.model.PriorityVector;
import java.util.Map;

public record MatchedRule(
    String ruleId,
    String ruleName,
    int version,
    PriorityVector priority,
    Map<String, Object> matchedConditions,
    Map<String, Object> producedOutput
) {
    public MatchedRule {
        matchedConditions = matchedConditions == null ? Map.of() : Map.copyOf(matchedConditions);
        producedOutput = producedOutput == null ? Map.of() : Map.copyOf(producedOutput);
    }
}
