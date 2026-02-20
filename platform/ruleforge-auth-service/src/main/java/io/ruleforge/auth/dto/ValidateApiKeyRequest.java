package io.ruleforge.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateApiKeyRequest(
        @NotBlank(message = "API key is required")
        String apiKey
) {}
