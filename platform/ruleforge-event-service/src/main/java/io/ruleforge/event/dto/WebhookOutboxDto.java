package io.ruleforge.event.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookOutboxDto(
        UUID id,
        UUID webhookId,
        String eventType,
        String status,
        int attemptCount,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime lastAttemptAt,
        Integer lastStatusCode,
        String lastError,
        OffsetDateTime createdAt
) {}
