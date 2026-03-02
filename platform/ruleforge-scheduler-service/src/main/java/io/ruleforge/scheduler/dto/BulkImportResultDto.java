package io.ruleforge.scheduler.dto;

import java.util.List;

public record BulkImportResultDto(int imported, int skipped, List<String> errors) {}
