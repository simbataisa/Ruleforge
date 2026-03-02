package io.ruleforge.scheduler.workflow.repository;

import io.ruleforge.scheduler.workflow.entity.WorkflowRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRunEntity, UUID> {

    Page<WorkflowRunEntity> findByWorkflowId(UUID workflowId, Pageable pageable);
}
