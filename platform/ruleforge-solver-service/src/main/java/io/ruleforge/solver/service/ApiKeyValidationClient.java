package io.ruleforge.solver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.ruleforge.common.security.ApiKeyValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates API keys by calling the auth service's internal validation endpoint.
 * Valid keys are cached for 30 seconds to reduce load on the auth service.
 */
@Service
public class ApiKeyValidationClient implements ApiKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyValidationClient.class);

    private final RestClient authServiceClient;
    private final Cache<String, Optional<ApiKeyInfo>> keyCache;

    public ApiKeyValidationClient(RestClient authServiceClient) {
        this.authServiceClient = authServiceClient;
        this.keyCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Optional<ApiKeyInfo> validate(String apiKey) {
        return keyCache.get(apiKey, this::doValidate);
    }

    @SuppressWarnings("unchecked")
    private Optional<ApiKeyInfo> doValidate(String apiKey) {
        try {
            Map<String, Object> response = authServiceClient.post()
                    .uri("/internal/api-keys/validate")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !Boolean.TRUE.equals(response.get("valid"))) {
                return Optional.empty();
            }

            UUID orgId = UUID.fromString((String) response.get("orgId"));
            UUID spaceId = UUID.fromString((String) response.get("spaceId"));
            String keyType = (String) response.get("keyType");
            List<String> permissions = (List<String>) response.getOrDefault("permissions", List.of());

            return Optional.of(new ApiKeyInfo(orgId, spaceId, keyType, permissions));
        } catch (RestClientException e) {
            log.warn("Failed to validate API key against auth service: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during API key validation", e);
            return Optional.empty();
        }
    }
}
