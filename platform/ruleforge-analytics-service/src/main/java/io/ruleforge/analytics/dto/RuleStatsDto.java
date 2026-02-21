package io.ruleforge.analytics.dto;

import java.time.Instant;

/**
 * Aggregated statistics for a single rule over a given time window.
 */
public record RuleStatsDto(
        String ruleId,
        String ruleName,
        long totalExecutions,
        double avgExecutionTimeMs,
        double errorRate,
        Instant lastExecuted
) {}
