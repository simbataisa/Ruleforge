package io.ruleforge.scheduler.workflow.service;

import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.scheduler.workflow.dto.*;
import io.ruleforge.scheduler.workflow.entity.WorkflowEntity;
import io.ruleforge.scheduler.workflow.entity.WorkflowRunEntity;
import io.ruleforge.scheduler.workflow.repository.WorkflowRepository;
import io.ruleforge.scheduler.workflow.repository.WorkflowRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowRunRepository workflowRunRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowRunRepository = workflowRunRepository;
    }

    public WorkflowDto create(CreateWorkflowRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        UUID spaceId = request.spaceId() != null ? request.spaceId()
                : tenant.spaceId() != null ? tenant.spaceId()
                : UUID.fromString("d0000000-0000-0000-0000-000000000001"); // default space
        WorkflowEntity entity = new WorkflowEntity();
        entity.setSpaceId(spaceId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setDefinition(request.definition() != null ? request.definition() : Map.of("nodes", List.of(), "edges", List.of()));
        entity.setCronExpression(request.cronExpression());
        entity.setCreatedBy(request.createdBy() != null ? request.createdBy() : tenant.userId());
        if (request.triggerType() != null) {
            entity.setTriggerType(request.triggerType());
        }
        if (request.triggerConfig() != null) {
            entity.setTriggerConfig(request.triggerConfig());
        }
        return toDto(workflowRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public WorkflowDto findById(UUID id) {
        return workflowRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<WorkflowDto> findAll() {
        return workflowRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowDto> findBySpaceId(UUID spaceId) {
        return workflowRepository.findBySpaceId(spaceId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public WorkflowDto update(UUID id, UpdateWorkflowRequest request) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.definition() != null) entity.setDefinition(request.definition());
        if (request.cronExpression() != null) entity.setCronExpression(request.cronExpression());
        if (request.enabled() != null) entity.setEnabled(request.enabled());
        if (request.triggerType() != null) entity.setTriggerType(request.triggerType());
        if (request.triggerConfig() != null) entity.setTriggerConfig(request.triggerConfig());
        return toDto(workflowRepository.save(entity));
    }

    public void delete(UUID id) {
        workflowRepository.deleteById(id);
    }

    public WorkflowDto enable(UUID id) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        entity.setEnabled(true);
        return toDto(workflowRepository.save(entity));
    }

    public WorkflowDto disable(UUID id) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        entity.setEnabled(false);
        return toDto(workflowRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowRunDto> getRuns(UUID workflowId, Pageable pageable) {
        return workflowRunRepository.findByWorkflowId(workflowId, pageable).map(this::toRunDto);
    }

    private WorkflowDto toDto(WorkflowEntity e) {
        String status = e.isEnabled() ? "ACTIVE" : "DRAFT";
        return new WorkflowDto(e.getId(), e.getSpaceId(), e.getName(), e.getDescription(),
                e.getDefinition(), e.isEnabled(), e.getCronExpression(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt(), status,
                e.getTriggerType(), e.getTriggerConfig());
    }

    private WorkflowRunDto toRunDto(WorkflowRunEntity e) {
        return new WorkflowRunDto(e.getId(), e.getWorkflowId(), e.getStatus(),
                e.getStartedAt(), e.getCompletedAt(), e.getStepResults(),
                e.getError(), e.getCreatedAt());
    }
}
