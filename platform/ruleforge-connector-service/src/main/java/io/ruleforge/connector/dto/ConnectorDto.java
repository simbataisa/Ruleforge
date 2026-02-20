package io.ruleforge.connector.dto;

import io.ruleforge.connector.entity.ConnectorType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConnectorDto(
    UUID id,
    UUID spaceId,
    String name,
    ConnectorType type,
    Map<String, Object> config,
    boolean enabled,
    String description,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
