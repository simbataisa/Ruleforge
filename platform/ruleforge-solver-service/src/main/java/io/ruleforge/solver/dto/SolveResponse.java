package io.ruleforge.solver.dto;

import java.util.List;
import java.util.Map;

/**
 * Response payload returned after rule evaluation.
 *
 * @param output       the rule evaluation output
 * @param metadata     execution metadata (timing, counts, strategy)
 * @param matchedRules list of rules that matched during evaluation
 * @param trace        optional execution trace (included when tracing is requested)
 */
public record SolveResponse(
        Map<String, Object> output,
        SolveMetadata metadata,
        List<MatchedRuleDto> matchedRules,
        DecisionTraceDto trace
) {}
