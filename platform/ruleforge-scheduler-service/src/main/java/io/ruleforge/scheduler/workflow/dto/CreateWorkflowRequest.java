package io.ruleforge.scheduler.workflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record CreateWorkflowRequest(
    UUID spaceId,
    @NotBlank String name,
    String description,
    Map<String, Object> definition,
    String cronExpression,
    UUID createdBy,
    String triggerType,
    Map<String, Object> triggerConfig
) {}
