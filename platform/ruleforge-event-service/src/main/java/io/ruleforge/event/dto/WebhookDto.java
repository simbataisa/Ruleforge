package io.ruleforge.event.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookDto(
        UUID id,
        UUID spaceId,
        String url,
        List<String> events,
        boolean active,
        Instant createdAt
) {}
