package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.ValidateApiKeyRequest;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.security.ApiKeyValidator;
import io.ruleforge.common.security.ApiKeyValidator.ApiKeyInfo;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final ApiKeyValidator apiKeyValidator;

    public InternalController(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @PostMapping("/api-keys/validate")
    public ApiResponse<ApiKeyInfo> validateApiKey(@Valid @RequestBody ValidateApiKeyRequest request) {
        Optional<ApiKeyInfo> result = apiKeyValidator.validate(request.apiKey());
        return result
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.error("Invalid API key"));
    }
}
