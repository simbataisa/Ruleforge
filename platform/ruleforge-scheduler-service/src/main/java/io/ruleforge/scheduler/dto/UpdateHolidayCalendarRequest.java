package io.ruleforge.scheduler.dto;

import java.util.List;

public record UpdateHolidayCalendarRequest(
    String name,
    String description,
    String market,
    String region,
    List<String> weekendDays,
    Boolean enabled
) {}
