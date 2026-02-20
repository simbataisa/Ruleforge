package io.ruleforge.connector.dto;

import java.util.Map;
import java.util.UUID;

public record EnrichRequest(UUID connectorId, Map<String, Object> parameters) {}
