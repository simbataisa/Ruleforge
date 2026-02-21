package io.ruleforge.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateAbTestRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Version A is required")
        Integer versionA,

        @NotNull(message = "Version B is required")
        Integer versionB,

        @Min(value = 0, message = "Traffic split must be between 0 and 100")
        @Max(value = 100, message = "Traffic split must be between 0 and 100")
        int trafficSplit,

        Instant startDate,

        Instant endDate
) {}
