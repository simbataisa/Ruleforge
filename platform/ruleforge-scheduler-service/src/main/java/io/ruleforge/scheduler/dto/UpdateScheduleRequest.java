package io.ruleforge.scheduler.dto;

import java.util.Map;
import java.util.UUID;

public record UpdateScheduleRequest(
    String name,
    String cronExpression,
    Map<String, Object> inputTemplate,
    Boolean enabled,
    String description,
    UUID workflowId,
    UUID holidayCalendarId,
    String skipOnHoliday
) {}
