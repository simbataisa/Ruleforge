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

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class RuleController {

    private final RuleService ruleService;
    private final RuleValidationService ruleValidationService;

    public RuleController(RuleService ruleService, RuleValidationService ruleValidationService) {
        this.ruleService = ruleService;
        this.ruleValidationService = ruleValidationService;
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
                                               @Valid @RequestBody TestRuleRequest request) {
        RuleDto rule = ruleService.getRule(ruleId);
        TestResultDto result = ruleValidationService.testRule(ruleId, rule.latestVersion(), request);
        return ApiResponse.ok(result);
    }
}
