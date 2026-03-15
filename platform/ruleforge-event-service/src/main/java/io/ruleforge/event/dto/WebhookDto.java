package io.ruleforge.event.dto;

import io.ruleforge.event.model.WebhookScopeType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WebhookDto(
        UUID id,
        String name,
        WebhookScopeType scopeType,
        UUID scopeId,
        UUID orgId,
        String url,
        List<String> events,
        boolean active,
        String circuitStatus,
        int consecutiveFailures,
        OffsetDateTime lastDeliveredAt,
        String secretOnCreation,
        Instant createdAt
) {}
