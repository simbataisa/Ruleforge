package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.ApprovalRequestDto;
import io.ruleforge.management.dto.BatchApprovalRequestDto;
import io.ruleforge.management.dto.BatchApprovalResultDto;
import io.ruleforge.management.entity.ApprovalRequestEntity;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.repository.ApprovalRequestRepository;
import io.ruleforge.management.repository.SpaceRepository;
import io.ruleforge.management.service.ApprovalService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ApprovalRequestController {

    private final ApprovalService approvalService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final SpaceRepository spaceRepository;

    public ApprovalRequestController(ApprovalService approvalService,
                                      ApprovalRequestRepository approvalRequestRepository,
                                      SpaceRepository spaceRepository) {
        this.approvalService = approvalService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.spaceRepository = spaceRepository;
    }

    /**
     * List approval requests with filters.
     * GET /v1/approval-requests?status=PENDING&assignedToMe=true
     */
    @GetMapping("/approval-requests")
    public ApiResponse<PageResponse<ApprovalRequestDto>> listApprovalRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID ruleId,
            @RequestParam(required = false) UUID requestedBy,
            @RequestParam(required = false, defaultValue = "false") boolean assignedToMe,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        TenantContext.TenantInfo tenant = TenantContext.get();
        UUID orgId = tenant.orgId();
        UUID currentUserId = tenant.userId();

        List<UUID> spaceIds = spaceRepository.findByOrgId(orgId).stream()
                .map(SpaceEntity::getId)
                .toList();

        if (spaceIds.isEmpty()) {
            return ApiResponse.ok(new PageResponse<>(List.of(), 0, pageable.getPageSize(), 0, 0));
        }

        Page<ApprovalRequestEntity> page = approvalRequestRepository.findByAdvancedFilters(
                spaceIds, status, ruleId, requestedBy, from, to,
                assignedToMe, currentUserId, pageable);

        List<ApprovalRequestDto> content = page.getContent().stream()
                .map(approvalService::toApprovalRequestDto)
                .toList();

        return ApiResponse.ok(new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()));
    }

    /**
     * Get a single approval request by ID.
     * GET /v1/approval-requests/{requestId}
     */
    @GetMapping("/approval-requests/{requestId}")
    public ApiResponse<ApprovalRequestDto> getApprovalRequest(@PathVariable UUID requestId) {
        ApprovalRequestEntity entity = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new io.ruleforge.common.exception.EntityNotFoundException(
                        "ApprovalRequest", requestId.toString()));
        return ApiResponse.ok(approvalService.toApprovalRequestDto(entity));
    }

    /**
     * Batch request approval for multiple rules.
     * POST /v1/rules/batch/request-approval
     */
    @PostMapping("/rules/batch/request-approval")
    @AuditLog(action = "BATCH_REQUEST_APPROVAL", resourceType = "ApprovalRequest")
    public ApiResponse<BatchApprovalResultDto> batchRequestApproval(
            @Valid @RequestBody BatchApprovalRequestDto request) {
        BatchApprovalResultDto result = approvalService.batchRequestApproval(request);
        return ApiResponse.ok(result, "Batch approval request processed");
    }

    /**
     * Cancel an approval request.
     * POST /v1/approval-requests/{requestId}/cancel
     */
    @PostMapping("/approval-requests/{requestId}/cancel")
    @AuditLog(action = "CANCEL_APPROVAL_REQUEST", resourceType = "ApprovalRequest")
    public ApiResponse<ApprovalRequestDto> cancelApprovalRequest(@PathVariable UUID requestId) {
        ApprovalRequestEntity entity = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new io.ruleforge.common.exception.EntityNotFoundException(
                        "ApprovalRequest", requestId.toString()));

        // Delegate to cancel by ruleId/version
        approvalService.cancelApproval(entity.getRuleId(), entity.getVersion());

        // Refresh and return
        entity = approvalRequestRepository.findById(requestId).orElse(entity);
        return ApiResponse.ok(approvalService.toApprovalRequestDto(entity), "Approval request cancelled");
    }
}
