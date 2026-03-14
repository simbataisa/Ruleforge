package io.ruleforge.scheduler.workflow.controller;

import io.ruleforge.scheduler.workflow.dto.*;
import io.ruleforge.scheduler.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowDto> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto>> list(@RequestParam(required = false) UUID spaceId) {
        if (spaceId == null) {
            return ResponseEntity.ok(workflowService.findAll());
        }
        return ResponseEntity.ok(workflowService.findBySpaceId(spaceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDto> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<WorkflowDto> enable(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.enable(id));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<WorkflowDto> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.disable(id));
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<WorkflowRunDto> triggerWorkflow(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.triggerManually(id));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<Page<WorkflowRunDto>> getRuns(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(workflowService.getRuns(id, pageable));
    }
}
