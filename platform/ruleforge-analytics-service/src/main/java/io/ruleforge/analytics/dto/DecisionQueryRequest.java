package io.ruleforge.analytics.dto;

import java.time.Instant;

/**
 * Query parameters for filtering decision logs from Elasticsearch.
 */
public record DecisionQueryRequest(
        String orgId,
        String ruleId,
        Instant from,
        Instant to,
        int page,
        int size
) {}
