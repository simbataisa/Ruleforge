package io.ruleforge.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TypeDistributionDto(
        @JsonProperty("type") String ruleType,
        long count,
        double percentage
) {}
