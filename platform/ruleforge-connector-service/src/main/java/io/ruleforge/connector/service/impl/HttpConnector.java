package io.ruleforge.connector.service.impl;

import io.ruleforge.connector.dto.TestConnectionResponse;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpConnector {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public TestConnectionResponse test(Map<String, Object> config) {
        String baseUrl = (String) config.get("baseUrl");
        if (baseUrl == null) {
            return new TestConnectionResponse(false, "Missing 'baseUrl' in config");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status < 500) {
                return new TestConnectionResponse(true, "Connection successful (HTTP " + status + ")");
            }
            return new TestConnectionResponse(false, "Server error: HTTP " + status);
        } catch (Exception e) {
            return new TestConnectionResponse(false, "Connection failed: " + e.getMessage());
        }
    }
}
