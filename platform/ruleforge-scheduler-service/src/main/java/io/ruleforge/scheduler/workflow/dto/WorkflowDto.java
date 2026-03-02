package io.ruleforge.scheduler.workflow.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowDto(
    UUID id,
    UUID spaceId,
    String name,
    String description,
    Map<String, Object> definition,
    boolean enabled,
    String cronExpression,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt,
    String status,
    String triggerType,
    Map<String, Object> triggerConfig
) {}
