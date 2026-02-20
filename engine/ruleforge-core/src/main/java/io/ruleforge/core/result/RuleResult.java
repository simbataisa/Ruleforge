package io.ruleforge.core.result;

import java.util.List;
import java.util.Map;

public record RuleResult(
    Map<String, Object> output,
    RuleResultMetadata metadata,
    List<MatchedRule> matchedRules,
    DecisionTrace trace
) {
    public RuleResult {
        output = output == null ? Map.of() : Map.copyOf(output);
        matchedRules = matchedRules == null ? List.of() : List.copyOf(matchedRules);
    }

    public static RuleResult empty() {
        return new RuleResult(Map.of(), RuleResultMetadata.empty(), List.of(), null);
    }

    public boolean hasOutput() {
        return output != null && !output.isEmpty();
    }
}
