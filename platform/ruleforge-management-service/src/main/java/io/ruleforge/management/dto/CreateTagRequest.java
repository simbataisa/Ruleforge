package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(
        @NotBlank(message = "Name is required")
        String name,

        String color
) {}
