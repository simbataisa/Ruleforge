package io.ruleforge.scheduler.workflow.dto;

import java.util.Map;

public record UpdateWorkflowRequest(
    String name,
    String description,
    Map<String, Object> definition,
    String cronExpression,
    Boolean enabled,
    String triggerType,
    Map<String, Object> triggerConfig
) {}
