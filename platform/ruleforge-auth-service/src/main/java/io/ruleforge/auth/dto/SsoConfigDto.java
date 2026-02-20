package io.ruleforge.auth.dto;

import io.ruleforge.auth.entity.SsoProviderType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SsoConfigDto(
    UUID id,
    UUID orgId,
    SsoProviderType providerType,
    String metadataUrl,
    String clientId,
    Map<String, String> attributeMapping,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
