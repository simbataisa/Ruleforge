package io.ruleforge.scheduler.workflow.dto;

import java.util.Map;
import java.util.UUID;

public record UpdateWorkflowRequest(
    String name,
    String description,
    Map<String, Object> definition,
    String cronExpression,
    Boolean enabled,
    String triggerType,
    Map<String, Object> triggerConfig,
    UUID holidayCalendarId,
    String skipOnHoliday
) {}
