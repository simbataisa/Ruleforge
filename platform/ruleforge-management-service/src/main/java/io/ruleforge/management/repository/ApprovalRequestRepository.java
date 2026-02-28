package io.ruleforge.management.repository;

import io.ruleforge.management.entity.ApprovalRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {

    List<ApprovalRequestEntity> findByRuleIdAndVersion(UUID ruleId, int version);

    Optional<ApprovalRequestEntity> findByRuleIdAndVersionAndStatus(UUID ruleId, int version, String status);

    /**
     * Advanced filter query for approval requests dashboard.
     * Supports filtering by org (via space), status, assignee, requester, and date range.
     */
    @Query(value = """
        SELECT ar.* FROM approval_requests ar
        JOIN rules r ON r.id = ar.rule_id
        WHERE r.space_id IN (:spaceIds)
          AND (:status IS NULL OR ar.status = :status)
          AND (:ruleId IS NULL OR ar.rule_id = CAST(:ruleId AS uuid))
          AND (:requestedBy IS NULL OR ar.requested_by = CAST(:requestedBy AS uuid))
          AND (CAST(:fromDate AS timestamptz) IS NULL OR ar.requested_at >= CAST(:fromDate AS timestamptz))
          AND (CAST(:toDate AS timestamptz) IS NULL OR ar.requested_at <= CAST(:toDate AS timestamptz))
          AND (:assignedToMe = false OR EXISTS (
               SELECT 1 FROM approval_request_assignees ara
               WHERE ara.request_id = ar.id
                 AND ara.assignee_id = CAST(:currentUserId AS uuid)
          ))
        ORDER BY ar.requested_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM approval_requests ar
        JOIN rules r ON r.id = ar.rule_id
        WHERE r.space_id IN (:spaceIds)
          AND (:status IS NULL OR ar.status = :status)
          AND (:ruleId IS NULL OR ar.rule_id = CAST(:ruleId AS uuid))
          AND (:requestedBy IS NULL OR ar.requested_by = CAST(:requestedBy AS uuid))
          AND (CAST(:fromDate AS timestamptz) IS NULL OR ar.requested_at >= CAST(:fromDate AS timestamptz))
          AND (CAST(:toDate AS timestamptz) IS NULL OR ar.requested_at <= CAST(:toDate AS timestamptz))
          AND (:assignedToMe = false OR EXISTS (
               SELECT 1 FROM approval_request_assignees ara
               WHERE ara.request_id = ar.id
                 AND ara.assignee_id = CAST(:currentUserId AS uuid)
          ))
        """,
        nativeQuery = true)
    Page<ApprovalRequestEntity> findByAdvancedFilters(
        @Param("spaceIds") Collection<UUID> spaceIds,
        @Param("status") String status,
        @Param("ruleId") UUID ruleId,
        @Param("requestedBy") UUID requestedBy,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        @Param("assignedToMe") boolean assignedToMe,
        @Param("currentUserId") UUID currentUserId,
        Pageable pageable
    );
}
