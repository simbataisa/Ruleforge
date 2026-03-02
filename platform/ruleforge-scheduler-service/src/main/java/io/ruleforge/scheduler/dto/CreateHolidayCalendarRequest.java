package io.ruleforge.scheduler.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateHolidayCalendarRequest(
    @NotBlank String name,
    String description,
    String market,
    String region,
    List<String> weekendDays,
    UUID spaceId,
    UUID createdBy
) {}
