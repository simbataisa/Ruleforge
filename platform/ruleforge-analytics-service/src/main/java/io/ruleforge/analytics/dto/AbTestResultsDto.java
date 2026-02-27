package io.ruleforge.analytics.dto;

public record AbTestResultsDto(
        String testId,
        VariantStatsDto versionA,
        VariantStatsDto versionB,
        long totalSamples
) {}
