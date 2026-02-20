package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.CreateOrganizationRequest;
import io.ruleforge.auth.dto.OrganizationDto;
import io.ruleforge.auth.service.OrganizationService;
import io.ruleforge.common.dto.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        OrganizationDto org = organizationService.createOrganization(userId, request);
        return ApiResponse.ok(org);
    }

    @GetMapping
    public ApiResponse<List<OrganizationDto>> listOrganizations(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<OrganizationDto> orgs = organizationService.getOrganizationsForUser(userId);
        return ApiResponse.ok(orgs);
    }

    @GetMapping("/{orgId}")
    public ApiResponse<OrganizationDto> getOrganization(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        OrganizationDto org = organizationService.getOrganization(orgId, userId);
        return ApiResponse.ok(org);
    }

    @PutMapping("/{orgId}")
    public ApiResponse<OrganizationDto> updateOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        OrganizationDto org = organizationService.updateOrganization(orgId, userId, request);
        return ApiResponse.ok(org);
    }
}
