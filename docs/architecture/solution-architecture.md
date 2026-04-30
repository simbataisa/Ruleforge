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

---

# Brownfield Solution Architecture Onboard Snapshot

> **Status:** Draft — auto-seeded by /solution-architect:onboard. Source: code structure + controller mappings as of 2026-04-30. Owner: Solution Architect

This snapshot preserves the existing Spaces Page architecture above and records repo-wide solution architecture signals discovered during brownfield onboarding. It is descriptive, not yet authoritative for future refactoring decisions.

## 1. Service Decomposition

Detected pattern: Microservices platform plus embeddable engine modules and SDKs.

| Component | Source | Language / Runtime | Boundary |
|---|---|---|---|
| `platform/ruleforge-auth-service` | Gradle module | Java 21 / Spring Boot 3.3 | Auth, orgs, members, spaces, API keys, SSO |
| `platform/ruleforge-management-service` | Gradle module | Java 21 / Spring Boot 3.3 | Rules, folders, versions, approvals, tags, import/export |
| `platform/ruleforge-solver-service` | Gradle module | Java 21 / Spring Boot 3.3 | Runtime rule execution and bulk solving |
| `platform/ruleforge-event-service` | Gradle module | Java 21 / Spring Boot 3.3 | Audit, notifications, webhook delivery, webhook metrics |
| `platform/ruleforge-analytics-service` | Gradle module | Java 21 / Spring Boot 3.3 | Decision analytics and AB test reporting |
| `platform/ruleforge-scheduler-service` | Gradle module | Java 21 / Spring Boot 3.3 | Schedules, workflow triggers, holiday calendars |
| `platform/ruleforge-connector-service` | Gradle module | Java 21 / Spring Boot 3.3 | External data connectors and internal enrichment |
| `platform/ruleforge-common` | Gradle module | Java 21 | Shared security, event, tenant, and API utilities |
| `engine/ruleforge-*` | Gradle composite build | Java 21 | Embeddable rule engine packages; `ruleforge-core` has minimal dependency constraints |
| `frontend` | `package.json` | React 18 / TypeScript / Vite | Browser application |
| `ai-service` | `pyproject.toml` | Python / FastAPI | AI-assisted rule generation and suggestions |
| `sdks/sdk-java`, `sdks/sdk-node`, `sdks/sdk-python` | SDK manifests | Java, TypeScript, Python | External client SDKs |

Boundary rationale: [populate] Confirm whether shared PostgreSQL plus RLS remains the intended coupling model for all service boundaries, especially as webhook, analytics, and scheduler workflows grow.

## 2. API Contracts

No OpenAPI, Swagger, proto, GraphQL, or AsyncAPI contract files were detected. The API surface below is inferred from Spring controller mappings and should be formalized before external SDK or partner commitments.

| API | Endpoint / Resource | Style | Spec source | Status |
|---|---|---|---|---|
| API-001 | `/v1/auth/*` | REST | `platform/ruleforge-auth-service/.../AuthController.java` | Inferred from code |
| API-002 | `/v1/users/*` | REST | `platform/ruleforge-auth-service/.../UserController.java` | Inferred from code |
| API-003 | `/v1/organizations/*` and `/v1/organizations/current/*` | REST | `platform/ruleforge-auth-service/.../*Org*Controller.java` | Inferred from code |
| API-004 | `/v1/spaces/{spaceId}/api-keys`, `/v1/api-keys/*` | REST | `platform/ruleforge-auth-service/.../ApiKeyController.java` | Inferred from code |
| API-005 | `/v1/sso/*` and `/internal/api-keys/validate` | REST | `platform/ruleforge-auth-service/.../SsoController.java`, `InternalController.java` | Inferred from code |
| API-006 | `/v1/spaces/{spaceId}/rules`, `/v1/rules/*` | REST | `platform/ruleforge-management-service/.../RuleController.java` | Inferred from code |
| API-007 | `/v1/rules/{ruleId}/versions/*` | REST | `platform/ruleforge-management-service/.../VersionController.java` | Inferred from code |
| API-008 | `/v1/folders/*`, `/v1/spaces/{spaceId}/folders` | REST | `platform/ruleforge-management-service/.../FolderController.java` | Inferred from code |
| API-009 | `/v1/approval-requests/*`, `/v1/spaces/{spaceId}/approval-settings` | REST | `platform/ruleforge-management-service/.../*Approval*Controller.java` | Inferred from code |
| API-010 | `/v1/rules/{ruleId}/ab-tests/*`, `/v1/tags`, rule tags | REST | `platform/ruleforge-management-service/.../AbTestController.java`, `TagController.java` | Inferred from code |
| API-011 | `/v1/solve/*` | REST | `platform/ruleforge-solver-service/.../SolverController.java` | Inferred from code |
| API-012 | `/v1/audit/*`, `/v1/events/stream`, `/v1/webhooks/*` | REST / SSE | `platform/ruleforge-event-service/.../*Controller.java` | Inferred from code |
| API-013 | `/v1/analytics/*` | REST | `platform/ruleforge-analytics-service/.../AnalyticsController.java` | Inferred from code |
| API-014 | `/v1/schedules/*`, `/v1/workflows/*`, `/v1/holiday-calendars/*` | REST | `platform/ruleforge-scheduler-service/.../*Controller.java` | Inferred from code |
| API-015 | `/v1/connectors/*`, `/internal/enrich` | REST | `platform/ruleforge-connector-service/.../*Controller.java` | Inferred from code |

## 3. Data Model Summary

Migration strategy detected: Flyway SQL migrations in `db/migrations/`, with PostgreSQL 16 and RLS policies. Local development service configs also reference `ddl-auto: update`; production ownership should remain with Flyway migrations.

| Entity | Source | Owner service | Notes |
|---|---|---|---|
| Organization, user, member, team | `V001`, `V002`, `V004`, `V005` | auth-service | Tenant and identity foundation |
| Space, folder, rule, rule version | `V003`, `V006`, `V007`, `V008`, `V047` | auth-service / management-service | Core rule authoring hierarchy |
| API key, SSO config | `V009`, `V016` | auth-service | Machine auth and enterprise identity |
| Tag, rule tag | `V010`, `V033` | management-service | Rule discovery metadata |
| Audit event, notification | `V011`, `V032`, `V038`, `V046` | event-service | Audit trail and user notifications |
| Webhook, webhook outbox, webhook delivery metrics | `V012`, `V048`, `V050`, `V052` | event-service | Outbound integration delivery and auth |
| Connector | `V013` | connector-service | External data enrichment connections |
| Schedule, schedule run, workflow, workflow run | `V014`, `V015`, `V042`, `V043`, `V044` | scheduler-service | Timed and workflow execution |
| AB test | `V017` | management-service / analytics-service | Experiment routing and analytics |
| Approval request, approval assignee, approval settings, comments | `V023`, `V031`, `V034`, `V036` | management-service | Rule approval workflows |
| Rule sample input | `V026`, `V028` | management-service | Rule test fixtures |
| Holiday calendar, holiday entry | `V045` | scheduler-service | Schedule exception handling |

## 4. Integration Patterns

- Kafka 3.7 is used for rule lifecycle, execution, activity, and system events; producers live in shared common code and consumers exist in solver, analytics, and event services.
- Redis 7 is used for distributed caching, AB test routing cache, audit/webhook related storage, and OAuth2 webhook token caching.
- Quartz powers scheduler-service job execution.
- Webhook delivery uses an outbox pattern with retry semantics and circuit/reset operations.
- Server-Sent Events are exposed at `/v1/events/stream` for notifications.
- [populate] Define sync vs async boundaries, retry budgets, idempotency keys, dead-letter handling, and replay ownership per topic/outbox.

## 5. External Integrations

| Integration | Purpose | Direction | Failure mode |
|---|---|---|---|
| PostgreSQL | System of record and RLS enforcement | Internal dependency | Request failure, degraded writes/reads |
| Kafka | Domain event stream and analytics feed | Internal dependency | Delayed consumers, event loss risk if producer/outbox contracts drift |
| Redis | L2 cache, token cache, routing cache | Internal dependency | Cache miss fallback, token refetch, possible latency increase |
| Elasticsearch | Decision log analytics | Internal dependency | Analytics degradation, search/reporting gaps |
| MinIO / S3 | Rule exports and batch artifacts | Internal or external storage | Export/import artifact failure |
| Kong | Production API gateway | Inbound infrastructure | Routing, CORS, or rate-limit failures |
| Webhook targets | Customer outbound integrations | Outbound HTTP | Retry exhaustion, AUTH_FAILURE, circuit breaker trips |
| OAuth2 token endpoints | Webhook auth token acquisition | Outbound HTTP | Auth failure status prevents retry storms |
| Anthropic / LLM provider | AI rule assistance | Outbound API | AI feature degradation; core rule execution should remain available |
| JDBC / HTTP / Redis connector targets | Data enrichment | Outbound data access | Connector test/fetch failures; rule execution may need fallback behavior |

## 6. Authentication & Authorization

Detected auth model: JWT bearer auth using shared Spring Security filters, API keys for machine access, PostgreSQL RLS via `app.current_org_id`, role-based org membership checks, and optional SAML/OIDC SSO configuration. Webhook auth supports bearer/basic/custom header/OAuth2-style outbound credential injection.

[populate] Confirm token lifetime, refresh behavior, API key scoping, service-to-service auth, and whether authorization remains role-based or evolves toward permission/policy-based authz.

## 7. Caching

Detected cache layers: Caffeine L1 in JVMs, Redis L2 distributed cache, PostgreSQL source of truth, and Redis-backed OAuth2 token cache for webhooks.

[populate] Document cache keys, invalidation events, TTLs, stampede protection, and degradation behavior when Redis is unavailable.

## 8. Observability Hooks

Detected hooks: Spring Actuator health/info endpoints, service log files under `logs/`, Kafka-backed audit events, analytics ingestion, and selected cache stats through actuator configuration.

[populate] Metrics and tracing remain under-specified. Decide Prometheus/Grafana, OpenTelemetry trace propagation, correlation IDs, and alert thresholds for webhook delivery, solve latency, scheduler lag, Kafka consumer lag, and auth failures.

## 9. ADRs Relevant to This Solution

- [ADR-001: Slug Generation Strategy for Spaces](adr/ADR-001-slug-generation.md)
- [ADR-002: ruleCount Computation Strategy](adr/ADR-002-rulecount-strategy.md)
- [ADR-003: Space-Scoped Rule API](adr/ADR-003-space-scoped-rule-api.md)
- [ADR-004: Move/Copy Rules](adr/ADR-004-move-copy-rules.md)
- [ADR-005: Sandbox Detection by Name](adr/ADR-005-sandbox-detection-by-name.md)
- [ADR-006: Assign-vs-Create Separation](adr/ADR-006-assign-vs-create-separation.md)
- [ADR-007: Webhook Credential Encryption](adr/ADR-007-webhook-credential-encryption.md)
- [ADR-008: Webhook Auth Injection Strategy](adr/ADR-008-webhook-auth-injection-strategy.md)
- [ADR-009: Webhook Metrics Storage](adr/ADR-009-webhook-metrics-storage.md)
- [ADR-010: OAuth2 Token Caching Strategy](adr/ADR-010-oauth2-token-caching-strategy.md)
- [ADR Index](adr/ADR-INDEX.md)

## 10. Open Questions for the SA

- Service decomposition: keep current microservices + shared database pattern, or refactor boundaries around ownership of data and events?
- API versioning policy: continue URL path versioning only, or add header/content-type version negotiation for SDK compatibility?
- Cross-service transactions: how is consistency enforced across shared tables, Kafka events, outbox delivery, and scheduler workflows?
- Caching invalidation strategy: which service owns invalidation for rule definitions, webhook OAuth2 tokens, AB test routing, and analytics projections?
- Contract governance: should Spring controller mappings be promoted to OpenAPI specs and used as SDK generation sources?
