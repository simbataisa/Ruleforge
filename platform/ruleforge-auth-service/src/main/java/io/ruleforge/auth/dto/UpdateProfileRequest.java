package io.ruleforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 1, max = 255, message = "Full name must be between 1 and 255 characters")
        String fullName
) {}
