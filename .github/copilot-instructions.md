# RuleForge Copilot Instructions

## Build, test, and lint commands

### Preferred local workflow

- Start the full local stack: `./scripts/start-local.sh`
- Fast restart without rebuilding or infra: `./scripts/start-local.sh --skip-build --skip-infra`
- Stop local services: `./scripts/stop-local.sh`
- Health and API smoke tests: `./scripts/smoke-test.sh`
- Health-only smoke tests: `./scripts/smoke-test.sh --quick`
- Helpful Make aliases: `make dev-up`, `make dev-up-fast`, `make dev-down`, `make dev-status`, `make smoke-test`

### Engine (`engine/`)

- Build all engine modules: `cd engine && ./gradlew build`
- Run all engine tests: `cd engine && ./gradlew test`
- Compile without tests: `cd engine && ./gradlew compileJava`
- Run one engine module test class: `cd engine && ./gradlew :ruleforge-core:test --tests io.ruleforge.core.memory.WorkingMemoryTest`
- Run one engine test method: `cd engine && ./gradlew :ruleforge-core:test --tests io.ruleforge.core.memory.WorkingMemoryTest.assertAndRetrieveFact`

### Platform services (`platform/`)

- Build all platform services: `cd platform && ./gradlew build`
- Run all platform tests: `cd platform && ./gradlew test`
- Run one service test class: `cd platform && ./gradlew :ruleforge-management-service:test --tests io.ruleforge.management.service.RuleValidationServiceTest`
- Run one service test method: `cd platform && ./gradlew :ruleforge-solver-service:test --tests io.ruleforge.solver.service.SolverServiceTest.solve_returnsEngineOutput`

### Frontend (`frontend/`)

- Install dependencies: `cd frontend && npm install`
- Start dev server: `cd frontend && npm run dev`
- Build: `cd frontend && npm run build`
- Lint: `cd frontend && npm run lint`
- Run all tests: `cd frontend && npm test`
- Run a single test file: `cd frontend && npm test -- src/api/client.test.ts`
- Run tests by name: `cd frontend && npm test -- --grep "LoginPage"`

### AI service (`ai-service/`)

- Install dependencies: `cd ai-service && pip install -r requirements.txt`
- Start locally: `cd ai-service && uvicorn app.main:app --reload --port 8000`
- Run tests: `cd ai-service && pytest`
- Run a single test: `cd ai-service && pytest tests/test_health.py::test_health`

### Infrastructure and migrations

- Start infra only: `make infra-up`
- Stop infra: `make infra-down`
- Run Flyway migrations: `make migrate`
- Full Docker Compose stack: `make stack-up` / `make stack-down`

## High-level architecture

RuleForge is split into an embeddable Java rule engine, a Spring Boot microservice platform, a React frontend, an optional FastAPI AI service, and shared local infrastructure.

- `engine/` is a Gradle multi-module build. `ruleforge-core` defines the shared model, working memory, RFL parser, and the `RuleTypeEngine` SPI. Rule-type implementations live in sibling modules such as table, tree, scorecard, flow, script, CEP, DMN, ensemble, and hierarchy.
- `platform/` is a separate Gradle composite build that includes `../engine` with `includeBuild("../engine")`. The shared `ruleforge-common` module holds common DTOs and tenant/security plumbing, and the service modules provide auth, management, solver, event, analytics, scheduler, and connector APIs.
- `frontend/` is a React 18 + Vite app. In local development it does not go through Kong; `frontend/vite.config.ts` proxies `/v1/*` routes directly to service ports 8081-8087 and the AI service on 8000.
- `ai-service/` is a Python FastAPI service used for AI-assisted rule workflows and is optional in local startup.
- `db/migrations/` contains the Flyway schema history for the shared PostgreSQL database.
- `infra/` holds the Docker Compose setup for PostgreSQL, Redis, Kafka, Elasticsearch, MinIO, and the full-stack deployment wiring.

The important runtime flow is:

1. The frontend calls `/v1/*`.
2. In local dev, Vite proxies those requests directly to individual services.
3. `management-service` owns rule CRUD and publishing.
4. `solver-service` loads and executes compiled rules using the shared engine and caches compiled rules in Caffeine.
5. Kafka carries lifecycle and execution events to downstream services such as `event-service` and `analytics-service`.

Multi-tenancy is enforced in PostgreSQL, not only in application code. `TenantContext` in `platform/ruleforge-common` is the per-request thread-local carrier, and the database uses `app.current_org_id` in RLS policies from `db/migrations/V020__enable_rls.sql`.

## Key conventions

### Engine registration must stay aligned across management and solver

`platform/ruleforge-management-service/.../EngineConfig.java` and `platform/ruleforge-solver-service/.../EngineConfig.java` both build a `RuleForgeEngine` and register the supported rule engines. When adding a new rule type or action executor, update both places unless the change is intentionally service-specific.

### The platform build depends on the engine composite build

`platform/settings.gradle.kts` includes `../engine`, so platform changes can rely on local engine module edits without publishing artifacts first. If engine APIs change, expect platform compilation to break immediately.

### API responses are wrapped

Backend endpoints use `ApiResponse<T>` from `platform/ruleforge-common/src/main/java/io/ruleforge/common/dto/ApiResponse.java`. Frontend hooks consistently read `res.data.data`, not `res.data` directly. Preserve that shape when adding endpoints or frontend API hooks.

### Tenant isolation relies on context propagation plus RLS

`TenantContext` is a thread-local carrier, while the database policies enforce org scoping with `current_setting('app.current_org_id', true)`. Tenant-aware backend work should preserve that flow; do not treat org filtering as a purely controller-level concern.

### Local service persistence is intentionally relaxed

Platform `application.yml` files use:

- `spring.jpa.hibernate.ddl-auto: update`
- `spring.flyway.enabled: false`

for local development. Schema changes still belong in `db/migrations/`; the relaxed JPA settings are just for local startup convenience.

### Solver caching is space-scoped and event-invalidated

`RuleCacheService` uses Caffeine with cache keys shaped like `spaceId:ruleIdOrAlias:version`. `CacheInvalidationListener` listens to the Kafka topic `rule.lifecycle` and invalidates cached entries when rules are published, deleted, or archived. Changes to publishing or rule lifecycle behavior need to consider solver cache invalidation.

### Frontend server state uses React Query; session state uses Zustand

Shared API hooks live in `frontend/src/api/*.api.ts` and are typically React Query hooks with explicit query keys and invalidations. Persistent auth/session behavior lives in `frontend/src/stores/auth.store.ts`, which stores the token in `localStorage` under `ruleforge_token` and drives expiry warnings/logout behavior.

### Use the current-user endpoint under `/users/me`

Frontend API code and smoke tests use `/v1/users/me`; there is no `/v1/auth/me` flow in this codebase. Keep new auth-related UI and integrations aligned with the existing route shape.

### Script rule support has a couple of non-obvious constraints

Per the existing repository guidance and engine wiring:

- script rules should define a function named `evaluate`
- the script language is `"js"`, not `"javascript"`

### Local startup order matters

`scripts/start-local.sh` treats auth as the first platform dependency, then starts the rest of the services, optional AI service, and frontend. If you are debugging boot failures, check auth first and then use `make dev-status` or the per-service logs under `logs/`.
