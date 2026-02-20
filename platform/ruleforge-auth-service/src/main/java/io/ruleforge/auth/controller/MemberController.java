package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.AddMemberRequest;
import io.ruleforge.auth.dto.MemberDto;
import io.ruleforge.auth.dto.UpdateRoleRequest;
import io.ruleforge.auth.service.MemberService;
import io.ruleforge.common.dto.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations/{orgId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberDto> addMember(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        MemberDto member = memberService.addMember(orgId, userId, request);
        return ApiResponse.ok(member);
    }

    @GetMapping
    public ApiResponse<List<MemberDto>> listMembers(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<MemberDto> members = memberService.listMembers(orgId, userId);
        return ApiResponse.ok(members);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeMember(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        memberService.removeMember(orgId, memberId, userId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{memberId}/role")
    public ApiResponse<MemberDto> updateRole(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        MemberDto member = memberService.updateRole(orgId, memberId, request.role(), userId);
        return ApiResponse.ok(member);
    }
}
