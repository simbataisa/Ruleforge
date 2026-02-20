package io.ruleforge.management.dto;

import java.util.List;
import java.util.Map;

public record TestResultDto(
        Map<String, Object> output,
        long executionTimeMs,
        List<String> logs,
        boolean success,
        String error
) {}
