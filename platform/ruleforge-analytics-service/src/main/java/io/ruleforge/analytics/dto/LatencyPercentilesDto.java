package io.ruleforge.analytics.dto;

public record LatencyPercentilesDto(
        double p50,
        double p75,
        double p90,
        double p95,
        double p99
) {}
