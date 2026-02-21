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
import java.util.Map;

/**
 * API client for RuleForge AI assistant endpoints.
 */
public class AiApi {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    AiApi(String baseUrl, String apiKey, String jwtToken) {
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
     * Returns a natural-language explanation of a rule.
     *
     * @param ruleId the rule UUID
     * @return explanation map with keys such as {@code summary}, {@code conditions}, {@code actions}
     */
    public Map<String, Object> explainRule(String ruleId) {
        return post("%s/v1/ai/explain".formatted(baseUrl), Map.of("ruleId", ruleId));
    }

    /**
     * Generates a rule definition from a natural-language prompt.
     *
     * @param prompt   natural-language description of the desired rule
     * @param ruleType target rule type (e.g. "DECISION_TABLE", "DECISION_TREE")
     * @return generated rule definition map
     */
    public Map<String, Object> generateRule(String prompt, String ruleType) {
        return post("%s/v1/ai/generate-rule".formatted(baseUrl),
                Map.of("prompt", prompt, "ruleType", ruleType));
    }

    /**
     * Generates test data suitable for exercising a rule.
     *
     * @param ruleId the rule UUID
     * @param count  number of test records to generate
     * @return map containing a {@code testData} list of input records
     */
    public Map<String, Object> generateTestData(String ruleId, int count) {
        return post("%s/v1/ai/generate-test-data".formatted(baseUrl),
                Map.of("ruleId", ruleId, "count", count));
    }

    private Map<String, Object> post(String url, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuth(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call AI endpoint: " + url, e);
        }
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
