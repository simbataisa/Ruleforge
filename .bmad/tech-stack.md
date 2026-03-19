# Confirmed Tech Stack: RuleForge

> Populated from architecture documentation, README, and CLAUDE.md.
> Once a decision is recorded here, agents do NOT re-open it.

## Decision Status Legend
- ✅ Confirmed — do not re-discuss
- 🔄 Under review — architect still evaluating
- ❌ Rejected — do not suggest

---

## Application Layer (Solution Architect)

### Backend — Engine (`engine/`, Gradle, Java 21)

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Primary language | Java 21 | ✅ | Records, sealed classes, text blocks; GraalVM requires 21+ |
| Build tool | Gradle with Kotlin DSL (`build.gradle.kts`) | ✅ | Multi-module support; composite build with platform |
| Core dependencies | SLF4J API + Jackson only (ruleforge-core) | ✅ | Zero-dep embeddable requirement; must run in any Java app |
| Expression language | Hand-written recursive-descent RFL parser | ✅ | Zero-dep requirement — no ANTLR; null-safe nav (`?.`), lambdas |
| Scripting engine | GraalVM polyglot runtime (bundled) | ✅ | Sandboxed JavaScript execution; works on any JDK 21+ (no GraalVM JDK needed) |
| Streaming CEP | Kafka Streams | ✅ | Pattern detection over event streams; optional (not registered in all services) |
| Tree compilation | BDD (Binary Decision Diagram) | ✅ | Optimal for condition matching in Decision Tree engine |
| Priority model | Multi-dimensional PriorityVector (salience, specificity, weight, recency, order) | ✅ | Required for enterprise rule conflict resolution |
| Execution modes | 7 modes: FIRST_MATCH, ALL_MATCH, BEST_MATCH, SEQUENTIAL, PARALLEL, FORWARD_CHAIN, CASCADING | ✅ | Covers all enterprise BRMS use cases |
| Forward chaining | Max 100 iterations with cycle detection | ✅ | Prevents infinite loops in cascading rules |

### Backend — Platform (`platform/`, Spring Boot 3.3.5)

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Framework | Spring Boot 3.3.5 | ✅ | Industry standard for Java microservices; actuator, security, JPA included |
| Dependency management | Spring Dependency Management Plugin 1.1.6 | ✅ | Hardcoded in `plugins {}` block — do not use `property()` there |
| Build | Gradle (composite with engine via `includeBuild("../engine")`) | ✅ | Allows engine changes to be immediately picked up by platform builds |
| API style | REST (`/v1/...`) | ✅ | Standard HTTP REST with conventional commit-style versioning |
| Auth | JWT (HS512, 64+ byte secret) + API keys | ✅ | Stateless, multi-tenant; `JwtAuthenticationFilter` in ruleforge-common |
| Multi-tenancy | PostgreSQL RLS via `app.current_org_id` session variable | ✅ | Row-level isolation without application-layer filtering |
| Security framework | Spring Security 6 | ✅ | Integrated with JWT; custom `AuthenticationEntryPoint` returns 401 (not 403) |
| Event streaming | Apache Kafka 3.7.1 | ✅ | Rule lifecycle, execution, activity, system events |
| Cache L1 | Caffeine (per-JVM) | ✅ | Fastest; evicted on Kafka rule lifecycle events |
| Cache L2 | Redis 7 (distributed) | ✅ | Shared across JVM instances; key: `tenant:{orgId}:rule:{id}:{ver}:compiled` |
| Cache L3 | PostgreSQL (source of truth) | ✅ | Always accurate; consulted on L1+L2 miss |
| Scheduling | Quartz | ✅ | Robust cron-based batch execution in scheduler-service |
| Webhook reliability | Outbox pattern + at-least-once delivery | ✅ | Database outbox prevents event loss; retry with exponential backoff |
| SSRF protection | `SsrfGuard` utility class | ✅ | Blocks webhook delivery to private/localhost IPs |
| Object storage | S3 / MinIO | ✅ | Rule exports, batch results |

### Frontend (`frontend/`, port 3000)

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Framework | React 18 | ✅ | Concurrent mode, Suspense, Server Components ready |
| Language | TypeScript 5.5 | ✅ | Type safety; strict mode enabled |
| Build tool | Vite 5.4 | ✅ | Fast HMR; code-splitting via `React.lazy()` |
| Component library | shadcn/ui + Tailwind CSS 3.4 | ✅ | Accessible, composable, no lock-in |
| Icons | Lucide React | ✅ | Consistent icon set used throughout |
| State management | Zustand (client state) + React Query (server state) | ✅ | No Redux; React Query handles caching/invalidation |
| Rule editors | React Flow (tree/flow), Monaco Editor (scripts), spreadsheet (tables) | ✅ | Best-in-class for each rule type |
| Charts | Recharts | ✅ | Analytics dashboard charts |
| Test framework | Vitest + React Testing Library | ✅ | Vite-native; fast; co-located test files |
| Linter | ESLint | ✅ | Run: `npm run lint` |
| Local dev proxy | Vite proxy in `vite.config.ts` | ✅ | Routes `/v1/*` directly to service ports (no Kong needed) |

### Mobile

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Approach | Web-only (no mobile app) | ✅ | Enterprise BRMS — browser-based authoring tools require desktop |

### Data

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Primary database | PostgreSQL 16 (port 5432) | ✅ | JSONB for rule definitions; RLS for multi-tenancy |
| Cache | Redis 7 (port 6379) | ✅ | L2 distributed rule cache; also used for session data |
| Search / Analytics | Elasticsearch 8 (port 9200) | ✅ | Decision log aggregation; analytics queries |
| Message queue | Apache Kafka 3.7.1 (port 9092) | ✅ | Topics: rule.lifecycle, rule.execution, user.activity, system.events |
| Object storage | MinIO (ports 9000/9001) | ✅ | S3-compatible; rule exports, batch results |
| Migrations | Flyway | ✅ | V{N}__{description}.sql in `db/migrations/`; local dev uses ddl-auto:update |

### Integration

| Category | Decision | Status | Rationale |
|---|---|---|---|
| API gateway | Kong 3.6 (Docker/prod only) | ✅ | CORS, rate limiting, routing; not used in local dev |
| Auth / identity | Internal JWT + optional SAML/OIDC SSO | ✅ | auth-service owns JWT issuance; SSO config stored per org |
| AI / LLM | Anthropic SDK (Python FastAPI) | ✅ | Rule generation, optimization suggestions, NL querying |
| Data enrichment | JDBC / HTTP / Redis connectors | ✅ | connector-service handles credential encryption + data fetch |

---

## Platform Layer (Enterprise Architect)

### Infrastructure

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Cloud provider | Cloud-agnostic (Docker + Kubernetes) | ✅ | Helm charts in `infra/`; runs on any cloud or on-prem |
| Container orchestration | Kubernetes (Helm charts) | ✅ | Production deployment target; Docker Compose for local dev |
| Local dev | Docker Compose (infra containers only) | ✅ | Services run as JARs locally for faster iteration |
| CI/CD platform | 🔄 Not yet specified | 🔄 | No CI pipeline configured in repo |
| Service startup | `scripts/start-local.sh` orchestration | ✅ | Sequential startup: auth first, then management/event/connector/solver, then analytics, then scheduler |

### Observability

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Health checks | Spring Actuator `/actuator/health` on all services | ✅ | Used by startup script for readiness verification |
| Logging | Per-service log files in `logs/` | ✅ | `logs/ruleforge-<service>.log`; tail with `tail -f` |
| Metrics | 🔄 Not yet specified | 🔄 | Actuator endpoints available; no Prometheus/Grafana configured |
| Tracing | 🔄 Not yet specified | 🔄 | Distributed tracing not yet configured |
| Audit trail | event-service Kafka consumers → PostgreSQL | ✅ | All rule lifecycle events logged with full mutation diff |

### Data Platform

| Category | Decision | Status | Rationale |
|---|---|---|---|
| Analytics pipeline | Kafka → event-service → Elasticsearch | ✅ | Decision logs indexed in Elasticsearch for aggregation |
| BI / visualisation | Recharts in frontend analytics dashboard | ✅ | In-app charts: execution timeline, type distribution, pass/fail ratio, latency percentiles |

---

## Rejected Technologies

| Technology | Category | Rejected by | Reason |
|---|---|---|---|
| ANTLR | Expression parsing | Solution Architect | Adds external dependency; violates ruleforge-core zero-dep requirement |
| Denormalized rule_count column | Data modeling | Solution Architect (ADR-002) | Counter drift risk; synchronization complexity across services |
| `GET /rules?spaceId=...` query param | API design | Solution Architect (ADR-003) | Conflates org-wide and space-scoped endpoints; REST hierarchy violated |
| `PUT /rules/{id}` for space moves | API design | Solution Architect (ADR-004) | Conflates metadata update with ownership reassignment; breaks audit semantics |
| Compute-on-the-fly slug | Data modeling | Solution Architect (ADR-001) | Unstable URLs; no uniqueness enforcement possible without DB storage |
| `space_type` DB column | Data modeling | Solution Architect (ADR-005) | Over-engineered for name-based sandbox detection; requires migration + DTO change |
| GraalVM JDK (required) | Runtime | Solution Architect | GraalVM polyglot bundled in ruleforge-script — works on any JDK 21+ (Temurin, Corretto, etc.) |
| Redux | State management | Frontend Engineer | Zustand + React Query covers all needs with less boilerplate |
| Cross-service REST for ruleCount | Data access | Solution Architect (ADR-002) | N+1 HTTP calls to management-service; tight coupling; latency |

## Architecture Decision Records

All ADRs are in `docs/architecture/adr/`. Current index:

| ADR | Decision | Date | Status |
|---|---|---|---|
| ADR-001 | Slug Generation Strategy for Spaces — store in DB at write time | 2026-03-14 | Accepted |
| ADR-002 | ruleCount Computation — real-time SQL COUNT from shared DB | 2026-03-14 | Accepted |
| ADR-003 | Space-Scoped Rule API — use `/spaces/{id}/rules`, not `/rules?spaceId=` | 2026-03-14 | Accepted |
| ADR-004 | Move Rule Implementation — dedicated `PATCH /rules/{id}/move` endpoint | 2026-03-14 | Accepted |
| ADR-005 | Sandbox Space Detection — by `space.name === "Sandbox"` | 2026-03-15 | Accepted |
| ADR-006 | Assign-vs-Create Separation — remove rule creation from space context | 2026-03-15 | Accepted |
