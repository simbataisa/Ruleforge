# Solution Architecture — Spaces Page Features

**Version:** 1.0
**Date:** 2026-03-14
**Author:** BMAD Solution Architect Agent (Phase 3a)
**Input:** `docs/prd.md`, `docs/project-brief.md`

---

## Executive Summary

The Spaces Page Features epic delivers full CRUD lifecycle for Spaces within the existing RuleForge microservice platform. The architecture is **evolutionary** — no new services, no new infrastructure. Changes are confined to two existing services (auth-service for space CRUD, management-service for approval settings UI data), one DB migration, and frontend pages/routes.

**Key Decisions:**
- ADR-001: Slug stored in DB (not computed on-the-fly) for stable URLs and unique constraints
- ADR-002: `ruleCount` computed via real-time SQL COUNT join in auth-service (same DB, no cross-service call)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Frontend (React, port 3000)                       │
│                                                                      │
│  /spaces          SpaceListPage        [ENHANCED]                    │
│  /spaces/:id      SpaceDetailPage      [NEW]                         │
│  /spaces/:id/settings  SpaceSettingsPage [NEW]                       │
│  /settings        OrgSettingsPage      [FIX: Manage button]          │
└───────────┬─────────────────────────────────────────────────────────┘
            │ HTTP via Vite proxy
            │
┌───────────▼────────────────────┐   ┌─────────────────────────────────┐
│  auth-service (port 8081)       │   │  management-service (port 8082)  │
│                                 │   │                                  │
│  GET    /v1/organizations/      │   │  GET  /v1/spaces/:id/            │
│         current/spaces          │   │       approval-settings  [EXISTS]│
│  POST   /v1/organizations/      │   │  PUT  /v1/spaces/:id/            │
│         current/spaces   [MOD]  │   │       approval-settings  [EXISTS]│
│  GET    /v1/organizations/      │   └─────────────────────────────────┘
│         current/spaces/:id [NEW]│
│  PUT    /v1/organizations/      │
│         current/spaces/:id [NEW]│
│  DELETE /v1/organizations/      │
│         current/spaces/:id [NEW]│
└───────────┬─────────────────────┘
            │ JPA / JDBC
            ▼
┌─────────────────────────────────┐
│  PostgreSQL (port 5432)          │
│                                  │
│  spaces table [V047 migration]   │
│  + slug column (VARCHAR 255)     │
│  + UNIQUE (org_id, slug)         │
└─────────────────────────────────┘
```

---

## Service Changes

### auth-service (primary)

**Domain:** Space lifecycle management (CRUD, enriched DTO)

**New/Modified Endpoints:**

| Method | Path | Status | Auth |
|--------|------|--------|------|
| GET | `/v1/organizations/current/spaces` | Modified (enrich DTO) | Any member |
| POST | `/v1/organizations/current/spaces` | Modified (add slug) | ADMIN/OWNER |
| GET | `/v1/organizations/current/spaces/:id` | New | Any member |
| PUT | `/v1/organizations/current/spaces/:id` | New | ADMIN/OWNER |
| DELETE | `/v1/organizations/current/spaces/:id` | New | OWNER only |

**Files Changed:**
- `entity/SpaceEntity.java` — add `slug`, `description` fields
- `dto/SpaceDto.java` — add `slug`, `description`, `ruleCount`, `updatedAt`
- `service/SpaceService.java` — add `updateSpace()`, `deleteSpace()`, `getRuleCount()`, slug generation
- `controller/CurrentOrgController.java` — add GET /:id, PUT /:id, DELETE /:id endpoints
- `repository/SpaceRepository.java` — add `findByOrgIdAndSlug()`, slug uniqueness check

**New Utility:**
- `util/SlugUtils.java` — `toSlug(String name)` utility method

### management-service (frontend-only gap)

No backend changes needed. The `SpaceApprovalSettingsController` already exposes:
- `GET /v1/spaces/:id/approval-settings`
- `PUT /v1/spaces/:id/approval-settings`

Frontend just needs to call these from the new SpaceSettingsPage.

---

## Data Model

### V047 Migration — Add Slug Column

```sql
ALTER TABLE spaces ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- Backfill existing spaces
UPDATE spaces
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL;

-- Make slug NOT NULL after backfill
ALTER TABLE spaces ALTER COLUMN slug SET NOT NULL;

-- Unique slug per org
ALTER TABLE spaces
  ADD CONSTRAINT spaces_slug_org_unique UNIQUE (org_id, slug);
```

### Updated SpaceEntity Fields

```
spaces
  id           UUID         PK
  org_id       UUID         FK → organizations(id), NOT NULL
  name         VARCHAR(255) NOT NULL
  slug         VARCHAR(255) NOT NULL          [NEW]
  description  TEXT         NULL              [already in DB, just not mapped]
  limits       JSONB        DEFAULT '{}'
  created_at   TIMESTAMPTZ  NOT NULL
  updated_at   TIMESTAMPTZ  NOT NULL
  UNIQUE(org_id, slug)                        [NEW constraint]
```

Note: `description` column already exists in the `spaces` table (from V003 migration) but is not currently mapped in `SpaceEntity`. We simply add the `@Column` mapping.

### ruleCount Query

Auth-service shares the same PostgreSQL database as management-service. The rule count is computed inline using a JPQL count query:

```java
// In SpaceRepository
@Query("SELECT COUNT(r) FROM RuleEntity r WHERE r.spaceId = :spaceId")
long countRulesBySpaceId(@Param("spaceId") UUID spaceId);
```

This requires a `RuleEntity` read-access in auth-service (or use native SQL COUNT directly). Given auth-service uses `ddl-auto: update` and shares the DB, the simplest approach is a native count query:

```java
@Query(value = "SELECT COUNT(*) FROM rules WHERE space_id = :spaceId", nativeQuery = true)
long countRulesBySpaceId(@Param("spaceId") UUID spaceId);
```

---

## API Contracts

### SpaceDto (Updated)

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Production Rules",
  "slug": "production-rules",
  "description": "Rules for production deployment",
  "ruleCount": 42,
  "orgId": "...",
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-03-14T08:00:00Z"
}
```

### PUT /v1/organizations/current/spaces/:id

**Request:**
```json
{
  "name": "Updated Space Name",
  "description": "Optional updated description"
}
```

**Responses:**
- `200 OK` — SpaceDto (updated)
- `403 Forbidden` — Insufficient role (not ADMIN/OWNER)
- `404 Not Found` — Space not found in this org
- `409 Conflict` — `{"code": "SLUG_CONFLICT", "message": "A space with this name already exists in your organization"}`

### DELETE /v1/organizations/current/spaces/:id

**Responses:**
- `204 No Content` — Deleted successfully
- `403 Forbidden` — Not OWNER role
- `404 Not Found` — Space not found
- `409 Conflict` — `{"code": "SPACE_NOT_EMPTY", "message": "This space contains rules and cannot be deleted. Remove all rules first."}`

### GET /v1/organizations/current/spaces/:id

**Responses:**
- `200 OK` — SpaceDto (enriched)
- `404 Not Found` — Space not found

---

## Frontend Architecture

### New Pages

**SpaceDetailPage** (`/spaces/:id`)
```
Breadcrumb: Spaces > {name}
Stats bar: {ruleCount} rules | Created {date} | Updated {date}
Tabs: [Overview (default)] [Rules link]
Action: Settings button → /spaces/:id/settings
States: loading, 404 error, data
```

**SpaceSettingsPage** (`/spaces/:id/settings`)
```
Breadcrumb: Spaces > {name} > Settings
Back button: → /spaces/:id

Tabs:
  General:
    - Name field (required)
    - Description field (textarea, optional)
    - Save button (PUT /organizations/current/spaces/:id)

  Approval Settings (P1, deferred):
    - Approvers list
    - SLA hours
    - Require all toggle
    - Save (PUT /v1/spaces/:id/approval-settings)

  Danger Zone:
    - Delete Space button (red)
    - ConfirmDialog: "Delete '{name}'? This cannot be undone."
    - On confirm: DELETE /organizations/current/spaces/:id
    - On success: navigate('/spaces')
```

### API Hooks (new in `org.api.ts`)

```typescript
export interface OrgSpace {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  ruleCount: number;
  orgId: string;
  createdAt: string;
  updatedAt: string;
}

// New hooks:
useSpace(spaceId: string)          // GET /organizations/current/spaces/:id
useUpdateSpace()                   // PUT /organizations/current/spaces/:id
useDeleteSpace()                   // DELETE /organizations/current/spaces/:id
useSpaceApprovalSettings(spaceId)  // GET /v1/spaces/:id/approval-settings
useUpdateSpaceApprovalSettings()   // PUT /v1/spaces/:id/approval-settings
```

### Route Additions (`frontend/src/app/routes.tsx`)

```typescript
const SpaceDetailPage = lazy(() =>
  import('@/features/spaces/pages/SpaceDetailPage').then(...)
);
const SpaceSettingsPage = lazy(() =>
  import('@/features/spaces/pages/SpaceSettingsPage').then(...)
);

// In router children:
{ path: '/spaces/:id', element: <Lazy><SpaceDetailPage /></Lazy> }
{ path: '/spaces/:id/settings', element: <Lazy><SpaceSettingsPage /></Lazy> }
```

---

## Sequence Diagrams

### Create Space (with Slug)
```
User → POST /organizations/current/spaces {name}
     → SpaceService.createSpace()
     → SlugUtils.toSlug(name) → "my-space"
     → SpaceRepository.save()
     → IF UniqueConstraintViolation → 409 Conflict
     → ELSE → return SpaceDto {id, name, slug, ...}
```

### Delete Space (with Guard)
```
User → DELETE /organizations/current/spaces/:id
     → SpaceService.deleteSpace()
     → organizationService.ensureOwner(orgId, userId)  → 403 if not OWNER
     → spaceRepository.findById() → 404 if not found
     → spaceRepository.countRulesBySpaceId() → 409 if > 0
     → spaceRepository.delete()
     → 204 No Content
```

---

## SlugUtils

```java
public final class SlugUtils {
    private SlugUtils() {}

    public static String toSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9]+", "-")
                   .replaceAll("^-|-$", "");
    }
    // "My First Space!" → "my-first-space"
    // "Q4 Sales & Marketing" → "q4-sales-marketing"
    // "  hello  world  " → "hello-world"
}
```

---

## Security Architecture

| Operation | Required Role | Auth Mechanism |
|---|---|---|
| GET /spaces | Any org member | JWT, `TenantContext.orgId()` |
| GET /spaces/:id | Any org member | JWT, `TenantContext.orgId()` |
| POST /spaces | ADMIN or OWNER | `organizationService.ensureAdminOrOwner()` |
| PUT /spaces/:id | ADMIN or OWNER | `organizationService.ensureAdminOrOwner()` |
| DELETE /spaces/:id | OWNER only | `organizationService.ensureOwner()` (new) |
| GET /spaces/:id/approval-settings | ADMIN or OWNER | Existing in management-service |
| PUT /spaces/:id/approval-settings | ADMIN or OWNER | Existing in management-service |

---

## Performance & Scalability

- `ruleCount` is a COUNT query per space — O(1) with index on `rules.space_id`
- List endpoint calls `countRulesBySpaceId` N times (one per space). For large orgs with many spaces, this is a known N+1. **Mitigation:** Single bulk COUNT query using GROUP BY in a single SQL call for the list endpoint.

```sql
-- Optimized ruleCount for list endpoint
SELECT space_id, COUNT(*) as rule_count
FROM rules
WHERE space_id IN (:spaceIds)
GROUP BY space_id
```

This is called once and results mapped by spaceId in the service layer.

---

## ADR References

- [ADR-001: Slug Generation Strategy](adr/ADR-001-slug-generation.md)
- [ADR-002: ruleCount Computation Strategy](adr/ADR-002-rulecount-strategy.md)

---

**Status:** COMPLETE — Handoff to Enterprise Architect and UX Designer
**Solution Architect Sign-Off:** BMAD SA Agent, 2026-03-14
