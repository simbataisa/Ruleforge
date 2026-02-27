package io.ruleforge.analytics.dto;

public record SignificanceDto(
        String winner,
        double confidence,
        double pValue,
        boolean sufficientSamples,
        int samplesNeeded
) {}
