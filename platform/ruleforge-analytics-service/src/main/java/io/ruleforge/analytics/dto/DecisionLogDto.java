package io.ruleforge.analytics.dto;

import java.time.Instant;

/**
 * API response DTO representing a single decision log entry.
 * Maps the key fields from {@link io.ruleforge.analytics.model.DecisionLogDocument}
 * to a lightweight, serializable response suitable for client consumption.
 */
public record DecisionLogDto(
        String id,
        String ruleId,
        String ruleName,
        String ruleType,
        String orgId,
        long executionTimeMs,
        String status,
        Instant timestamp,
        String correlationId
) {}
