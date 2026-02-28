package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.management.dto.SpaceApprovalSettingsDto;
import io.ruleforge.management.dto.UpdateSpaceApprovalSettingsRequest;
import io.ruleforge.management.service.ApprovalService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/spaces/{spaceId}/approval-settings")
public class SpaceApprovalSettingsController {

    private final ApprovalService approvalService;

    public SpaceApprovalSettingsController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    public ApiResponse<SpaceApprovalSettingsDto> getApprovalSettings(@PathVariable UUID spaceId) {
        SpaceApprovalSettingsDto settings = approvalService.getSpaceApprovalSettings(spaceId);
        return ApiResponse.ok(settings);
    }

    @PutMapping
    @AuditLog(action = "UPDATE_SPACE_APPROVAL_SETTINGS", resourceType = "SpaceApprovalSettings")
    public ApiResponse<SpaceApprovalSettingsDto> updateApprovalSettings(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceApprovalSettingsRequest request) {
        SpaceApprovalSettingsDto settings = approvalService.updateSpaceApprovalSettings(spaceId, request);
        return ApiResponse.ok(settings, "Approval settings updated successfully");
    }
}
