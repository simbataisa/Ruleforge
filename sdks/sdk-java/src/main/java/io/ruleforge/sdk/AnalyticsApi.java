package io.ruleforge.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * API client for RuleForge analytics endpoints.
 */
public class AnalyticsApi {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    AnalyticsApi(String baseUrl, String apiKey, String jwtToken) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.jwtToken = jwtToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Retrieves aggregated metrics for the current organization.
     *
     * @return metrics map containing execution counts, latency percentiles, etc.
     */
    public Map<String, Object> getMetrics() {
        return get("%s/v1/analytics/metrics".formatted(baseUrl));
    }

    /**
     * Retrieves execution statistics for a specific rule.
     *
     * @param ruleId the rule UUID
     * @return statistics map with hit count, avg latency, error rate, etc.
     */
    public Map<String, Object> getRuleStats(String ruleId) {
        return get("%s/v1/analytics/rules/%s/stats".formatted(baseUrl, ruleId));
    }

    /**
     * Retrieves decision log entries, optionally filtered by rule.
     *
     * @param ruleId optional rule UUID filter (pass null for all logs)
     * @param page   zero-based page index
     * @param size   page size
     * @return paginated decision log entries
     */
    public List<Map<String, Object>> getDecisionLogs(String ruleId, int page, int size) {
        String url;
        if (ruleId != null && !ruleId.isBlank()) {
            url = "%s/v1/analytics/decision-logs?ruleId=%s&page=%d&size=%d"
                    .formatted(baseUrl, ruleId, page, size);
        } else {
            url = "%s/v1/analytics/decision-logs?page=%d&size=%d"
                    .formatted(baseUrl, page, size);
        }
        return getList(url);
    }

    private Map<String, Object> get(String url) {
        HttpRequest request = buildGetRequest(url);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call analytics endpoint: " + url, e);
        }
    }

    private List<Map<String, Object>> getList(String url) {
        HttpRequest request = buildGetRequest(url);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), LIST_MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call analytics endpoint: " + url, e);
        }
    }

    private HttpRequest buildGetRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET();
        applyAuth(builder);
        return builder.build();
    }

    private void applyAuth(HttpRequest.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("X-API-Key", apiKey);
        } else if (jwtToken != null && !jwtToken.isBlank()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
    }

    private void checkStatus(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuleForgeClientException(
                    "Unexpected HTTP status %d: %s".formatted(response.statusCode(), response.body()));
        }
    }
}
