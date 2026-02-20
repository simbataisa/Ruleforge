package io.ruleforge.scheduler.dto;

import java.util.Map;

public record UpdateScheduleRequest(
    String name,
    String cronExpression,
    Map<String, Object> inputTemplate,
    Boolean enabled,
    String description
) {}
