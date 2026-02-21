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
 * API client for RuleForge schedule management endpoints.
 */
public class SchedulesApi {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SchedulesApi(String baseUrl, String apiKey, String jwtToken) {
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
     * Lists all schedules for the current organization.
     *
     * @return list of schedule definitions
     */
    public List<Map<String, Object>> listSchedules() {
        String url = "%s/v1/schedules".formatted(baseUrl);
        HttpRequest request = buildGetRequest(url);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), LIST_MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to call schedules endpoint", e);
        }
    }

    /**
     * Creates a new schedule.
     *
     * @param schedule map describing the schedule (ruleId, cronExpression, input, enabled, etc.)
     * @return the created schedule
     */
    public Map<String, Object> createSchedule(Map<String, Object> schedule) {
        String url = "%s/v1/schedules".formatted(baseUrl);
        try {
            String body = objectMapper.writeValueAsString(schedule);
            HttpRequest request = buildPostRequest(url, body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to create schedule", e);
        }
    }

    /**
     * Deletes a schedule by its UUID.
     *
     * @param scheduleId the schedule UUID
     */
    public void deleteSchedule(String scheduleId) {
        String url = "%s/v1/schedules/%s".formatted(baseUrl, scheduleId);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .DELETE();
        applyAuth(builder);
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleForgeClientException("Failed to delete schedule: " + scheduleId, e);
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
