# RuleForge Codebase Map

Snapshot of `main` at commit `d10575e` (2026-10-20), built from the GitNexus index
(10,550 symbols · 24,345 relationships · 465 clusters · 300 execution flows) and a
manual read of build files and config. Where this disagrees with `CLAUDE.md`, this
reflects the code.

## Diagrams

Open in a browser. Both have guided views, search, focus and export built in.

| File | What it shows |
| --- | --- |
| [`code-dependencies.html`](code-dependencies.html) | Compile-time import graph between Gradle modules, with import counts per edge |
| [`runtime-interactions.html`](runtime-interactions.html) | How services talk at runtime: HTTP, Kafka topics, data stores |

Sources are the `*.architecture.json` files next to them. To regenerate, use the
`archify` skill (`archify deliver architecture <spec> <html> --quality showcase`).

## What RuleForge is

A multi-tenant business-rules management system (BRMS). Authors build rules in a
web UI, management-service versions and approves them, and solver-service runs them
on a custom embeddable rule engine. Every execution is audited, and decisions flow
into analytics.

**Nine rule types**, each a separate engine module: decision table, decision tree,
scorecard, ensemble, flow (DAG), DMN 1.4/FEEL, sandboxed JavaScript (GraalVM),
CEP (Kafka Streams), and hierarchy.

## Codebase at a glance

| Area | Stack | Size (main code) | Test files |
| --- | --- | --- | --- |
| `engine/` | Java 21, Gradle, 12 modules | 186 files · 17.5k LOC | 7 |
| `platform/` | Spring Boot 3.3, 7 services + `ruleforge-common` | 346 files · 23.4k LOC | 22 |
| `frontend/` | React 18, Vite, Zustand, TanStack Query, React Flow, Monaco | 186 files · 33.6k LOC | 14 |
| `ai-service/` | FastAPI, Anthropic SDK | 14 files · 0.8k LOC | — |
| `sdks/` | Java, Node, Python clients | 19 files · 2.0k LOC | — |
| `db/migrations/` | Flyway SQL, V001–V052 (50 files, V018–V019 missing) | 4.2k LOC | — |

## Functional summary by component

### Engine (`engine/`)

- **`ruleforge-core`** (5.7k LOC) has no dependencies beyond slf4j and Jackson, so it
  can be embedded.
  - Entry point: `RuleForgeEngine` / `RuleForgeEngineBuilder` → `DefaultRuleForgeEngine`.
  - Execution: `WorkingMemory`, an `Agenda` that ranks rules by salience, specificity,
    weight, recency and order, and `ForwardChainingController` (cycle detection,
    100-iteration cap).
  - **RFL expression language:** `RflLexer` → `RflParser` → `ExpressionEvaluator`.
  - **SPI extension points:** `RuleTypeEngine`, operators, functions, conflict
    resolution, data enrichers, lifecycle listeners, action executors.
- **Rule-type modules** each depend only on core. Table (1.9k LOC, indexed conditions)
  and DMN (2.2k LOC) are the largest.
- **Packaging:**
  - `ruleforge-spring-boot-starter` wires in all modules.
  - `ruleforge-all` is a shadow JAR bundling core, table, tree, scorecard and ensemble.

### Platform services (`platform/`)

| Service | Port | Responsibility | Key infra |
| --- | --- | --- | --- |
| auth | 8081 | Login, JWT, API keys, RBAC, orgs/spaces/members, SSO config | PostgreSQL |
| management | 8082 | Rule CRUD, versions, folders, tags, approvals, sample inputs, import/export | PostgreSQL, Redis, Kafka (produce) |
| solver | 8083 | Rule execution, L1 Caffeine → L2 Redis → PostgreSQL compiled-rule cache | Redis, Kafka (both) |
| event | 8084 | Audit trail, webhooks with outbox and OAuth2 client credentials | PostgreSQL, Kafka (consume), Redis |
| analytics | 8085 | Decision logs and aggregations | Elasticsearch, Kafka (consume) |
| scheduler | 8086 | Quartz batch schedules, workflows, holiday calendars | PostgreSQL, Quartz |
| connector | 8087 | JDBC/HTTP/Redis connection configs and data enrichment | outbound only |

**`ruleforge-common`** is the shared kernel. It holds `TenantContext`, the JWT and
API-key filters, `GlobalExceptionHandler`, and the `EventPublisher` abstraction
(Kafka, or a no-op when Kafka isn't running).

### Runtime interactions

| Path | Mechanism |
| --- | --- |
| Web UI → every service | Vite proxy (local) or Kong (Docker) |
| management → solver | Kafka `rule.lifecycle`, used to invalidate the solver cache (also consumed by event) |
| solver → event, analytics | Kafka `rule.execution` |
| solver → auth | HTTP `POST /internal/api-keys/validate` |
| scheduler → solver | HTTP batch solve |
| ai-service → management | HTTP `GET /v1/rules` (no auth header forwarded) |

### Frontend (`frontend/`)

- About 25 routes, covering:
  - rules list and editor
  - spaces, approvals, A/B tests and analytics
  - API keys, audit, connectors, schedules and workflows
  - webhooks, tags, profile and settings (members, SSO)
- One editor per rule type, plus a test bench and an AI assistant panel.
- 15 Zustand stores and 18 typed API modules over axios.

### AI service (`ai-service/`)

- Endpoints under `/v1/ai/`: `generate-rule`, `explain-rule`, `optimize-rule`,
  `generate-test-data`, `detect-conflicts` and `query`.
- Calls Anthropic through `AsyncAnthropic`. The model comes from `ANTHROPIC_MODEL`.

## Dependency health

**What's working:**

- The engine has no dependency on the platform. GitNexus reports two
  `engine → auth-service` imports, but they're false positives: it resolved
  `import java.util.*` to `SlugUtils`.
- Services never import each other. They only couple through HTTP and Kafka.
- Only management and solver depend on the engine, and both register all 9 engines
  in their `EngineConfig`.

**Coupling hot spots:**

- management-service is the largest service (6.2k LOC) and the heaviest
  `ruleforge-common` consumer (62 imports). It also produces the topic that drives
  solver cache invalidation.
- solver depends on auth at request time (API-key validation over HTTP). If auth is
  down, solve calls authenticated by API key fail.

## Findings for engineering leadership

Ranked by likely impact.

1. **Kong config doesn't match the services.** Several UI features work locally
   through the Vite proxy but will break behind Kong:
   - No Kong route reaches event-service (`/v1/audit`, `/v1/events`).
   - `/v1/webhooks` is routed to management, but webhooks live in event-service.
   - `/v1/approval-requests`, `/v1/workflows` and `/v1/holiday-calendars` have no route.
2. **Schema is managed by Hibernate `ddl-auto: update`, not Flyway, in every
   service.** event-service also ships its own `V013`/`V014` migrations, which clash
   with the root `db/migrations` versions. Turning Flyway on would fail as things stand.
3. **Test coverage is thin for a "Phase 5: tests DONE" claim.**
   - 7 engine test files; table, tree, DMN, CEP, script, scorecard and hierarchy
     have none.
   - analytics and scheduler have no tests.
4. **Stubs that look finished in the UI:**
   - SSO SAML and OIDC handlers are placeholders.
   - `RedisAuditEventStore` is three TODOs.
   - Ensemble returns a stub result when no `SubRuleInvoker` is wired.
5. **Packaging gaps for embedding:**
   - `ruleforge-all` omits flow, script, DMN, CEP and hierarchy.
   - The Spring Boot starter has no `AutoConfiguration.imports`, so Boot 3 apps won't
     auto-configure it.
6. **Infrastructure that is declared but unused:**
   - MinIO/S3 has no code using it.
   - Kafka `system.events` is unused, and `user.activity` has no producer.
7. **`CLAUDE.md` drift:**
   - It says migrations run V001–V028; they actually run to V052.
   - It mentions Helm charts, but none exist.
   - It says 11 engine modules; there are 12.
   - It says TestBench uses `react-quill-new`, which isn't in `package.json`.
8. **Repo hygiene:** `nohup.out` is committed at the root.

## GitNexus index

The index is enabled for this repo (stored in `.gitnexus/`, which git ignores). It
was built with `gitnexus analyze --skip-agents-md --skip-skills`, so `CLAUDE.md` was
not modified. The GitNexus MCP tools (`query`, `context`, `impact`, `cypher`) now work
against it. Refresh it after significant changes with:

```bash
gitnexus analyze --skip-agents-md --skip-skills
```
