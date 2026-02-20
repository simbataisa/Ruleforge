package io.ruleforge.connector.dto;

import java.util.Map;

public record EnrichResponse(Map<String, Object> data, boolean success, String error) {}
