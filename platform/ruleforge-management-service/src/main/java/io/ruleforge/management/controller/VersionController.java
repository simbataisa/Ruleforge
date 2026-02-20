package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.management.dto.CreateVersionRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.service.VersionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
