package io.ruleforge.connector.dto;

import java.util.Map;

public record UpdateConnectorRequest(
    String name,
    Map<String, Object> config,
    String credentials,
    Boolean enabled,
    String description
) {}
