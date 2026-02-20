package io.ruleforge.connector.controller;

import io.ruleforge.connector.dto.EnrichRequest;
import io.ruleforge.connector.dto.EnrichResponse;
import io.ruleforge.connector.entity.ConnectorEntity;
import io.ruleforge.connector.repository.ConnectorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class EnrichmentController {

    private final ConnectorRepository connectorRepository;

    public EnrichmentController(ConnectorRepository connectorRepository) {
        this.connectorRepository = connectorRepository;
    }

    @PostMapping("/enrich")
    public ResponseEntity<EnrichResponse> enrich(@RequestBody EnrichRequest request) {
        if (request.connectorId() == null) {
            return ResponseEntity.ok(new EnrichResponse(Map.of(), false, "connectorId is required"));
        }
        ConnectorEntity connector = connectorRepository.findById(request.connectorId()).orElse(null);
        if (connector == null) {
            return ResponseEntity.ok(new EnrichResponse(Map.of(), false,
                    "Connector not found: " + request.connectorId()));
        }
        // Basic enrichment: return connector config merged with parameters
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        if (connector.getConfig() != null) data.putAll(connector.getConfig());
        if (request.parameters() != null) data.putAll(request.parameters());
        return ResponseEntity.ok(new EnrichResponse(data, true, null));
    }
}
