# Team Conventions: RuleForge

> Agents read this and follow these rules strictly.
> Maintained by Tech Lead.

## Version Control

- **Branch strategy:** GitHub Flow (feature branches off main, squash merge)
- **Branch naming:** `feat/<short-description>` or `fix/<short-description>` — e.g. `feat/space-rule-assignment`
- **Commit format:** Conventional Commits — `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`
  - Examples from this repo: `feat: Add initial Ruleforge UI/UX design`, `fix: return 401 (not 403) for unauthenticated requests`, `fix: install ai-service Python deps before starting uvicorn`
- **PR size target:** < 400 lines changed — split epics into stories to keep PRs small
- **Required reviewers:** Tech Lead reviews all PRs before merge
- **Worktrees:** Use git worktrees for feature isolation (see `.bmad-agents/tech-lead/`)

## Code Style

### Java (Engine + Platform)

- **Build tool:** Gradle with Kotlin DSL (`build.gradle.kts`)
- **Java version:** Java 21 (required — uses records, sealed classes, text blocks)
- **Package root:** `io.ruleforge` — all classes live here
- **Naming — classes:** PascalCase (`WebhookService`, `TenantContext`)
- **Naming — methods/fields:** camelCase (`findByOrgId`, `currentOrgId`)
- **Naming — packages:** lowercase dot-separated (`io.ruleforge.event.service`)
- **Annotations:** Use `@JdbcTypeCode(SqlTypes.JSON)` on JSONB columns in Hibernate entities
- **Entity IDs:** `UUID` type throughout — never `Long` or `Integer`
- **Exception handling:** Throw `EntityNotFoundException(String, String)` — takes two String args (not UUID)
- **Lambda captures:** Must reference effectively-final variables — use `savedX` pattern
- **Spring Security:** Services use `@PreAuthorize` / manual RBAC checks; VIEWER role is read-only

### TypeScript / React (Frontend)

- **Framework:** React 18 + TypeScript 5.5 + Vite 5.4
- **Naming — files:** `PascalCase.tsx` for components/pages, `camelCase.ts` for hooks/utils/api
- **Naming — components:** PascalCase (`WebhookListPage`, `AssignRulesModal`)
- **Naming — hooks:** `use` prefix camelCase (`useWebhooks`, `useMoveRule`)
- **Naming — API files:** `<domain>.api.ts` — e.g. `webhooks.api.ts`, `org.api.ts`
- **State management:** Zustand for client state; React Query for server state (no Redux)
- **Component location:** Feature-scoped — `frontend/src/features/<domain>/components/`
- **Page location:** `frontend/src/features/<domain>/pages/`
- **Shared components:** `frontend/src/shared/components/` — e.g. `ConfirmDialog`, `EmptyState`
- **Styling:** Tailwind CSS 3.4 + shadcn/ui components
- **Icons:** Lucide React
- **Max file length:** 300 lines — split if longer

### Python (AI Service)

- **Framework:** FastAPI + uvicorn
- **Python version:** 3.12+
- **Naming:** snake_case for functions/vars, PascalCase for classes
- **Entry point:** `app.main:app` (uvicorn target)

## API Conventions

- **API style:** REST
- **URL format:** `/v1/<resource>/<id>/<sub-resource>` — e.g. `/v1/rules/123/move`, `/v1/spaces/abc/rules`
- **Verb conventions:**
  - `POST` for create
  - `PUT` for full update
  - `PATCH` for partial update (e.g., `PATCH /rules/{id}/move`)
  - `DELETE` for removal
- **Error format:** Standard `ErrorResponse` DTO from `ruleforge-common` — `{ error, message, timestamp }`
- **Auth header:** `Authorization: Bearer <JWT>` or `X-API-Key: <key>`
- **Pagination:** `PageResponse<T>` wrapper with `content`, `totalElements`, `totalPages`, `page`, `size`
- **Multi-tenancy:** All tenant-scoped endpoints derive org from JWT `orgId` claim via `TenantContext`
- **401 vs 403:** 401 = unauthenticated (missing/expired token); 403 = authenticated but unauthorized (wrong role)
- **Idempotency:** PATCH endpoints must be safe to call twice with the same params

## API Gateway (Kong) — Docker Only

In local dev, Vite proxy routes directly to each service port. In Docker/prod, Kong on port 8000 routes:

| Path prefix | Service | Port |
|---|---|---|
| `/v1/auth`, `/v1/users`, `/v1/organizations`, `/v1/spaces`, `/v1/api-keys`, `/v1/sso` | auth-service | 8081 |
| `/v1/rules`, `/v1/folders`, `/v1/tags` | management-service | 8082 |
| `/v1/solve` | solver-service | 8083 |
| `/v1/audit`, `/v1/webhooks`, `/v1/events` | event-service | 8084 |
| `/v1/analytics` | analytics-service | 8085 |
| `/v1/schedules`, `/v1/batch` | scheduler-service | 8086 |
| `/v1/connectors`, `/v1/connections` | connector-service | 8087 |
| `/v1/ai` | ai-service | 8000 |

## Testing

- **Unit test coverage target:** 80% line coverage
- **Test naming (Java):** `should_[expected]_when_[condition]`
- **Test framework (Java):** JUnit 5 + Mockito
- **Test framework (frontend):** Vitest + React Testing Library
- **Test file location (frontend):** co-located alongside source — `*.test.ts` / `*.test.tsx`
- **Integration tests:** Smoke test suite at `scripts/smoke-test.sh` — runs against live stack
  - Run: `./scripts/smoke-test.sh` (full) or `--quick` (health only) or `--verbose` (show response bodies)
- **Test accounts:** See `db/migrations/V021__seed_test_accounts.sql` (admin / editor / viewer)
- **Engine tests:** Use `evaluate(CompiledRule, WorkingMemory)` for ad-hoc evaluation — not ID-based lookup
- **Table conditions:** Values must be numeric JSON when using GTE/LTE operators
- **No direct DB access from controllers** — always via repository/service pattern

## Database

- **Migration tool:** Flyway
- **Migration naming:** `V{N}__{description}.sql` — e.g. `V047__add_space_slug.sql`
- **Migration location:** `db/migrations/`
- **Local dev:** `flyway.enabled: false` + `spring.jpa.hibernate.ddl-auto: update` in `application.yml`
- **Run migrations:** `make migrate` (applies against local PostgreSQL)
- **Multi-tenancy:** All tenant tables have RLS policies enabled; session variable `app.current_org_id` set from JWT
- **JSONB columns:** Must have `@JdbcTypeCode(SqlTypes.JSON)` annotation on Hibernate entities
- **No direct DB access from controllers** — always via repository pattern

## Security

- **JWT:** HS512, 64+ byte secret key required
- **Token expiry:** Custom `AuthenticationEntryPoint` returns 401 (not 403) for expired/missing tokens
- **RBAC roles:** OWNER (full), EDITOR (create/update rules), VIEWER (read-only)
- **SSRF protection:** All webhook URL targets pass through `SsrfGuard` validation
- **Rate limiting:** Applied at webhook delivery layer and API endpoints
- **XSS:** All dynamic content rendered via React (not `dangerouslySetInnerHTML`)
- **CSRF:** CSRF protection enabled on state-changing endpoints

## Documentation

- **Public APIs:** OpenAPI/Swagger docs required for new endpoints
- **Complex logic:** Inline comments explaining WHY not WHAT
- **ADRs required for:** Any decision affecting >1 service or with long-term structural consequences
- **ADR location:** `docs/architecture/adr/ADR-{N}-{title}.md`
- **ADR index:** `docs/architecture/adr/ADR-INDEX.md` — update after each new ADR

## Sprint / Process (BMAD)

- **Epic structure:** Analysis → Planning → Solutioning → Implementation (per `.bmad/` and `docs/.bmad/handoff-log.md`)
- **Story size limit:** Max 5 points — split if larger
- **Definition of Done:** Code reviewed by TL, smoke tests passing, frontend TypeScript 0 errors, PO accepted
- **Smoke test gate:** All new features must add a numbered section to `scripts/smoke-test.sh`
- **Frontend build gate:** `npm run build` must produce 0 TypeScript errors before release
- **Backend build gate:** `./gradlew build` must be BUILD SUCCESSFUL before release

## Local Development

```bash
# Start everything (build + infra + services + frontend)
./scripts/start-local.sh

# Fast restart (services only — infra running, no code changes)
./scripts/start-local.sh --skip-build --skip-infra

# Stop services (keep infra running)
./scripts/stop-local.sh

# Smoke tests
./scripts/smoke-test.sh
```

- Service logs: `logs/ruleforge-<service>.log`
- No Kong in local dev — Vite proxy handles routing
- `NoOpEventPublisher` auto-registers when Kafka is unavailable (`@ConditionalOnMissingBean`)
- AI service optional — silently skipped if Python 3 not available
