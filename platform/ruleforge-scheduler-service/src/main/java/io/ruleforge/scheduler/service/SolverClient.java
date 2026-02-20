package io.ruleforge.scheduler.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class SolverClient {

    private final RestClient restClient;

    public SolverClient(
            @Value("${ruleforge.solver.url:http://localhost:8083}") String solverUrl) {
        this.restClient = RestClient.builder().baseUrl(solverUrl).build();
    }

    public Map<String, Object> solve(UUID ruleId, Map<String, Object> input) {
        try {
            return restClient.post()
                    .uri("/v1/solve")
                    .body(Map.of("ruleId", ruleId.toString(), "input", input != null ? input : Map.of()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to call solver service: " + e.getMessage(), e);
        }
    }
}
