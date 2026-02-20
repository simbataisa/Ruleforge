package io.ruleforge.auth.dto;

import java.util.UUID;

public record ApiKeyCreatedDto(
        UUID id,
        String apiKey,
        String keyPrefix,
        String name
) {}
