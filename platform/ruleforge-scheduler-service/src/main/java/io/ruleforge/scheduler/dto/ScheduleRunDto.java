package io.ruleforge.scheduler.dto;

import io.ruleforge.scheduler.entity.RunStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduleRunDto(
    UUID id,
    UUID scheduleId,
    RunStatus status,
    Instant startedAt,
    Instant completedAt,
    String resultLocation,
    String errorMessage,
    int recordsProcessed,
    Instant createdAt
) {}
