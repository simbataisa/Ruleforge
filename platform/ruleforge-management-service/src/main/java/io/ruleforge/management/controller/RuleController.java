package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CopyRuleRequest;
import io.ruleforge.management.dto.CreateRuleRequest;
import io.ruleforge.management.dto.FilterOptionsDto;
import io.ruleforge.management.dto.CreateSampleInputRequest;
import io.ruleforge.management.dto.RuleDto;
import io.ruleforge.management.dto.SampleInputDto;
import io.ruleforge.management.dto.TestResultDto;
import io.ruleforge.management.dto.TestRuleRequest;
import io.ruleforge.management.dto.UpdateRuleRequest;
import io.ruleforge.management.dto.UpdateSampleInputRequest;
import io.ruleforge.management.dto.ValidationResultDto;
import io.ruleforge.management.dto.AbTestDto;
import io.ruleforge.management.dto.EligibleApproverDto;
import io.ruleforge.management.entity.MemberEntity;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.entity.UserEntity;
import io.ruleforge.management.repository.MemberRepository;
import io.ruleforge.management.repository.SpaceRepository;
import io.ruleforge.management.repository.UserRepository;
import io.ruleforge.management.service.AbTestService;
import io.ruleforge.management.service.RuleService;
import io.ruleforge.management.service.RuleValidationService;
import io.ruleforge.management.service.SampleInputService;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
public class RuleController {

    private final RuleService ruleService;
    private final RuleValidationService ruleValidationService;
    private final AbTestService abTestService;
    private final SpaceRepository spaceRepository;
    private final SampleInputService sampleInputService;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public RuleController(RuleService ruleService, RuleValidationService ruleValidationService,
                          AbTestService abTestService, SpaceRepository spaceRepository,
                          SampleInputService sampleInputService, MemberRepository memberRepository,
                          UserRepository userRepository) {
        this.ruleService = ruleService;
        this.ruleValidationService = ruleValidationService;
        this.abTestService = abTestService;
        this.spaceRepository = spaceRepository;
        this.sampleInputService = sampleInputService;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
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
     * Supports enhanced search: multi-field text search, tag containment, date range, creator filter.
     */
    @GetMapping("/rules")
    public ApiResponse<PageResponse<RuleDto>> listRulesForOrg(
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) Instant updatedAfter,
            @RequestParam(required = false) Instant updatedBefore,
            @RequestParam(required = false) String hasVersion,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID orgId = TenantContext.get().orgId();
        PageResponse<RuleDto> rules = ruleService.listRulesForOrgAdvanced(
                orgId, folderId, status, type, types, statuses, search,
                tags, createdBy, updatedAfter, updatedBefore, hasVersion, pageable);
        return ApiResponse.ok(rules);
    }

    /**
     * Get filter options for the rules list (distinct statuses, types, spaces).
     */
    @GetMapping("/rules/filter-options")
    public ApiResponse<FilterOptionsDto> getFilterOptions() {
        UUID orgId = TenantContext.get().orgId();
        List<SpaceEntity> spaces = spaceRepository.findByOrgId(orgId);
        List<FilterOptionsDto.SpaceOption> spaceOptions = spaces.stream()
                .map(s -> new FilterOptionsDto.SpaceOption(s.getId().toString(), s.getName()))
                .toList();

        FilterOptionsDto options = new FilterOptionsDto(
                List.of("DRAFT", "PUBLISHED", "ARCHIVED", "PENDING_APPROVAL", "DEPRECATED"),
                List.of("TABLE", "TREE", "SCRIPT", "FLOW", "SCORECARD", "DMN", "CEP", "ENSEMBLE"),
                spaceOptions
        );
        return ApiResponse.ok(options);
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

    @PostMapping("/rules/{ruleId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "COPY_RULE", resourceType = "Rule")
    public ApiResponse<RuleDto> copyRule(@PathVariable UUID ruleId,
                                          @RequestBody(required = false) CopyRuleRequest request) {
        String nameOverride = request != null ? request.name() : null;
        RuleDto rule = ruleService.copyRule(ruleId, nameOverride);
        return ApiResponse.ok(rule, "Rule copied successfully");
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

    @GetMapping("/rules/{ruleId}/samples")
    public ApiResponse<List<SampleInputDto>> getSampleInputs(@PathVariable UUID ruleId) {
        List<SampleInputDto> samples = sampleInputService.listSampleInputs(ruleId);
        return ApiResponse.ok(samples);
    }

    @PostMapping("/rules/{ruleId}/samples")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_SAMPLE_INPUT", resourceType = "SampleInput")
    public ApiResponse<SampleInputDto> createSampleInput(@PathVariable UUID ruleId,
                                                          @Valid @RequestBody CreateSampleInputRequest request) {
        SampleInputDto sample = sampleInputService.createSampleInput(ruleId, request);
        return ApiResponse.ok(sample, "Sample input created successfully");
    }

    @PutMapping("/rules/{ruleId}/samples/{sampleId}")
    @AuditLog(action = "UPDATE_SAMPLE_INPUT", resourceType = "SampleInput")
    public ApiResponse<SampleInputDto> updateSampleInput(@PathVariable UUID ruleId,
                                                          @PathVariable UUID sampleId,
                                                          @Valid @RequestBody UpdateSampleInputRequest request) {
        SampleInputDto sample = sampleInputService.updateSampleInput(ruleId, sampleId, request);
        return ApiResponse.ok(sample, "Sample input updated successfully");
    }

    @DeleteMapping("/rules/{ruleId}/samples/{sampleId}")
    @AuditLog(action = "DELETE_SAMPLE_INPUT", resourceType = "SampleInput")
    public ApiResponse<Void> deleteSampleInput(@PathVariable UUID ruleId,
                                                @PathVariable UUID sampleId) {
        sampleInputService.deleteSampleInput(ruleId, sampleId);
        return ApiResponse.ok(null, "Sample input deleted successfully");
    }

    @GetMapping("/rules/{ruleId}/eligible-approvers")
    public ApiResponse<List<EligibleApproverDto>> getEligibleApprovers(@PathVariable UUID ruleId) {
        // Verify rule exists (triggers tenant ownership check)
        ruleService.getRule(ruleId);

        UUID orgId = TenantContext.get().orgId();
        List<MemberEntity> members = memberRepository.findByOrgIdAndRoleIn(
                orgId, List.of("OWNER", "ADMIN", "EDITOR"));

        // Batch-load user details
        List<UUID> userIds = members.stream().map(MemberEntity::getUserId).toList();
        Map<UUID, UserEntity> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<EligibleApproverDto> approvers = members.stream()
                .map(m -> {
                    UserEntity user = userMap.get(m.getUserId());
                    String name = user != null
                            ? (user.getFullName() != null ? user.getFullName() : user.getEmail())
                            : null;
                    String email = user != null ? user.getEmail() : null;
                    return new EligibleApproverDto(m.getId(), m.getUserId(), name, email, m.getRole());
                })
                .toList();

        return ApiResponse.ok(approvers);
    }
}
