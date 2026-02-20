package io.ruleforge.connector.controller;

import io.ruleforge.connector.dto.*;
import io.ruleforge.connector.service.ConnectorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/connectors")
public class ConnectorController {

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @PostMapping
    public ResponseEntity<ConnectorDto> create(@Valid @RequestBody CreateConnectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(connectorService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ConnectorDto>> list(@RequestParam UUID spaceId) {
        return ResponseEntity.ok(connectorService.findBySpaceId(spaceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectorDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(connectorService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConnectorDto> update(@PathVariable UUID id,
                                                @RequestBody UpdateConnectorRequest request) {
        return ResponseEntity.ok(connectorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        connectorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<TestConnectionResponse> testConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(connectorService.testConnection(id));
    }
}
