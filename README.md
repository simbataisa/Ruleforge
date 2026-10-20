# RuleForge

**Enterprise Business Rules Management System**

---

## What is RuleForge?

RuleForge is an enterprise BRMS for authoring, managing, and executing business rules at scale. It combines an embeddable Java rule engine with a microservices platform and a modern React-based UI.

- **Embeddable Engine** — Zero external dependencies in the core module (SLF4J + Jackson only), suitable for embedding directly into Java applications.
- **8 Rule Types** — Decision Tables, Decision Trees, Scorecards, Ensembles, DMN, Rule Flows, Scripts (GraalVM), and Complex Event Processing.
- **Visual Rule Editors** — Spreadsheet-style decision tables, drag-and-drop tree editors, DAG flow designers, scorecard builders, and sandboxed script editors.
- **Multi-Tenant Platform** — Organization → Space → Folder → Rule hierarchy with PostgreSQL Row-Level Security, JWT authentication, API keys, and SSO support.
- **Production-Ready** — Multi-layer caching, Kafka-based event streaming, audit trails, batch scheduling, data connectors, client SDKs (Java, Node.js, Python), approval workflows, real-time notifications, and security hardening.

**Use cases:** Financial services (loan decisioning, risk scoring), insurance (underwriting, claims automation), healthcare (eligibility determination), e-commerce (dynamic pricing, promotions).

## Architecture Overview

```
┌───────────────────────────────────────────────────────────────────────┐
│                          RuleForge System                             │
│                                                                       │
│  ┌──────────────┐                       ┌───────────────────────────┐ │
│  │   Frontend    │───── Vite proxy ────▶ │     Platform Services     │ │
│  │  React 18     │   (local dev mode)    │                           │ │
│  │  port 3000    │                       │  auth-service       8081  │ │
│  └──────────────┘                       │  management-service 8082  │ │
│                                          │  solver-service     8083  │ │
│  ┌──────────────┐    ┌──────────────┐   │  event-service      8084  │ │
│  │   Frontend    │──▶│ Kong Gateway │──▶│  analytics-service  8085  │ │
│  │  (Docker)     │   │  port 8000   │   │  scheduler-service  8086  │ │
│  └──────────────┘    └──────────────┘   │  connector-service  8087  │ │
│                                          └──────────┬────────────────┘ │
│  ┌──────────────┐                                   │                  │
│  │  AI Service   │   Python/FastAPI, port 8000      │                  │
│  │   (optional)  │   (8088 in Docker)               │                  │
│  └──────────────┘                                   │                  │
│                                                      │                  │
│  ┌──────────────────────────────────────────────────┼────────────────┐ │
│  │                     Data Stores                   │                │ │
│  │  PostgreSQL 16 (5432)  Redis 7 (6379)  Kafka 3.7.1 (9092)        │ │
│  │  Elasticsearch 9 (9200)  MinIO (9000/9001)                        │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

> **Local dev vs Docker:** In local development, the Vite dev server proxies `/v1/*` API requests **directly** to each backend service (no Kong needed). In the full Docker Compose stack, all traffic routes through Kong on port 8000.

## Project Structure

```
ruleforge/
├── engine/                              # Embeddable rule engine (Gradle, Java 21)
│   ├── ruleforge-core/                  # Core: model, memory, SPI, RFL parser, engine API
│   ├── ruleforge-table/                 # Decision Table engine (7 index types)
│   ├── ruleforge-tree/                  # Decision Tree engine (BDD compilation)
│   ├── ruleforge-script/                # GraalVM sandboxed scripting
│   ├── ruleforge-flow/                  # DAG flow orchestrator
│   ├── ruleforge-scorecard/             # Weighted scoring engine
│   ├── ruleforge-ensemble/              # Voting/averaging engine
│   ├── ruleforge-cep/                   # Kafka Streams CEP
│   ├── ruleforge-dmn/                   # DMN 1.4 / FEEL
│   ├── ruleforge-hierarchy/             # Hierarchical resolution
│   ├── ruleforge-all/                   # Uber module (all engines)
│   └── ruleforge-spring-boot-starter/   # Auto-configuration for Spring Boot apps
├── platform/                            # Microservices (Spring Boot 4.1.1)
│   ├── ruleforge-common/                # Shared: JWT, security filters, DTOs, Kafka schemas
│   ├── ruleforge-auth-service/          # Auth, users, orgs, spaces, API keys, SSO
│   ├── ruleforge-management-service/    # Rule CRUD, versioning, folders, tags, import/export
│   ├── ruleforge-solver-service/        # Rule execution with multi-layer caching
│   ├── ruleforge-event-service/         # Kafka consumers, audit events, webhooks
│   ├── ruleforge-analytics-service/     # Decision logs, aggregation
│   ├── ruleforge-scheduler-service/     # Batch scheduling with Quartz
│   └── ruleforge-connector-service/     # DB/HTTP/Redis data enrichment
├── frontend/                            # React 18 + TypeScript + Vite + Tailwind CSS
├── ai-service/                          # AI assistant (Python, FastAPI)
├── sdks/                                # Client SDKs
│   ├── sdk-java/                        # Java SDK
│   ├── sdk-node/                        # Node.js SDK
│   └── sdk-python/                      # Python SDK
├── db/                                  # Flyway SQL migrations (V001–V028)
├── infra/                               # Docker Compose, Kong gateway config
├── scripts/                             # Dev scripts (start-local, stop-local, smoke-test)
└── docs/                                # Architecture and developer documentation
```

## Tech Stack

| Layer          | Technology                                                                 |
| -------------- | -------------------------------------------------------------------------- |
| Engine         | Java 21, zero-dep core (SLF4J + Jackson only), GraalVM 23.1.2              |
| Platform       | Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Jackson 3), PostgreSQL 16, Redis 7, Kafka 3.7.1, Elasticsearch 9.4.5, Caffeine |
| Build          | Gradle 8.14.6 (wrapper committed for `engine/` and `platform/`)            |
| Frontend       | React 18, TypeScript 5.5, Vite 5.4, Tailwind CSS 3.4, Zustand, React Query, Recharts, Monaco Editor |
| AI             | Python 3.12, FastAPI, Anthropic SDK                                        |
| Infrastructure | Docker, Kong 3.6 API Gateway, Flyway                                       |

### Spring Boot 4 notes for developers

The platform was upgraded from Spring Boot 3.3 to 4.1. When writing platform code:

- **JSON is Jackson 3.** Import from `tools.jackson.*` (e.g. `tools.jackson.databind.ObjectMapper`), not `com.fasterxml.jackson.*`. Annotations (`@JsonProperty`, etc.) stay on `com.fasterxml.jackson.annotation`. Jackson 3 exceptions are unchecked (`JacksonException`).
- **The shared mapper keeps Jackson 2 defaults.** `ruleforge-common`'s `JacksonConfig` defines a `JsonMapper` built with `builderWithJackson2Defaults()` and ISO-8601 dates, so API JSON output did not change.
- **Starters were renamed.** Use `spring-boot-starter-webmvc` (not `-web`), `spring-boot-starter-flyway`, and `spring-boot-starter-kafka` — Kafka auto-configuration now lives in its own module.
- **The engine stays on Jackson 2** so it remains embeddable in applications that are not on Spring Boot 4.

## Quick Start

### Prerequisites

- **Java 21** (with `JAVA_HOME` set). Use JDK 21 specifically: the bundled GraalVM polyglot runtime (23.1.2) fails on JDK 22+, which breaks Script rules at runtime.
- **Node.js 18+** and npm
- **Docker** and **Docker Compose** (v2.20+)
- _Optional:_ Python 3.10+ (for AI service), Flyway CLI (for standalone migrations)

### Option A: One-Command Local Dev Stack

The easiest way to start everything:

```bash
make dev-up
# or: ./scripts/start-local.sh
```

#### What the script does

`start-local.sh` orchestrates the full local development stack in six sequential phases:

| Phase | What happens |
|-------|--------------|
| **1. Prerequisites** | Validates Java 21+, Node.js 18+, and Docker are available. Aborts with a clear error if any are missing. |
| **2. Infrastructure** | Runs `docker compose up -d` for the infra stack (PostgreSQL 5432, Redis 6379, Kafka 9092, Elasticsearch 9200, MinIO 9000). Waits with retries for each service to become healthy before proceeding. |
| **3. Build** | Compiles the engine (`./gradlew build -x test`) and all platform services (`./gradlew build -x test`). Installs frontend npm dependencies. |
| **4. Platform services** | Starts the 7 Spring Boot JARs from `platform/*/build/libs/` as background processes, in dependency order: auth-service first, then management/event/connector/solver in parallel, then analytics, then scheduler. Each service is health-checked via its `/actuator/health` endpoint. |
| **5. AI service** | Starts `uvicorn` if Python 3 is available. Skipped silently otherwise. |
| **6. Frontend** | Starts the Vite dev server (`npm run dev`) as a background process. |

After startup, the script prints a service status table showing the port, PID, and running state of every process.

#### Flags

| Flag | Effect |
|------|--------|
| `--skip-build` | Skip Gradle and npm builds. Uses existing JARs in `build/libs/`. Fastest way to restart after a code change that doesn't need a rebuild. |
| `--skip-infra` | Skip starting Docker infrastructure. Use when containers are already running. |
| `--no-frontend` | Skip starting the Vite dev server. Useful for backend-only work. |

```bash
# Fast restart (services only, infra already up, no code changes):
./scripts/start-local.sh --skip-build --skip-infra

# Backend dev only:
./scripts/start-local.sh --no-frontend
```

#### Service logs

Each process writes its output to `logs/` in the project root:

```
logs/
├── ruleforge-auth-service.log
├── ruleforge-management-service.log
├── ruleforge-solver-service.log
├── ruleforge-event-service.log
├── ruleforge-analytics-service.log
├── ruleforge-scheduler-service.log
├── ruleforge-connector-service.log
├── ai-service.log
└── frontend.log
```

To tail a service log:

```bash
tail -f logs/ruleforge-solver-service.log
```

#### Common issues

| Symptom | Fix |
|---------|-----|
| `Java 21+ required (found )` | Ensure `JAVA_HOME` is set to a valid absolute path (e.g. `export JAVA_HOME="/path/to/java21"`). |
| `PostgreSQL did not become ready` | Check that `docker-postgres-1` is running: `docker ps`. If `pg_isready` isn't installed locally, the script falls back to `docker exec` automatically. |
| `No JAR found for <service>` | Run without `--skip-build` or rebuild manually: `cd platform && ./gradlew build -x test`. |
| Services start but return 500 | Database migrations may not have run. Apply them with `make migrate`. |
| Script rules return 500 with `NoSuchMethodError: sun.misc.Unsafe.ensureClassInitialized` | The services are running on JDK 22+. Point `JAVA_HOME` (and `PATH`) at JDK 21 and restart. |
| `analytics-service` fails to start: `status: 400, [es/indices.exists] Expecting a response body` | Elasticsearch is older than 9.x. Spring Boot 4 ships the Elasticsearch 9 client. Recreate the container from the current compose file: `docker compose -f infra/docker/docker-compose.infra.yml up -d elasticsearch` (remove the old container and `docker_es_data` volume first; 8.x data cannot be opened by 9.x). |
| `pull access denied for minio/minio` | MinIO is not used by any service. Start only what is needed: `docker compose -f infra/docker/docker-compose.infra.yml up -d postgres redis kafka elasticsearch`, then run the script with `--skip-infra`. |
| Gradle fails with `PKIX path building failed` | A TLS-intercepting proxy (e.g. Cloudflare WARP or a corporate proxy) is in the path and Java does not trust its CA. Import the proxy CA into a Java truststore and pass it via `JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=<path> -Djavax.net.ssl.trustStorePassword=<password>"`. |
| `No JAR found` after pulling `main` | JARs are not committed. Build once without `--skip-build`. |

#### Stopping

```bash
make dev-down        # Stop all services started by start-local.sh
make dev-down --include-infra  # Also stop Docker infrastructure containers
```

Or use `Ctrl+C` if running in the foreground — the script traps signals and shuts down all child processes cleanly.

### Option B: Step-by-Step

#### 1. Start Infrastructure

```bash
make infra-up
# Starts: PostgreSQL (5432), Redis (6379), Kafka (9092), Elasticsearch (9200), MinIO (9000)
```

#### 2. Run Database Migrations

```bash
make migrate
# Runs Flyway V001–V052 against local PostgreSQL
```

#### 3. Build the Engine

```bash
cd engine && ./gradlew build
```

The engine is a Gradle multi-module project with 12 modules. The core module (`ruleforge-core`) has zero external dependencies beyond SLF4J and Jackson, making it embeddable in any Java application.

#### 4. Build and Run Platform Services

```bash
cd platform && ./gradlew build
```

The platform uses a **composite Gradle build** that includes the engine directly (`settings.gradle.kts` contains `includeBuild("../engine")`).

Start services individually, in dependency order:

```bash
# 1. Auth service first (others validate tokens against it)
cd platform && ./gradlew :ruleforge-auth-service:bootRun

# 2. Core services (each in a separate terminal)
cd platform && ./gradlew :ruleforge-management-service:bootRun
cd platform && ./gradlew :ruleforge-solver-service:bootRun
cd platform && ./gradlew :ruleforge-event-service:bootRun

# 3. Optional services
cd platform && ./gradlew :ruleforge-analytics-service:bootRun
cd platform && ./gradlew :ruleforge-scheduler-service:bootRun
cd platform && ./gradlew :ruleforge-connector-service:bootRun
```

#### 5. Start the Frontend

```bash
cd frontend && npm install && npm run dev
# Runs on http://localhost:3000
```

The Vite dev server proxies all `/v1/*` API requests **directly** to the corresponding backend service (not through Kong). See `vite.config.ts` for the full proxy mapping.

#### 6. Start the AI Service (Optional)

```bash
cd ai-service
export RULEFORGE_AI_LLM_API_KEY=sk-ant-...
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### Option C: Full Docker Compose Stack

```bash
make stack-up    # Builds and starts everything (infra + services + Kong + frontend)
make stack-down  # Stops everything
```

In this mode, all API traffic routes through the **Kong API Gateway** on port 8000. The AI service maps to host port **8088** (internal port 8000).

## Services

| Service                      | Port | Purpose                                                |
| ---------------------------- | ---- | ------------------------------------------------------ |
| ruleforge-auth-service       | 8081 | JWT auth, API keys, RBAC, SSO config, user/org mgmt    |
| ruleforge-management-service | 8082 | Rule CRUD, versioning, folders, tags, import/export    |
| ruleforge-solver-service     | 8083 | Rule execution with L1/L2/L3 caching                   |
| ruleforge-event-service      | 8084 | Kafka consumers, audit trail, webhooks                 |
| ruleforge-analytics-service  | 8085 | Decision logs, Elasticsearch aggregation               |
| ruleforge-scheduler-service  | 8086 | Batch scheduling (Quartz), cron-based execution        |
| ruleforge-connector-service  | 8087 | JDBC/HTTP/Redis data enrichment, credential encryption |
| AI service                   | 8000 | LLM-powered rule generation, optimization (optional)   |
| Kong Gateway (Docker only)   | 8000 | API routing, CORS, rate limiting                       |

## Frontend API Proxy (Local Dev)

In local development, the Vite dev server routes requests directly to each service:

| Path Prefix                                                                           | Backend Service           |
| ------------------------------------------------------------------------------------- | ------------------------- |
| `/v1/auth`, `/v1/users`, `/v1/organizations`, `/v1/spaces`, `/v1/api-keys`, `/v1/sso` | auth-service (8081)       |
| `/v1/rules`, `/v1/folders`, `/v1/tags`                                                | management-service (8082) |
| `/v1/solve`                                                                           | solver-service (8083)     |
| `/v1/audit`, `/v1/webhooks`, `/v1/events`                                             | event-service (8084)      |
| `/v1/analytics`                                                                       | analytics-service (8085)  |
| `/v1/schedules`, `/v1/batch`                                                          | scheduler-service (8086)  |
| `/v1/connectors`, `/v1/connections`                                                   | connector-service (8087)  |
| `/v1/ai`                                                                              | AI service (8000)         |

## Key Features

- **Decision Tables** — Spreadsheet-style rule authoring with 7 condition index types (HashMap, IntervalTree, SortedArray, Trie, Bitmap, and more).
- **Decision Trees** — Visual tree editor with multi-branch conditions and BDD compilation.
- **Scorecards** — Weighted scoring with configurable aggregation strategies (additive, weighted average, multiplicative).
- **Ensembles** — Combine outputs from multiple rules via voting, averaging, or selection strategies.
- **DMN 1.4** — OMG Decision Model and Notation with S-FEEL expressions and decision chaining.
- **Rule Flows** — DAG orchestrator with 6 node types (Condition, Action, Gate, Fork, Join, Sub-Rule).
- **Scripting Rules** — GraalVM-sandboxed JavaScript execution with resource limits. Runs on a standard JDK 21 (GraalVM JDK not required — polyglot runtime is bundled); the bundled runtime does not work on JDK 22+.
- **Complex Event Processing** — Pattern detection (Sequence, Aggregation, Frequency) with in-memory evaluation and optional Kafka Streams streaming mode.
- **RFL Expression Language** — Custom recursive-descent parser with null-safe navigation (`?.`), lambdas, and extensible function/operator registries.
- **Multi-Layer Caching** — L1 Caffeine (per-JVM) → L2 Redis (distributed) → L3 PostgreSQL, with Kafka-driven cache invalidation.
- **Import/Export** — JSON-based single and bulk rule operations for environment promotion and backup.
- **AI Assistant** — LLM-powered rule generation, optimization suggestions, conflict detection, and natural language querying.
- **Approval Workflow** — Request approval, approve, or reject rule versions before publishing (DRAFT → PENDING_APPROVAL → PUBLISHED).
- **A/B Testing** — Experiment framework for comparing rule versions with traffic splitting.
- **Real-Time Notifications** — Server-Sent Events (SSE) for live rule lifecycle updates with in-app notification bell.
- **Analytics Dashboard** — Interactive charts (execution timeline, type distribution, pass/fail ratio, latency percentiles) powered by Recharts.
- **Audit Trail** — Comprehensive event logging with filterable audit log page and resource-level history.
- **Webhook Management** — CRUD for webhooks with event selection, delivery history tracking, and secret management.
- **User Profile** — Profile editing, password change, and session timeout management.
- **Error Handling** — Global error boundary, skeleton loaders, API error toasts, and automatic 503 retry with exponential backoff.
- **Security Hardening** — CSRF protection, rate-limit display, JWT session timeout with expiry warning, and XSS-safe rendering.
- **Performance Optimization** — React.lazy() code-splitting for editors and routes, debounced search, memoized components.
- **Monaco Code Editor** — Syntax-highlighted JavaScript editor with IntelliSense for Script rules (replaces plain textarea).
- **Version Diff Viewer** — Side-by-side JSON comparison of rule versions with color-coded additions, removals, and changes.
- **Keyboard Shortcuts** — Ctrl+S to save, Ctrl+Shift+T to test in the rule editor.
- **Client SDKs** — Java, Node.js, and Python SDKs for rule execution.

## Frontend Pages

| Route                | Page                    | Description                                                         |
| -------------------- | ----------------------- | ------------------------------------------------------------------- |
| `/rules`             | Rule List               | Folder tree, search, type/status filters, pagination, import/export |
| `/rules/:id/edit`    | Rule Editor             | Type-specific editors, versioning, testing, settings, approval      |
| `/analytics`         | Analytics Dashboard     | Metric cards, execution charts, top rules, decision logs            |
| `/analytics/rules/:id` | Rule Stats           | Per-rule analytics detail                                           |
| `/ab-tests`          | A/B Tests               | Create, monitor, and stop A/B test experiments                      |
| `/approvals`         | Approval Queue          | Review and approve/reject pending rule versions                     |
| `/audit`             | Audit Trail             | Filterable event log with expandable changes                        |
| `/webhooks`          | Webhook Management      | CRUD for webhooks, event selection, delivery history                |
| `/tags`              | Tag Management          | Create and manage rule tags                                         |
| `/spaces`            | Space Management        | Organization spaces with create capability                          |
| `/schedules`         | Scheduler               | Batch scheduling with cron expressions                              |
| `/connectors`        | Connectors              | JDBC/HTTP/Redis data enrichment setup                               |
| `/api-keys`          | API Keys                | Manage API keys for programmatic access                             |
| `/settings`          | Organization Settings   | Org name, slug, and configuration                                   |
| `/settings/sso`      | SSO Configuration       | SAML/OIDC provider setup                                            |
| `/settings/members`  | Member Management       | Invite members, manage roles (Owner/Editor/Viewer)                  |
| `/profile`           | User Profile            | Edit name, change password                                          |

## Makefile Reference

| Command               | Description                                                      |
| --------------------- | ---------------------------------------------------------------- |
| `make dev-up`         | Start full local dev stack (build + infra + services + frontend) |
| `make dev-down`       | Stop all local dev services                                      |
| `make dev-up-fast`    | Start local stack without rebuilding                             |
| `make infra-up`       | Start infrastructure containers (PG, Redis, Kafka, ES, MinIO)    |
| `make infra-down`     | Stop infrastructure containers                                   |
| `make build-engine`   | Build all engine modules                                         |
| `make build-platform` | Build all platform services                                      |
| `make test-engine`    | Run engine tests                                                 |
| `make test-platform`  | Run platform tests                                               |
| `make test-frontend`  | Run frontend tests                                               |
| `make migrate`        | Run Flyway database migrations                                   |
| `make stack-up`       | Build and start full Docker Compose stack                        |
| `make stack-down`     | Stop full Docker Compose stack                                   |
| `make dev-frontend`   | Start frontend dev server only                                   |
| `make lint-frontend`  | Run ESLint on frontend                                           |

## Test Accounts

Seed data (V021 migration) provides three test accounts:

| Email                  | Password      | Organization | Role   |
| ---------------------- | ------------- | ------------ | ------ |
| `admin@ruleforge.io`   | `password123` | Acme Corp    | OWNER  |
| `editor@ruleforge.io`  | `password123` | Acme Corp    | EDITOR |
| `viewer@ruleforge.io`  | `password123` | Test Org     | VIEWER |

Sample rules for all 8 rule types are seeded via V022. Comprehensive showcase rules with real-world use cases are seeded via V024. Callable action samples are seeded via V025. V026–V028 add the `rule_sample_inputs` table with seed data and RLS policies, enabling per-rule sample input CRUD (create, edit, delete) from the TestBench UI.

## Smoke Tests

Verify the full stack is working:

```bash
./scripts/smoke-test.sh              # Full test suite (health + API flows)
./scripts/smoke-test.sh --quick      # Health checks only
./scripts/smoke-test.sh --verbose    # Show response bodies
```

The smoke test suite validates: infrastructure (5 services), platform services (7 services), frontend, user registration/login, rule CRUD, and rule execution for all 8 rule types.

Expected result on a fresh local stack (Spring Boot 4.1, JDK 21): **117 pass, 1 fail, 7 skip**. The one failure is the known rule-creation issue below; most skips depend on it.

## Known Issues

These pre-date the Spring Boot 4 upgrade and are not yet fixed:

| Issue | Impact | Cause |
| --- | --- | --- |
| Creating a rule via `POST /v1/spaces/{spaceId}/rules` returns 500 | Rules cannot be created through the API or UI | Migration V007 defines `rules.created_by REFERENCES members(id)`, but `RuleService` stores the user ID, which differs from the member ID |
| Domain events are never published to Kafka | Audit trail, decision analytics and solver cache invalidation receive no events | `KafkaEventPublisher` is a scanned `@Component` guarded by `@ConditionalOnBean(KafkaTemplate.class)`, which is evaluated before Kafka auto-configuration runs, so `NoOpEventPublisher` is always used |
| Showcase seed rules (V024, V025) can be missing on a brand-new database | Some smoke tests return 404 | Not yet identified — the files apply cleanly when replayed with `psql` after first boot |
| MinIO image cannot be pulled | `docker compose up` for the full infra stack fails | `minio/minio:latest` pull is denied; no service uses MinIO |

## Documentation

- [Getting Started](docs/getting-started.md) — Local development setup walkthrough
- [Rule Types Guide](docs/rule-types-guide.md) — All 8 rule types with definition formats, examples, and testing
- [Authentication Guide](docs/auth-guide.md) — JWT, API keys, SSO setup
- [API Reference](docs/api-reference.md) — All REST endpoints
- [AI Assistant Guide](docs/ai-assistant-guide.md) — AI-powered rule generation and optimization
- [Architecture Summary](docs/architecture/ARCHITECTURE-SUMMARY.md) — System design overview
- [Full Architecture](docs/architecture/RuleForge-Architecture-v2.md) — Detailed design document
- [Engine Architecture](docs/architecture/RuleForge-Engine-Core-Architecture-v2.1.md) — Engine internals
- [Codebase Map](docs/architecture/codebase-map/README.md) — Functional summary and interactive dependency diagrams (snapshot before the Spring Boot 4 upgrade)

## Development Roadmap

| Phase                              | Status | Scope                                                                                                          |
| ---------------------------------- | ------ | -------------------------------------------------------------------------------------------------------------- |
| **Phase 1 — MVP**                  | Done   | Core engine (tables, trees), Auth, Management, Solver, Event, basic frontend                                   |
| **Phase 2 — Platform**             | Done   | Scripting (GraalVM), Flows (DAG), Connectors, Scheduler, SSO config, Import/Export                             |
| **Phase 3 — Enterprise**           | Done   | Analytics pipeline, decision dashboards, Scorecard engine, Ensemble engine, SDKs, A/B testing                  |
| **Phase 4 — Advanced**             | Done   | AI Assistant, DMN 1.4, CEP (Kafka Streams), Hierarchy engine, Embedded JAR, startup scripts                    |
| **Phase 5 — Quality**              | Done   | Test coverage, completeness, local boot fixes, smoke tests, seed data                                          |
| **Phase 6 — UX Polish**            | Done   | Monaco editor, analytics charts, FlowEditor modals, missing pages (audit, webhooks, tags, spaces, SSO, members), import/export wiring, version diff viewer, keyboard shortcuts |
| **Phase 7 — Production Readiness** | Done   | Approval workflow, real-time notifications (SSE), user profile, error boundary & skeletons, security hardening (CSRF, session timeout), performance optimization (code-splitting, debounce, memoization) |
| **Platform upgrade**               | Done   | Spring Boot 3.3 → 4.1.1 (Spring Framework 7, Jackson 3), Elasticsearch 9.4.5, Gradle 8.14.6                   |

## License

Proprietary. All rights reserved.
