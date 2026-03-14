# Story 002 — Space CRUD Backend (PUT + DELETE Endpoints)

**Epic:** Spaces Page Features
**Phase:** Backend
**Assignee:** Backend Engineer
**Priority:** P0
**Depends on:** Story 001

---

## User Story

As an org admin or owner,
I want REST endpoints to update and delete spaces,
So that I can manage the space lifecycle via the API.

---

## Acceptance Criteria

1. `PUT /v1/organizations/current/spaces/:id` — updates name and/or description; returns enriched `SpaceDto`
2. `DELETE /v1/organizations/current/spaces/:id` — deletes space; returns 204
3. `GET /v1/organizations/current/spaces/:id` — gets single space with enriched DTO (may already partially exist; ensure it returns all Story 001 fields)
4. PUT is restricted to ADMIN or OWNER — returns 403 otherwise
5. DELETE is restricted to OWNER only — returns 403 otherwise
6. DELETE blocked if space has rules — returns 409 `SPACE_NOT_EMPTY`
7. PUT with name that conflicts with another space's slug → 409 `SLUG_CONFLICT`
8. All operations validate that the space belongs to the caller's org

---

## Technical Implementation

### New method in `OrganizationService.java`
```java
public void ensureOwner(UUID orgId, UUID userId) {
    OrgMemberEntity member = getMemberOrThrow(orgId, userId);
    if (member.getRole() != OrgRole.OWNER) {
        throw new AccessDeniedException("Only the organization owner can perform this action");
    }
}
```

### New DTO: `UpdateSpaceRequest.java`
```java
public record UpdateSpaceRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2000) String description
) {}
```

### New methods in `SpaceService.java`
```java
@Transactional
public SpaceDto updateSpace(UUID orgId, UUID spaceId, UUID userId, UpdateSpaceRequest request) {
    organizationService.ensureAdminOrOwner(orgId, userId);

    SpaceEntity space = spaceRepository.findById(spaceId)
            .filter(s -> s.getOrgId().equals(orgId))
            .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

    space.setName(request.name());
    space.setSlug(SlugUtils.toSlug(request.name()));
    if (request.description() != null) {
        space.setDescription(request.description());
    }

    try {
        space = spaceRepository.save(space);
    } catch (DataIntegrityViolationException ex) {
        if (ex.getMessage().contains("spaces_slug_org_unique")) {
            throw new ConflictException("A space with this name already exists in your organization");
        }
        throw ex;
    }

    long ruleCount = spaceRepository.countRulesBySpaceId(spaceId);
    return toDto(space, ruleCount);
}

@Transactional
public void deleteSpace(UUID orgId, UUID spaceId, UUID userId) {
    organizationService.ensureOwner(orgId, userId);

    SpaceEntity space = spaceRepository.findById(spaceId)
            .filter(s -> s.getOrgId().equals(orgId))
            .orElseThrow(() -> new EntityNotFoundException("Space", spaceId.toString()));

    long ruleCount = spaceRepository.countRulesBySpaceId(spaceId);
    if (ruleCount > 0) {
        throw new ConflictException(
            String.format("This space contains %d rules and cannot be deleted. Remove all rules first.", ruleCount)
        );
    }

    spaceRepository.delete(space);
}
```

### Modified: `CurrentOrgController.java`
Add to the existing controller:
```java
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
```

### Error Response Format
`ConflictException` (already exists in `ruleforge-common`) should return:
```json
{"code": "CONFLICT", "message": "...descriptive message..."}
```
Verify `GlobalExceptionHandler` maps `ConflictException` → 409. If `ConflictException` doesn't exist yet, use the existing pattern from other exception classes.

---

## Definition of Done

- [ ] `GET /v1/organizations/current/spaces/:id` returns 200 with enriched SpaceDto
- [ ] `PUT /v1/organizations/current/spaces/:id` returns 200; space updated in DB
- [ ] `PUT` with VIEWER role returns 403
- [ ] `PUT` with name collision returns 409
- [ ] `DELETE /v1/organizations/current/spaces/:id` returns 204; space gone from DB
- [ ] `DELETE` with ADMIN role returns 403
- [ ] `DELETE` on space with rules returns 409 with ruleCount in message
- [ ] All endpoints verify space belongs to caller's org (no cross-tenant access)
- [ ] `@AuditLog` annotations on PUT and DELETE
- [ ] Smoke test Section 3.16 assertions for PUT and DELETE pass

---

## Dependencies

- Story 001 (enriched SpaceDto + slug)

**Estimated Complexity:** Medium (1.5 days)
