package io.ruleforge.core.spi;

import java.util.Map;

/**
 * SPI for custom data enrichment sources (JDBC, HTTP, Redis, etc.).
 * Enrichers fetch external data during rule evaluation.
 */
public interface DataEnricherExtension {

    /**
     * Protocol prefix this enricher handles (e.g., "jdbc:", "http:", "redis:").
     */
    String protocol();

    /**
     * Fetch external data and return as key-value pairs to merge into working memory.
     *
     * @param uri the enrichment URI (after protocol prefix)
     * @param params parameters for the enrichment query
     * @param ctx evaluation context
     * @return enrichment data as key-value pairs
     */
    Map<String, Object> enrich(String uri, Map<String, Object> params, EvaluationContext ctx);
}
