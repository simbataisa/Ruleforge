package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.AddMemberRequest;
import io.ruleforge.auth.dto.CreateSpaceRequest;
import io.ruleforge.auth.dto.MemberDto;
import io.ruleforge.auth.dto.OrganizationDto;
import io.ruleforge.auth.dto.SpaceDto;
import io.ruleforge.auth.dto.UpdateRoleRequest;
import io.ruleforge.auth.dto.UpdateSpaceRequest;
import io.ruleforge.auth.service.MemberService;
import io.ruleforge.auth.service.OrganizationService;
import io.ruleforge.auth.service.SpaceService;
import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Convenience controller for /organizations/current endpoints.
 * Derives orgId from the JWT token's TenantContext instead of requiring it in the path.
 */
@RestController
@RequestMapping("/v1/organizations/current")
public class CurrentOrgController {

    private final OrganizationService organizationService;
    private final SpaceService spaceService;
    private final MemberService memberService;

    public CurrentOrgController(OrganizationService organizationService,
                                SpaceService spaceService,
                                MemberService memberService) {
        this.organizationService = organizationService;
        this.spaceService = spaceService;
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<OrganizationDto> getCurrentOrganization(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        OrganizationDto org = organizationService.getOrganization(orgId, userId);
        return ApiResponse.ok(org);
    }

    // ─── Spaces ──────────────────────────────────────────────────────────────

    @GetMapping("/spaces")
    public ApiResponse<List<SpaceDto>> listSpaces(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        List<SpaceDto> spaces = spaceService.listSpaces(orgId, userId);
        return ApiResponse.ok(spaces);
    }

    @PostMapping("/spaces")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "SPACE_CREATED", resourceType = "SPACE")
    public ApiResponse<SpaceDto> createSpace(
            @Valid @RequestBody CreateSpaceRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        SpaceDto space = spaceService.createSpace(orgId, userId, request);
        return ApiResponse.ok(space);
    }

    @GetMapping("/spaces/{spaceId}")
    public ApiResponse<SpaceDto> getSpace(
            @PathVariable UUID spaceId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        SpaceDto space = spaceService.getSpace(orgId, spaceId, userId);
        return ApiResponse.ok(space);
    }

    @PutMapping("/spaces/{spaceId}")
    @AuditLog(action = "SPACE_UPDATED", resourceType = "SPACE")
    public ApiResponse<SpaceDto> updateSpace(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        SpaceDto space = spaceService.updateSpace(orgId, spaceId, userId, request);
        return ApiResponse.ok(space);
    }

    @DeleteMapping("/spaces/{spaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuditLog(action = "SPACE_DELETED", resourceType = "SPACE")
    public void deleteSpace(
            @PathVariable UUID spaceId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        spaceService.deleteSpace(orgId, spaceId, userId);
    }

    // ─── Members ─────────────────────────────────────────────────────────────

    @GetMapping("/members")
    public ApiResponse<List<MemberDto>> listMembers(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        List<MemberDto> members = memberService.listMembers(orgId, userId);
        return ApiResponse.ok(members);
    }

    @PostMapping("/members/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberDto> inviteMember(
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        MemberDto member = memberService.addMember(orgId, userId, request);
        return ApiResponse.ok(member);
    }

    @PutMapping("/members/{memberId}/role")
    public ApiResponse<MemberDto> updateMemberRole(
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UUID orgId = TenantContext.get().orgId();
        MemberDto member = memberService.updateRole(orgId, memberId, request.role(), userId);
        return ApiResponse.ok(member);
    }
}
