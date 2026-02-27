package io.ruleforge.analytics.dto;

public record TypeDistributionDto(
        String ruleType,
        long count,
        double percentage
) {}
