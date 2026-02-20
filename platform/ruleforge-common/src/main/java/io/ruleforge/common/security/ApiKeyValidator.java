package io.ruleforge.common.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for validating API keys. Each service that accepts API key
 * authentication must provide an implementation (typically the auth service).
 */
public interface ApiKeyValidator {

    /**
     * Validate the given API key and return key metadata if valid.
     *
     * @param apiKey the raw API key string
     * @return key info if valid, empty otherwise
     */
    Optional<ApiKeyInfo> validate(String apiKey);

    /**
     * Metadata associated with a validated API key.
     */
    record ApiKeyInfo(
            UUID orgId,
            UUID spaceId,
            String keyType,
            List<String> permissions
    ) {}
}
