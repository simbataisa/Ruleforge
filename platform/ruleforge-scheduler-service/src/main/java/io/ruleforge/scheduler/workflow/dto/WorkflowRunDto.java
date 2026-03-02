package io.ruleforge.scheduler.workflow.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowRunDto(
    UUID id,
    UUID workflowId,
    String status,
    Instant startedAt,
    Instant completedAt,
    List<Map<String, Object>> stepResults,
    String error,
    Instant createdAt
) {}
