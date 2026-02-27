package io.ruleforge.analytics.dto;

import java.time.Instant;

public record TimelinePointDto(
        Instant timestamp,
        long count,
        long errorCount
) {}
