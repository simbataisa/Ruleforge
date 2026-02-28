package io.ruleforge.management.service;

import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.common.event.EventPublisher;
import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.ApprovalRequestDto;
import io.ruleforge.management.dto.AssigneeStatusDto;
import io.ruleforge.management.dto.RequestApprovalRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.dto.SpaceApprovalSettingsDto;
import io.ruleforge.management.dto.UpdateSpaceApprovalSettingsRequest;
import io.ruleforge.management.entity.ApprovalRequestAssigneeEntity;
import io.ruleforge.management.entity.ApprovalRequestEntity;
import io.ruleforge.management.entity.RuleEntity;
import io.ruleforge.management.entity.RuleVersionEntity;
import io.ruleforge.management.entity.SpaceApprovalSettingsEntity;
import io.ruleforge.management.repository.ApprovalRequestAssigneeRepository;
import io.ruleforge.management.repository.ApprovalRequestRepository;
import io.ruleforge.management.repository.RuleRepository;
import io.ruleforge.management.repository.RuleVersionRepository;
import io.ruleforge.management.repository.SpaceApprovalSettingsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private static final String TOPIC_RULE_LIFECYCLE = "rule.lifecycle";
    private static final Set<String> APPROVAL_ROLES = Set.of("OWNER", "APPROVER");
    private static final int DEFAULT_SLA_HOURS = 48;

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestAssigneeRepository assigneeRepository;
    private final SpaceApprovalSettingsRepository settingsRepository;
    private final RuleRepository ruleRepository;
    private final RuleVersionRepository versionRepository;
    private final EventPublisher eventPublisher;

    public ApprovalService(ApprovalRequestRepository approvalRequestRepository,
                           ApprovalRequestAssigneeRepository assigneeRepository,
                           SpaceApprovalSettingsRepository settingsRepository,
                           RuleRepository ruleRepository,
                           RuleVersionRepository versionRepository,
                           EventPublisher eventPublisher) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.assigneeRepository = assigneeRepository;
        this.settingsRepository = settingsRepository;
        this.ruleRepository = ruleRepository;
        this.versionRepository = versionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RuleVersionDto requestApproval(UUID ruleId, int version, RequestApprovalRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleEntity rule = findRuleOrThrow(ruleId);

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"DRAFT".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only DRAFT versions can be submitted for approval. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        // Resolve assignees
        List<UUID> assigneeIds = resolveAssigneeIds(request, rule.getSpaceId());
        if (assigneeIds.isEmpty()) {
            throw new ConflictException("No approvers specified and no default approvers configured for this space.");
        }

        // Cancel any existing PENDING approval request for this rule+version
        approvalRequestRepository.findByRuleIdAndVersionAndStatus(ruleId, version, "PENDING")
                .ifPresent(existing -> {
                    existing.setStatus("CANCELLED");
                    existing.setResolvedAt(Instant.now());
                    approvalRequestRepository.save(existing);
                });

        // Load SLA hours from space settings
        int slaHours = settingsRepository.findBySpaceId(rule.getSpaceId())
                .map(SpaceApprovalSettingsEntity::getSlaHours)
                .orElse(DEFAULT_SLA_HOURS);

        // Create approval request
        ApprovalRequestEntity approvalRequest = new ApprovalRequestEntity();
        approvalRequest.setRuleId(ruleId);
        approvalRequest.setVersion(version);
        approvalRequest.setRequestedBy(tenant.userId());
        approvalRequest.setNote(request != null ? request.note() : null);
        approvalRequest.setStatus("PENDING");
        approvalRequest.setDeadline(Instant.now().plus(slaHours, ChronoUnit.HOURS));
        approvalRequest = approvalRequestRepository.save(approvalRequest);

        // Create assignee records
        for (UUID assigneeId : assigneeIds) {
            ApprovalRequestAssigneeEntity assignee = new ApprovalRequestAssigneeEntity();
            assignee.setRequestId(approvalRequest.getId());
            assignee.setAssigneeId(assigneeId);
            assignee.setStatus("PENDING");
            assigneeRepository.save(assignee);
        }

        // Update version status
        versionEntity.setStatus("PENDING_APPROVAL");
        versionEntity.setRejectionReason(null);
        versionEntity = versionRepository.save(versionEntity);

        log.info("Approval requested: ruleId={}, version={}, requestId={}, assignees={}",
                ruleId, version, approvalRequest.getId(), assigneeIds);

        // Publish event
        publishEvent("rule.approval.requested", ruleId, tenant, Map.of(
                "version", version,
                "requestId", approvalRequest.getId().toString(),
                "assigneeIds", assigneeIds.stream().map(UUID::toString).toList().toString()
        ));

        return toVersionDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto approve(UUID ruleId, int version, UUID approverId) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        enforceApprovalRole(tenant);
        RuleEntity rule = findRuleOrThrow(ruleId);

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PENDING_APPROVAL".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL versions can be approved. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        // Prevent self-approval
        if (versionEntity.getCreatedBy().equals(tenant.userId())) {
            throw new AccessDeniedException("Cannot approve your own version");
        }

        // Load approval request
        ApprovalRequestEntity approvalRequest = approvalRequestRepository
                .findByRuleIdAndVersionAndStatus(ruleId, version, "PENDING")
                .orElse(null);

        if (approvalRequest != null) {
            // Validate caller is assigned or is OWNER
            boolean isAssigned = assigneeRepository.findByRequestIdAndAssigneeId(
                    approvalRequest.getId(), approverId).isPresent();
            boolean isOwner = "OWNER".equals(tenant.role());

            if (!isAssigned && !isOwner) {
                throw new AccessDeniedException("You are not assigned to review this approval request");
            }

            // Update assignee status
            assigneeRepository.findByRequestIdAndAssigneeId(approvalRequest.getId(), approverId)
                    .ifPresent(assignee -> {
                        assignee.setStatus("APPROVED");
                        assignee.setRespondedAt(Instant.now());
                        assigneeRepository.save(assignee);
                    });

            // Check quorum
            boolean requireAll = settingsRepository.findBySpaceId(rule.getSpaceId())
                    .map(SpaceApprovalSettingsEntity::isRequireAllApprovers)
                    .orElse(false);

            if (requireAll) {
                List<ApprovalRequestAssigneeEntity> pendingAssignees =
                        assigneeRepository.findByRequestIdAndStatus(approvalRequest.getId(), "PENDING");
                if (!pendingAssignees.isEmpty()) {
                    log.info("Approval recorded but waiting for all approvers: ruleId={}, version={}", ruleId, version);
                    return toVersionDto(versionEntity);
                }
            }

            // All conditions met — publish the version
            approvalRequest.setStatus("APPROVED");
            approvalRequest.setResolvedBy(approverId);
            approvalRequest.setResolvedAt(Instant.now());
            approvalRequestRepository.save(approvalRequest);
        }

        // Deprecate currently published versions
        List<RuleVersionEntity> publishedVersions = versionRepository.findByRuleIdAndStatus(ruleId, "PUBLISHED");
        for (RuleVersionEntity published : publishedVersions) {
            published.setStatus("DEPRECATED");
            versionRepository.save(published);
        }

        // Publish the version
        versionEntity.setStatus("PUBLISHED");
        versionEntity.setApprovedBy(approverId);
        versionEntity.setApprovedAt(Instant.now());
        versionEntity.setPublishedAt(Instant.now());
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version approved and published: ruleId={}, version={}, approvedBy={}", ruleId, version, approverId);

        publishEvent("rule.approval.resolved", ruleId, tenant, Map.of(
                "version", version,
                "resolution", "APPROVED",
                "approvedBy", approverId.toString()
        ));

        return toVersionDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto reject(UUID ruleId, int version, String reason) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        enforceApprovalRole(tenant);

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PENDING_APPROVAL".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL versions can be rejected. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        // Update approval request
        ApprovalRequestEntity approvalRequest = approvalRequestRepository
                .findByRuleIdAndVersionAndStatus(ruleId, version, "PENDING")
                .orElse(null);

        if (approvalRequest != null) {
            // Validate caller is assigned or is OWNER
            boolean isAssigned = assigneeRepository.findByRequestIdAndAssigneeId(
                    approvalRequest.getId(), tenant.userId()).isPresent();
            boolean isOwner = "OWNER".equals(tenant.role());

            if (!isAssigned && !isOwner) {
                throw new AccessDeniedException("You are not assigned to review this approval request");
            }

            // Update assignee status
            assigneeRepository.findByRequestIdAndAssigneeId(approvalRequest.getId(), tenant.userId())
                    .ifPresent(assignee -> {
                        assignee.setStatus("REJECTED");
                        assignee.setRespondedAt(Instant.now());
                        assigneeRepository.save(assignee);
                    });

            approvalRequest.setStatus("REJECTED");
            approvalRequest.setResolvedBy(tenant.userId());
            approvalRequest.setResolvedAt(Instant.now());
            approvalRequestRepository.save(approvalRequest);
        }

        // Revert version to DRAFT
        versionEntity.setStatus("DRAFT");
        versionEntity.setRejectionReason(reason);
        versionEntity = versionRepository.save(versionEntity);

        log.info("Version rejected: ruleId={}, version={}, reason={}", ruleId, version, reason);

        publishEvent("rule.approval.resolved", ruleId, tenant, Map.of(
                "version", version,
                "resolution", "REJECTED",
                "reason", reason != null ? reason : ""
        ));

        return toVersionDto(versionEntity);
    }

    @Transactional
    public RuleVersionDto cancelApproval(UUID ruleId, int version) {
        TenantContext.TenantInfo tenant = TenantContext.get();

        RuleVersionEntity versionEntity = versionRepository.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(() -> new EntityNotFoundException("RuleVersion",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        if (!"PENDING_APPROVAL".equals(versionEntity.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL versions can be cancelled. Current status: %s"
                    .formatted(versionEntity.getStatus()));
        }

        ApprovalRequestEntity approvalRequest = approvalRequestRepository
                .findByRuleIdAndVersionAndStatus(ruleId, version, "PENDING")
                .orElseThrow(() -> new EntityNotFoundException("ApprovalRequest",
                        "ruleId=%s, version=%d".formatted(ruleId, version)));

        // Only original requester or OWNER can cancel
        boolean isRequester = approvalRequest.getRequestedBy().equals(tenant.userId());
        boolean isOwner = "OWNER".equals(tenant.role());
        if (!isRequester && !isOwner) {
            throw new AccessDeniedException("Only the original requester or OWNER can cancel an approval request");
        }

        approvalRequest.setStatus("CANCELLED");
        approvalRequest.setResolvedAt(Instant.now());
        approvalRequestRepository.save(approvalRequest);

        versionEntity.setStatus("DRAFT");
        versionEntity = versionRepository.save(versionEntity);

        log.info("Approval cancelled: ruleId={}, version={}", ruleId, version);

        return toVersionDto(versionEntity);
    }

    @Transactional
    public io.ruleforge.management.dto.BatchApprovalResultDto batchRequestApproval(
            io.ruleforge.management.dto.BatchApprovalRequestDto request) {
        List<io.ruleforge.management.dto.BatchApprovalResultDto.ApprovalSuccessItem> succeeded = new java.util.ArrayList<>();
        List<io.ruleforge.management.dto.BatchApprovalResultDto.ApprovalFailureItem> failed = new java.util.ArrayList<>();

        for (UUID ruleId : request.ruleIds()) {
            try {
                RuleEntity rule = findRuleOrThrow(ruleId);
                int latestVersion = rule.getLatestVersion();

                RequestApprovalRequest approvalRequest = new RequestApprovalRequest(
                        request.assigneeIds(), request.note());
                requestApproval(ruleId, latestVersion, approvalRequest);

                // Find the created approval request
                ApprovalRequestEntity created = approvalRequestRepository
                        .findByRuleIdAndVersionAndStatus(ruleId, latestVersion, "PENDING")
                        .orElse(null);

                succeeded.add(new io.ruleforge.management.dto.BatchApprovalResultDto.ApprovalSuccessItem(
                        ruleId, latestVersion, created != null ? created.getId() : null));
            } catch (Exception e) {
                failed.add(new io.ruleforge.management.dto.BatchApprovalResultDto.ApprovalFailureItem(
                        ruleId, e.getMessage()));
            }
        }

        return new io.ruleforge.management.dto.BatchApprovalResultDto(succeeded, failed);
    }

    // Space approval settings

    @Transactional(readOnly = true)
    public SpaceApprovalSettingsDto getSpaceApprovalSettings(UUID spaceId) {
        SpaceApprovalSettingsEntity entity = settingsRepository.findBySpaceId(spaceId)
                .orElse(null);

        if (entity == null) {
            return new SpaceApprovalSettingsDto(spaceId, List.of(), DEFAULT_SLA_HOURS, false);
        }

        return toSettingsDto(entity);
    }

    @Transactional
    public SpaceApprovalSettingsDto updateSpaceApprovalSettings(UUID spaceId, UpdateSpaceApprovalSettingsRequest request) {
        SpaceApprovalSettingsEntity entity = settingsRepository.findBySpaceId(spaceId)
                .orElseGet(() -> {
                    SpaceApprovalSettingsEntity newEntity = new SpaceApprovalSettingsEntity();
                    newEntity.setSpaceId(spaceId);
                    return newEntity;
                });

        if (request.defaultApprovers() != null) {
            entity.setDefaultApprovers(request.defaultApprovers().toArray(new UUID[0]));
        }
        if (request.slaHours() != null) {
            entity.setSlaHours(request.slaHours());
        }
        if (request.requireAllApprovers() != null) {
            entity.setRequireAllApprovers(request.requireAllApprovers());
        }

        entity = settingsRepository.save(entity);
        log.info("Space approval settings updated: spaceId={}", spaceId);

        return toSettingsDto(entity);
    }

    // Approval request DTO conversion

    public ApprovalRequestDto toApprovalRequestDto(ApprovalRequestEntity entity) {
        RuleEntity rule = ruleRepository.findById(entity.getRuleId()).orElse(null);
        List<AssigneeStatusDto> assignees = assigneeRepository.findByRequestId(entity.getId()).stream()
                .map(a -> new AssigneeStatusDto(a.getAssigneeId(), a.getStatus(), a.getRespondedAt()))
                .toList();

        return new ApprovalRequestDto(
                entity.getId(),
                entity.getRuleId(),
                rule != null ? rule.getName() : null,
                rule != null ? rule.getType() : null,
                entity.getVersion(),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                entity.getNote(),
                entity.getStatus(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getDeadline(),
                assignees
        );
    }

    // Helpers

    private List<UUID> resolveAssigneeIds(RequestApprovalRequest request, UUID spaceId) {
        if (request != null && request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
            return request.assigneeIds();
        }

        // Fall back to space default approvers
        return settingsRepository.findBySpaceId(spaceId)
                .map(settings -> settings.getDefaultApprovers() != null
                        ? Arrays.asList(settings.getDefaultApprovers())
                        : List.<UUID>of())
                .orElse(List.of());
    }

    private void enforceApprovalRole(TenantContext.TenantInfo tenant) {
        if (tenant.role() == null || !APPROVAL_ROLES.contains(tenant.role())) {
            throw new AccessDeniedException("Only OWNER or APPROVER roles can approve or reject versions");
        }
    }

    private RuleEntity findRuleOrThrow(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule", ruleId.toString()));
    }

    private RuleVersionDto toVersionDto(RuleVersionEntity entity) {
        return new RuleVersionDto(
                entity.getId(),
                entity.getRuleId(),
                entity.getVersion(),
                entity.getDefinition(),
                entity.getInputModel(),
                entity.getOutputModel(),
                entity.getStatus(),
                entity.getChangeNote(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason()
        );
    }

    private SpaceApprovalSettingsDto toSettingsDto(SpaceApprovalSettingsEntity entity) {
        List<UUID> approverList = entity.getDefaultApprovers() != null
                ? Arrays.asList(entity.getDefaultApprovers())
                : List.of();
        return new SpaceApprovalSettingsDto(
                entity.getSpaceId(),
                approverList,
                entity.getSlaHours(),
                entity.isRequireAllApprovers()
        );
    }

    private void publishEvent(String eventType, UUID ruleId, TenantContext.TenantInfo tenant, Map<String, Object> data) {
        try {
            DomainEvent event = DomainEvent.create(
                    eventType,
                    "Rule",
                    ruleId.toString(),
                    tenant.orgId(),
                    tenant.userId(),
                    data
            );
            eventPublisher.publish(TOPIC_RULE_LIFECYCLE, event);
        } catch (Exception e) {
            log.warn("Failed to publish event {}: {}", eventType, e.getMessage());
        }
    }
}
