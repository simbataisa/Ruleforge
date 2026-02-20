package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.ApiKeyCreatedDto;
import io.ruleforge.auth.dto.ApiKeyDto;
import io.ruleforge.auth.dto.CreateApiKeyRequest;
import io.ruleforge.auth.service.ApiKeyService;
import io.ruleforge.auth.service.OrganizationService;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final OrganizationService organizationService;

    public ApiKeyController(ApiKeyService apiKeyService,
                            OrganizationService organizationService) {
        this.apiKeyService = apiKeyService;
        this.organizationService = organizationService;
    }

    @PostMapping("/v1/spaces/{spaceId}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyCreatedDto> createApiKey(
            @PathVariable UUID spaceId,
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TenantContext.TenantInfo tenant = TenantContext.get();
        UUID orgId = tenant.orgId();

        organizationService.ensureAdminOrOwner(orgId, userId);

        ApiKeyCreatedDto apiKey = apiKeyService.createApiKey(orgId, spaceId, request);
        return ApiResponse.ok(apiKey);
    }

    @GetMapping("/v1/spaces/{spaceId}/api-keys")
    public ApiResponse<List<ApiKeyDto>> listApiKeys(
            @PathVariable UUID spaceId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TenantContext.TenantInfo tenant = TenantContext.get();
        organizationService.ensureMember(tenant.orgId(), userId);

        List<ApiKeyDto> keys = apiKeyService.listApiKeys(spaceId);
        return ApiResponse.ok(keys);
    }

    @DeleteMapping("/v1/api-keys/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> revokeApiKey(
            @PathVariable UUID keyId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TenantContext.TenantInfo tenant = TenantContext.get();
        organizationService.ensureAdminOrOwner(tenant.orgId(), userId);

        apiKeyService.revokeApiKey(keyId);
        return ApiResponse.ok(null);
    }
}
