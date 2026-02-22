package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateRuleRequest;
import io.ruleforge.management.dto.RuleDto;
import io.ruleforge.management.dto.TestResultDto;
import io.ruleforge.management.dto.TestRuleRequest;
import io.ruleforge.management.dto.UpdateRuleRequest;
import io.ruleforge.management.dto.ValidationResultDto;
import io.ruleforge.management.dto.AbTestDto;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.repository.SpaceRepository;
import io.ruleforge.management.service.AbTestService;
import io.ruleforge.management.service.RuleService;
import io.ruleforge.management.service.RuleValidationService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class RuleController {

    private final RuleService ruleService;
    private final RuleValidationService ruleValidationService;
    private final AbTestService abTestService;
    private final SpaceRepository spaceRepository;

    public RuleController(RuleService ruleService, RuleValidationService ruleValidationService,
                          AbTestService abTestService, SpaceRepository spaceRepository) {
        this.ruleService = ruleService;
        this.ruleValidationService = ruleValidationService;
        this.abTestService = abTestService;
        this.spaceRepository = spaceRepository;
    }

    @PostMapping("/spaces/{spaceId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> createRule(@PathVariable UUID spaceId,
                                           @Valid @RequestBody CreateRuleRequest request) {
        RuleDto rule = ruleService.createRule(spaceId, request);
        return ApiResponse.ok(rule, "Rule created successfully");
    }

    @GetMapping("/spaces/{spaceId}/rules")
    public ApiResponse<PageResponse<RuleDto>> listRules(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<RuleDto> rules = ruleService.listRules(spaceId, folderId, status, pageable);
        return ApiResponse.ok(rules);
    }

    /**
     * Create rule via convenience endpoint — derives spaceId from org's default space.
     */
    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> createRuleConvenience(@Valid @RequestBody CreateRuleRequest request) {
        UUID orgId = TenantContext.get().orgId();
        UUID spaceId = spaceRepository.findByOrgId(orgId).stream()
                .findFirst()
                .map(SpaceEntity::getId)
                .orElseThrow(() -> new io.ruleforge.common.exception.EntityNotFoundException(
                        "Space", "No space found for org " + orgId));
        RuleDto rule = ruleService.createRule(spaceId, request);
        return ApiResponse.ok(rule, "Rule created successfully");
    }

    /**
     * List rules across all spaces for the current org (convenience endpoint for frontend).
     */
    @GetMapping("/rules")
    public ApiResponse<PageResponse<RuleDto>> listRulesForOrg(
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID orgId = TenantContext.get().orgId();
        PageResponse<RuleDto> rules = ruleService.listRulesForOrg(orgId, folderId, status, type, search, pageable);
        return ApiResponse.ok(rules);
    }

    /**
     * List all A/B tests across all spaces for the current org.
     */
    @GetMapping("/rules/ab-tests")
    public ApiResponse<List<AbTestDto>> listAbTestsForOrg() {
        UUID orgId = TenantContext.get().orgId();
        List<AbTestDto> tests = abTestService.findAllForOrg(orgId);
        return ApiResponse.ok(tests);
    }

    @GetMapping("/rules/{ruleId}")
    public ApiResponse<RuleDto> getRule(@PathVariable UUID ruleId) {
        RuleDto rule = ruleService.getRule(ruleId);
        return ApiResponse.ok(rule);
    }

    @PutMapping("/rules/{ruleId}")
    @AuditLog(action = "UPDATE_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> updateRule(@PathVariable UUID ruleId,
                                           @Valid @RequestBody UpdateRuleRequest request) {
        RuleDto rule = ruleService.updateRule(ruleId, request);
        return ApiResponse.ok(rule, "Rule updated successfully");
    }

    @DeleteMapping("/rules/{ruleId}")
    @AuditLog(action = "DELETE_RULE", resourceType = "Rule")
    public ApiResponse<Void> deleteRule(@PathVariable UUID ruleId) {
        ruleService.deleteRule(ruleId);
        return ApiResponse.ok(null, "Rule deleted successfully");
    }

    @PostMapping("/rules/{ruleId}/lock")
    @AuditLog(action = "LOCK_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> lockRule(@PathVariable UUID ruleId) {
        UUID userId = TenantContext.get().userId();
        RuleDto rule = ruleService.lockRule(ruleId, userId);
        return ApiResponse.ok(rule, "Rule locked successfully");
    }

    @DeleteMapping("/rules/{ruleId}/lock")
    @AuditLog(action = "UNLOCK_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> unlockRule(@PathVariable UUID ruleId) {
        UUID userId = TenantContext.get().userId();
        RuleDto rule = ruleService.unlockRule(ruleId, userId);
        return ApiResponse.ok(rule, "Rule unlocked successfully");
    }

    @PostMapping("/rules/{ruleId}/validate")
    public ApiResponse<ValidationResultDto> validateRule(@PathVariable UUID ruleId) {
        RuleDto rule = ruleService.getRule(ruleId);
        ValidationResultDto result = ruleValidationService.validateRule(ruleId, rule.latestVersion());
        return ApiResponse.ok(result);
    }

    @PostMapping("/rules/{ruleId}/test")
    public ApiResponse<TestResultDto> testRule(@PathVariable UUID ruleId,
                                               @RequestParam(required = false) Integer version,
                                               @Valid @RequestBody TestRuleRequest request) {
        RuleDto rule = ruleService.getRule(ruleId);
        int testVersion = version != null ? version : rule.latestVersion();
        TestResultDto result = ruleValidationService.testRule(ruleId, testVersion, request);
        return ApiResponse.ok(result);
    }
}
