package io.ruleforge.auth.dto;

import java.util.Map;

public record UpdateSsoConfigRequest(
    String metadataUrl,
    String clientId,
    String clientSecret,
    Map<String, String> attributeMapping,
    Boolean enabled
) {}
