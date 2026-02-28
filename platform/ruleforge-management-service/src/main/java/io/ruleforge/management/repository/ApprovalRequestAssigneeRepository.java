package io.ruleforge.management.repository;

import io.ruleforge.management.entity.ApprovalRequestAssigneeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRequestAssigneeRepository extends JpaRepository<ApprovalRequestAssigneeEntity, UUID> {

    List<ApprovalRequestAssigneeEntity> findByRequestId(UUID requestId);

    Optional<ApprovalRequestAssigneeEntity> findByRequestIdAndAssigneeId(UUID requestId, UUID assigneeId);

    List<ApprovalRequestAssigneeEntity> findByRequestIdAndStatus(UUID requestId, String status);
}
