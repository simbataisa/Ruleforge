package io.ruleforge.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateScheduleRequest(
    @NotNull UUID spaceId,
    UUID ruleId,
    @NotBlank String name,
    @NotBlank String cronExpression,
    Map<String, Object> inputTemplate,
    String description,
    String ruleName,
    String ruleType,
    @NotNull UUID createdBy,
    UUID workflowId,
    UUID holidayCalendarId,
    String skipOnHoliday
) {}
