package io.ruleforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryDto(
        UUID id,
        UUID webhookId,
        String eventType,
        int statusCode,
        boolean success,
        Instant deliveredAt
) {}
