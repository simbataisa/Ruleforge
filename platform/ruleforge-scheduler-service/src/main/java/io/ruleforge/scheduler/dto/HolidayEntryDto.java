package io.ruleforge.scheduler.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayEntryDto(
    UUID id,
    UUID calendarId,
    LocalDate date,
    String name,
    String entryType,
    boolean recurring,
    Instant createdAt
) {}
