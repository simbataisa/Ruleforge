package io.ruleforge.connector.dto;

import io.ruleforge.connector.entity.ConnectorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateConnectorRequest(
    @NotNull UUID spaceId,
    @NotBlank String name,
    @NotNull ConnectorType type,
    Map<String, Object> config,
    String credentials,
    String description,
    @NotNull UUID createdBy
) {}
