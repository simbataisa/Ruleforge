package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSampleInputRequest(
        @NotBlank String name,
        String description,
        @NotNull Object input
) {}
