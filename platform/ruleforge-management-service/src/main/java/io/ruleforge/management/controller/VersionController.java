package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateVersionRequest;
import io.ruleforge.management.dto.RejectVersionRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.dto.UpdateVersionRequest;
import io.ruleforge.management.service.VersionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/rules/{ruleId}/versions")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> createVersion(@PathVariable UUID ruleId,
                                                     @Valid @RequestBody CreateVersionRequest request) {
        RuleVersionDto version = versionService.createVersion(ruleId, request);
        return ApiResponse.ok(version, "Version created successfully");
    }

    @PutMapping("/{version}")
    @AuditLog(action = "UPDATE_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> updateVersion(@PathVariable UUID ruleId,
                                                      @PathVariable int version,
                                                      @Valid @RequestBody UpdateVersionRequest request) {
        RuleVersionDto versionDto = versionService.updateVersion(ruleId, version, request);
        return ApiResponse.ok(versionDto, "Version updated successfully");
    }

    @GetMapping
    public ApiResponse<List<RuleVersionDto>> listVersions(@PathVariable UUID ruleId) {
        List<RuleVersionDto> versions = versionService.listVersions(ruleId);
        return ApiResponse.ok(versions);
    }

    @GetMapping("/{version}")
    public ApiResponse<RuleVersionDto> getVersion(@PathVariable UUID ruleId,
                                                  @PathVariable int version) {
        RuleVersionDto versionDto = versionService.getVersion(ruleId, version);
        return ApiResponse.ok(versionDto);
    }

    @PostMapping("/{version}/publish")
    @AuditLog(action = "PUBLISH_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> publishVersion(@PathVariable UUID ruleId,
                                                      @PathVariable int version) {
        RuleVersionDto versionDto = versionService.publishVersion(ruleId, version);
        return ApiResponse.ok(versionDto, "Version published successfully");
    }

    @PostMapping("/{version}/deprecate")
    @AuditLog(action = "DEPRECATE_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> deprecateVersion(@PathVariable UUID ruleId,
                                                        @PathVariable int version) {
        RuleVersionDto versionDto = versionService.deprecateVersion(ruleId, version);
        return ApiResponse.ok(versionDto, "Version deprecated successfully");
    }

    @PostMapping("/{version}/request-approval")
    @AuditLog(action = "REQUEST_APPROVAL", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> requestApproval(@PathVariable UUID ruleId,
                                                        @PathVariable int version) {
        RuleVersionDto versionDto = versionService.requestApproval(ruleId, version);
        return ApiResponse.ok(versionDto, "Approval requested successfully");
    }

    @PostMapping("/{version}/approve")
    @AuditLog(action = "APPROVE_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> approveVersion(@PathVariable UUID ruleId,
                                                       @PathVariable int version) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        RuleVersionDto versionDto = versionService.approve(ruleId, version, tenant.userId());
        return ApiResponse.ok(versionDto, "Version approved and published successfully");
    }

    @PostMapping("/{version}/reject")
    @AuditLog(action = "REJECT_VERSION", resourceType = "RuleVersion")
    public ApiResponse<RuleVersionDto> rejectVersion(@PathVariable UUID ruleId,
                                                      @PathVariable int version,
                                                      @RequestBody RejectVersionRequest request) {
        RuleVersionDto versionDto = versionService.reject(ruleId, version, request.reason());
        return ApiResponse.ok(versionDto, "Version rejected");
    }
}
