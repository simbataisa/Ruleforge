package io.ruleforge.solver.dto;

import java.util.List;
import java.util.Map;

/**
 * Serializable representation of an execution trace for debugging and auditing.
 *
 * @param ruleId      the rule that was traced
 * @param totalTimeMs total trace time in milliseconds
 * @param steps       ordered list of trace steps
 */
public record DecisionTraceDto(
        String ruleId,
        long totalTimeMs,
        List<Map<String, Object>> steps
) {}
