package io.ruleforge.auth.dto;

import io.ruleforge.auth.entity.SsoProviderType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateSsoConfigRequest(
    @NotNull UUID orgId,
    @NotNull SsoProviderType providerType,
    String metadataUrl,
    String clientId,
    String clientSecret,
    Map<String, String> attributeMapping
) {}
