package io.ruleforge.management.dto;

import java.util.List;

public record ImportResult(
    int totalRules,
    int importedCount,
    int skippedCount,
    int failedCount,
    List<String> errors
) {}
