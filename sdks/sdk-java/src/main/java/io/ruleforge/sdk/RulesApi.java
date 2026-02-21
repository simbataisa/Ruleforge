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
 * API client for RuleForge rule management endpoints.
 */
public class RulesApi {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    RulesApi(String baseUrl, String apiKey, String jwtToken) {
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
     * Lists all rules in a given space.
     *
     * @param spaceId the space UUID
     * @return paginated rule list as a generic map (contains "content", "totalElements", etc.)
     */
    public Map<String, Object> listRules(String spaceId) {
        String url = "%s/v1/spaces/%s/rules".formatted(baseUrl, spaceId);
        return get(url);
    }

    /**
     * Retrieves a single rule by its UUID.
     *
     * @param ruleId the rule UUID
     * @return rule details as a generic map
     */
    public Map<String, Object> getRule(String ruleId) {
        String url = "%s/v1/rules/%s".formatted(baseUrl, ruleId);
        return get(url);
    }

    private Map<String, Object> get(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET();

        applyAuth(builder);
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call rules endpoint: " + url, e);
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
