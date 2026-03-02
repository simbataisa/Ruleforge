package io.ruleforge.scheduler.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HolidayCalendarDto(
    UUID id,
    UUID spaceId,
    String name,
    String description,
    String market,
    String region,
    List<String> weekendDays,
    boolean enabled,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt,
    long entryCount
) {}
