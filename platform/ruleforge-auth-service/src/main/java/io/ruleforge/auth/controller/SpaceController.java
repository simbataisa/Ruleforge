package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.CreateSpaceRequest;
import io.ruleforge.auth.dto.SpaceDto;
import io.ruleforge.auth.service.SpaceService;
import io.ruleforge.common.dto.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations/{orgId}/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SpaceDto> createSpace(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateSpaceRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        SpaceDto space = spaceService.createSpace(orgId, userId, request);
        return ApiResponse.ok(space);
    }

    @GetMapping
    public ApiResponse<List<SpaceDto>> listSpaces(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<SpaceDto> spaces = spaceService.listSpaces(orgId, userId);
        return ApiResponse.ok(spaces);
    }

    @GetMapping("/{spaceId}")
    public ApiResponse<SpaceDto> getSpace(
            @PathVariable UUID orgId,
            @PathVariable UUID spaceId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        SpaceDto space = spaceService.getSpace(orgId, spaceId, userId);
        return ApiResponse.ok(space);
    }
}
