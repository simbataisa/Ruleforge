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
    String ruleName,
    String ruleType,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt,
    String status,
    Instant nextRunAt,
    Instant lastRunAt,
    UUID workflowId,
    String workflowName,
    UUID holidayCalendarId,
    String skipOnHoliday,
    String holidayCalendarName
) {}
