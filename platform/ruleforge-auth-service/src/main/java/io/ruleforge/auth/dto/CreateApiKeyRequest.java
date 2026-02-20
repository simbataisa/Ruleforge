package io.ruleforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateApiKeyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Key type is required")
        @Pattern(regexp = "^(SOLVER|MANAGEMENT)$", message = "Key type must be SOLVER or MANAGEMENT")
        String keyType,

        List<String> permissions,

        Instant expiresAt
) {}
