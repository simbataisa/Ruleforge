package io.ruleforge.analytics.dto;

import java.util.List;

/**
 * Aggregated dashboard metrics for an organization over a given time window.
 */
public record DashboardMetricsDto(
        long totalDecisions,
        double decisionsPerMinute,
        double avgExecutionTimeMs,
        double errorRate,
        List<RuleStatsDto> topRules
) {}
