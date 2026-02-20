package io.ruleforge.scheduler.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ScheduleDto(
    UUID id,
    UUID spaceId,
    UUID ruleId,
    String name,
    String cronExpression,
    Map<String, Object> inputTemplate,
    boolean enabled,
    String description,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
