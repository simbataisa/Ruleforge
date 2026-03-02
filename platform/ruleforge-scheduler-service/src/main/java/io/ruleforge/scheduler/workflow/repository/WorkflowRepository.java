package io.ruleforge.scheduler.workflow.repository;

import io.ruleforge.scheduler.workflow.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, UUID> {

    List<WorkflowEntity> findBySpaceId(UUID spaceId);
}
