package io.ruleforge.core.result;

import java.time.Duration;

public record RuleResultMetadata(
    Duration executionTime,
    int rulesEvaluated,
    int rulesFired,
    int forwardChainingIterations,
    String strategy
) {
    public static RuleResultMetadata empty() {
        return new RuleResultMetadata(Duration.ZERO, 0, 0, 0, "NONE");
    }
}
