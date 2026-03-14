# Enterprise Architecture — Spaces Page Features

**Version:** 1.0
**Date:** 2026-03-14
**Author:** BMAD Enterprise Architect Agent (Phase 3b)
**Input:** `docs/architecture/solution-architecture.md`, `docs/prd.md`

---

## Executive Summary

The Spaces Page Features epic is an **evolutionary enhancement** to the existing RuleForge platform. No new cloud infrastructure is required. All changes run within the existing local development stack and production deployment. This document covers the RBAC/security governance, compliance posture, cascading-delete safety, and observability requirements for the new capabilities.

---

## RBAC Governance

### Role Matrix for Space Operations

| Operation | VIEWER | EDITOR | ADMIN | OWNER |
|---|---|---|---|---|
| List spaces | ✅ | ✅ | ✅ | ✅ |
| View space detail | ✅ | ✅ | ✅ | ✅ |
| Create space | ❌ | ❌ | ✅ | ✅ |
| Edit space (name/description) | ❌ | ❌ | ✅ | ✅ |
| Delete space | ❌ | ❌ | ❌ | ✅ |
| View approval settings | ❌ | ❌ | ✅ | ✅ |
| Update approval settings | ❌ | ❌ | ✅ | ✅ |

**Rationale:**
- DELETE is OWNER-only because deleting a space is irreversible and potentially destroys all rules within it. Only the organizational owner should have this authority.
- ADMIN can configure spaces (create, edit, approval settings) as part of their operational responsibilities.
- VIEWER and EDITOR operate within spaces, not upon them — they lack the administrative authority to change the workspace structure.

### Authorization Implementation

The existing `OrganizationService` already provides:
- `ensureMember(orgId, userId)` — verifies org membership
- `ensureAdminOrOwner(orgId, userId)` — verifies ADMIN or OWNER role

Required addition:
```java
// New method in OrganizationService
public void ensureOwner(UUID orgId, UUID userId) {
    OrgMemberEntity member = getMemberOrThrow(orgId, userId);
    if (member.getRole() != OrgRole.OWNER) {
        throw new AccessDeniedException("Only the organization owner can perform this action");
    }
}
```

This maps to Spring Security's `AccessDeniedException`, which the `GlobalExceptionHandler` maps to 403 Forbidden.

---

## Cascading Delete Safety

### Risk

Deleting a space that contains rules, folders, or other associated data without safeguards results in silent data loss and broken references across the platform.

### Policy

**Block deletes of non-empty spaces.** A space may only be deleted if it has zero rules associated with it.

```
DELETE /organizations/current/spaces/:id
  → check: SELECT COUNT(*) FROM rules WHERE space_id = :id
  → if count > 0: return 409 Conflict
       {"code": "SPACE_NOT_EMPTY", "message": "This space contains N rules..."}
  → if count = 0: proceed with DELETE
```

### Error Response Contract

```json
{
  "code": "SPACE_NOT_EMPTY",
  "message": "This space contains 42 rules and cannot be deleted. Remove all rules first, or contact your administrator.",
  "details": {
    "ruleCount": 42,
    "spaceId": "3fa85f64-..."
  }
}
```

Frontend `ConfirmDialog` will show the rule count in the warning message. The delete button in the UI should be hidden/disabled (not just error on submit) when `ruleCount > 0`.

---

## Slug Uniqueness Collision Handling

### Risk

Two admins attempt to create spaces with the same name concurrently, or a rename creates a slug that already exists.

### Policy

The `UNIQUE(org_id, slug)` database constraint is the authoritative guard. The service layer catches `DataIntegrityViolationException` and maps it to a 409:

```java
try {
    SpaceEntity saved = spaceRepository.save(space);
    return toDto(saved, getRuleCount(saved.getId()));
} catch (DataIntegrityViolationException ex) {
    if (ex.getMessage().contains("spaces_slug_org_unique")) {
        throw new ConflictException("A space with this name already exists in your organization");
    }
    throw ex;
}
```

### Frontend Handling

The create and edit modals display the 409 error inline (not a global toast) so users understand the conflict is name-specific:

```
"A space named 'analytics' already exists. Try a different name."
```

---

## Audit Logging

All space mutation operations emit audit events via the existing `@AuditLog` annotation pattern:

| Operation | Event Type | Resource Type |
|---|---|---|
| Create space | `SPACE_CREATED` | `SPACE` |
| Update space | `SPACE_UPDATED` | `SPACE` |
| Delete space | `SPACE_DELETED` | `SPACE` |
| Update approval settings | `SPACE_APPROVAL_SETTINGS_UPDATED` | `SpaceApprovalSettings` |

These flow through Kafka → event-service → `audit_events` table, consistent with all other audit events in the platform. No additional instrumentation is needed beyond adding `@AuditLog` to the new controller methods.

---

## Compliance Posture

### Multi-Tenancy Isolation

All space operations are scoped by `orgId` via `TenantContext`. The query pattern:

```java
spaceRepository.findById(spaceId)
    .filter(s -> s.getOrgId().equals(orgId))  // Tenant boundary enforced
    .orElseThrow(...)
```

This prevents cross-tenant space access even if a valid JWT exists for a different org.

### Data Residency

No new data types are introduced. The `spaces.slug` and `spaces.description` fields are organizational metadata (not PII). They follow the same data residency rules as the existing `spaces` table.

### Input Validation

New request DTOs require validation:

```java
public record UpdateSpaceRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2000) String description
) {}
```

Slug generation from name is server-side only — users cannot inject arbitrary slug values.

---

## Observability

### New Metrics to Monitor

| Metric | Alert Threshold | Rationale |
|---|---|---|
| `spaces.create.error_rate` | > 5% | Monitors slug collision rate (could indicate UX issue) |
| `spaces.delete.conflict_rate` | > 20% | High rate = users confused by non-empty space delete |
| `spaces.list.p95_latency` | > 200ms | ruleCount query performance |

### Key Log Events

All new service methods emit structured logs:
```json
{"event": "space.created", "spaceId": "...", "slug": "...", "orgId": "...", "userId": "..."}
{"event": "space.delete.blocked", "spaceId": "...", "ruleCount": 42, "orgId": "..."}
{"event": "space.slug.conflict", "requestedName": "...", "orgId": "..."}
```

### Frontend Error Tracking

All new page errors (SpaceDetailPage, SpaceSettingsPage) should be caught by the existing `ErrorBoundary` component, which provides user-friendly error messages and logs to the console.

---

## Rollback Strategy

### Database Migration (V047)

The V047 migration is designed to be safe:
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — idempotent
- Backfill `UPDATE` runs before the NOT NULL constraint is added
- Unique constraint added last (after backfill ensures no pre-existing conflicts)

**Rollback procedure** (if needed):
```sql
ALTER TABLE spaces DROP CONSTRAINT IF EXISTS spaces_slug_org_unique;
ALTER TABLE spaces DROP COLUMN IF EXISTS slug;
```

### Code Rollback

Auth-service is stateless — reverting to the previous JAR reverts all new endpoints. The `description` field being unmapped again is safe (column exists in DB, just not read/written by app).

---

## Summary: Enterprise Readiness Checklist

| Concern | Status | Notes |
|---|---|---|
| RBAC enforcement | ✅ Ready | `ensureOwner()` new method; `ensureAdminOrOwner()` reused |
| Cascading delete safety | ✅ Ready | COUNT check before delete, 409 response |
| Slug uniqueness collision | ✅ Ready | DB constraint + 409 handler |
| Audit logging | ✅ Ready | `@AuditLog` on new endpoints |
| Multi-tenancy isolation | ✅ Ready | `orgId` filter on all queries |
| Input validation | ✅ Ready | `@NotBlank`, `@Size` on request DTOs |
| Frontend error handling | ✅ Ready | Existing `ErrorBoundary`, inline error display |
| Migration safety | ✅ Ready | Idempotent, reversible |

**Status:** COMPLETE — Ready for UX Design and Tech Lead Story Refinement
**Enterprise Architect Sign-Off:** BMAD EA Agent, 2026-03-14
