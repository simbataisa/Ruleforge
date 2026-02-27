package io.ruleforge.analytics.dto;

public record VariantStatsDto(
        String variant,
        long executions,
        long errors,
        double errorRate,
        double avgLatencyMs,
        double p95LatencyMs
) {}
