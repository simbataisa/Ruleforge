package io.ruleforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
        @NotBlank(message = "Space name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
