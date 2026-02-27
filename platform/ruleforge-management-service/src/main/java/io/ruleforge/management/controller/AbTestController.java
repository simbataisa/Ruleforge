package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.AbTestDto;
import io.ruleforge.management.dto.CreateAbTestRequest;
import io.ruleforge.management.dto.RuleVersionDto;
import io.ruleforge.management.service.AbTestService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/rules/{ruleId}/ab-tests")
public class AbTestController {

    private final AbTestService abTestService;

    public AbTestController(AbTestService abTestService) {
        this.abTestService = abTestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AbTestDto> createAbTest(@PathVariable UUID ruleId,
                                               @RequestParam UUID spaceId,
                                               @Valid @RequestBody CreateAbTestRequest request) {
        UUID userId = TenantContext.get().userId();
        AbTestDto dto = abTestService.create(ruleId, spaceId, userId, request);
        return ApiResponse.ok(dto, "A/B test created successfully");
    }

    @GetMapping
    public ApiResponse<List<AbTestDto>> listAbTests(@PathVariable UUID ruleId) {
        List<AbTestDto> tests = abTestService.findByRuleId(ruleId);
        return ApiResponse.ok(tests);
    }

    @PostMapping("/{id}/start")
    public ApiResponse<AbTestDto> startAbTest(@PathVariable UUID ruleId,
                                              @PathVariable UUID id) {
        AbTestDto dto = abTestService.start(id);
        return ApiResponse.ok(dto, "A/B test started successfully");
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<AbTestDto> stopAbTest(@PathVariable UUID ruleId,
                                             @PathVariable UUID id) {
        AbTestDto dto = abTestService.stop(id);
        return ApiResponse.ok(dto, "A/B test stopped successfully");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAbTest(@PathVariable UUID ruleId,
                             @PathVariable UUID id) {
        abTestService.delete(id);
    }

    @PostMapping("/{id}/promote")
    @AuditLog(action = "PROMOTE_AB_TEST", resourceType = "AbTest")
    public ApiResponse<RuleVersionDto> promoteAbTest(@PathVariable UUID ruleId,
                                                      @PathVariable UUID id,
                                                      @RequestParam String winner) {
        RuleVersionDto promoted = abTestService.promote(ruleId, id, winner);
        return ApiResponse.ok(promoted, "A/B test winner promoted successfully");
    }
}
