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
 * API client for RuleForge solver endpoints.
 *
 * <p>Provides single-record and bulk rule execution against the solver service.
 */
public class SolverApi {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SolverApi(String baseUrl, String apiKey, String jwtToken) {
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
     * Executes a single rule and returns the output.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param input         the input facts map
     * @return the solver response as a generic map
     */
    public Map<String, Object> solve(String ruleIdOrAlias, Map<String, Object> input) {
        String url = "%s/v1/solve/%s".formatted(baseUrl, ruleIdOrAlias);
        try {
            String body = objectMapper.writeValueAsString(Map.of("input", input));
            HttpRequest request = buildPostRequest(url, body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call solve endpoint", e);
        }
    }

    /**
     * Executes a rule against multiple input records in a single request.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param inputs        the list of input fact maps
     * @return list of solver responses
     */
    public List<Map<String, Object>> solveBulk(String ruleIdOrAlias, List<Map<String, Object>> inputs) {
        String url = "%s/v1/solve/bulk/%s".formatted(baseUrl, ruleIdOrAlias);
        try {
            String body = objectMapper.writeValueAsString(Map.of("inputs", inputs));
            HttpRequest request = buildPostRequest(url, body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), LIST_MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call solve/bulk endpoint", e);
        }
    }

    private HttpRequest buildPostRequest(String url, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

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
