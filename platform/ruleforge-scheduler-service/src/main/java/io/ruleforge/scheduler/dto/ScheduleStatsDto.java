package io.ruleforge.scheduler.dto;

public record ScheduleStatsDto(
    long total,
    long active,
    long paused,
    long successLast24h,
    long failedLast24h
) {}
