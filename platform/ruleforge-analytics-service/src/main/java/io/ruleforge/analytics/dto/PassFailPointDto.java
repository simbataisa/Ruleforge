package io.ruleforge.analytics.dto;

import java.time.Instant;

public record PassFailPointDto(
        Instant timestamp,
        long passCount,
        long failCount
) {}
