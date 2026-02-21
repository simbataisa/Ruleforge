# RuleForge Architecture Summary

A concise architectural overview for developers. See linked documents at the bottom for full details.

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        RuleForge System                                 │
│                                                                         │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────────────┐  │
│  │   Frontend    │───▶│  Kong Gateway    │───▶│  Platform Services    │  │
│  │  React 18     │    │  Port 8000       │    │  Ports 8081-8087      │  │
│  │  Port 3000    │    │  CORS, routing   │    │                       │  │
│  └──────────────┘    └──────────────────┘    │  auth-service    8081 │  │
│                                               │  management      8082 │  │
│  ┌──────────────┐                             │  solver-service  8083 │  │
│  │  AI Service   │◀──── Kong /v1/ai ─────────│  event-service   8084 │  │
│  │  FastAPI      │                            │  analytics       8085 │  │
│  │  Port 8000    │                            │  scheduler       8086 │  │
│  └──────────────┘                             │  connector       8087 │  │
│                                               └───────┬───────────────┘  │
│                                                       │                  │
│  ┌────────────────────────────────────────────────────┼────────────────┐ │
│  │                     Data Stores                    │                │ │
│  │  PostgreSQL 16 (5432)  Redis 7 (6379)  Kafka 3.7.1 (9092)         │ │
│  │  Elasticsearch 8 (9200)  MinIO (9000)                              │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

## Engine Architecture

11 Gradle modules built with Java 21. The `ruleforge-core` module has zero external dependencies (SLF4J API + Jackson only), making it embeddable in any JVM application.

### Module Dependency Graph

```
ruleforge-core  (zero deps — standalone, embeddable)
  ├── ruleforge-table       Decision Tables (indexed conditions)
  ├── ruleforge-tree        Decision Trees (BDD compilation)
  ├── ruleforge-script      GraalVM sandboxed JavaScript
  ├── ruleforge-flow        DAG orchestrator (runtime SPI for engines)
  ├── ruleforge-scorecard   Weighted scoring [Phase 3 stub]
  ├── ruleforge-ensemble    Voting/averaging [Phase 3 stub]
  ├── ruleforge-cep         Kafka Streams CEP [Phase 4 stub]
  ├── ruleforge-dmn         DMN 1.4 / FEEL [Phase 4 stub]
  └── ruleforge-hierarchy   Hierarchical resolution [Phase 4 stub]

ruleforge-spring-boot-starter  (depends on all modules + Spring Boot)
```

### Core Design Principles

- **Multi-dimensional priority vectors**: `(salience, specificity, weight, recency, order)` — conflict resolution considers all five dimensions in order.
- **7 execution modes**: FIRST_MATCH, ALL_MATCH, BEST_MATCH, SEQUENTIAL, PARALLEL, FORWARD_CHAIN, CASCADING.
- **Condition indexing**: HashMap (EQ/IN), IntervalTree (BETWEEN), SortedArray (GT/LT), Trie (CONTAINS), Bitmap (IS_NULL) — indexed lookup instead of linear scans.
- **Custom expression language "RFL"**: Hand-written recursive descent parser (no ANTLR), null-safe navigation (`?.`), lambda expressions.
- **Forward chaining**: Iterative re-evaluation with cycle detection (max 100 iterations).
- **10 SPI extension points**: OperatorExtension, FunctionExtension, ConflictResolutionStrategy, DataEnricherExtension, RuleLifecycleListener, and 5 more — extend without modifying core.

### Rule Types

| Type | Module | Status |
|------|--------|--------|
| Decision Tables | ruleforge-table | Complete |
| Decision Trees | ruleforge-tree | Complete |
| GraalVM Scripts | ruleforge-script | Complete |
| DAG Flows | ruleforge-flow | Complete |
| Scorecards | ruleforge-scorecard | Phase 3 stub |
| Ensemble | ruleforge-ensemble | Phase 3 stub |
| CEP | ruleforge-cep | Phase 4 stub |
| DMN 1.4 / FEEL | ruleforge-dmn | Phase 4 stub |

## Platform Architecture

8 Spring Boot 3.3.5 microservices plus a shared `ruleforge-common` module. Composite Gradle build includes the engine via `includeBuild("../engine")`.

### Service Responsibilities

| Service | Port | Purpose |
|---------|------|---------|
| auth-service | 8081 | JWT auth, API keys, RBAC, SSO config, user/org/space management |
| management-service | 8082 | Rule CRUD, versioning, folders, tags, import/export |
| solver-service | 8083 | Rule compilation + execution, L1 Caffeine -> L2 Redis -> L3 PostgreSQL caching |
| event-service | 8084 | Kafka consumers for audit trail, webhooks |
| analytics-service | 8085 | Decision log aggregation via Elasticsearch (Phase 3) |
| scheduler-service | 8086 | Quartz-based batch scheduling |
| connector-service | 8087 | JDBC/HTTP/Redis data enrichment |

### Shared Module: ruleforge-common

JWT token provider, JWT/API key authentication filters, TenantContext (ThreadLocal), security constants, ErrorResponse/PageResponse DTOs, Kafka event schemas.

## Data Architecture

- **PostgreSQL 16**: Primary operational database. JSONB for rule definitions, rule versions. 20 Flyway migrations (V001-V020).
- **Row-Level Security**: All tenant tables have RLS policies. Session variable `app.current_org_id` enforces organization isolation.
- **Redis 7**: L2 distributed cache for compiled rules. Key pattern: `tenant:{orgId}:rule:{ruleId}:{version}:compiled`. 256MB max, allkeys-lfu eviction.
- **Kafka 3.7.1**: Event streaming in KRaft mode (no Zookeeper). Topics: `rule.lifecycle`, `rule.execution`, `user.activity`, `system.events`.
- **Elasticsearch 8**: Decision log indexing and aggregation (Phase 3).
- **MinIO**: S3-compatible object storage for rule exports and batch results.

## Security Architecture

- **JWT**: JJWT library, HMAC-SHA512. Access tokens (1hr) + refresh tokens (7d).
- **API Keys**: SOLVER and MANAGEMENT types, SHA-256 hashed storage, prefix-based lookup.
- **SSO**: Config CRUD for SAML 2.0 and OIDC providers (callback stubs, full implementation planned).
- **RBAC**: 5 roles — OWNER, ADMIN, EDITOR, VIEWER, SOLVER.
- **Multi-tenancy**: Organization -> Space -> Folder -> Rule hierarchy. PostgreSQL RLS enforced at the database level.
- **Filter chain**: JwtAuthenticationFilter -> ApiKeyAuthenticationFilter. TenantContext cleared after each request.

## Infrastructure

- **Docker Compose**: `infra/docker/docker-compose.infra.yml` (infrastructure only), `docker-compose.yml` (full stack).
- **Kong 3.6**: API gateway with declarative config. CORS plugin, 10MB request size limit.
- **Port assignments**: 3000 (frontend), 5432 (PostgreSQL), 6379 (Redis), 8000 (Kong), 8081-8087 (services), 9000 (MinIO), 9092 (Kafka), 9200 (Elasticsearch).

## Implementation Status

| Component | Status | Phase |
|-----------|--------|-------|
| ruleforge-core | Complete | 1 |
| ruleforge-table | Complete | 1 |
| ruleforge-tree | Complete | 1 |
| ruleforge-script | Complete | 2 |
| ruleforge-flow | Complete | 2 |
| ruleforge-scorecard | Stub | 3 |
| ruleforge-ensemble | Stub | 3 |
| ruleforge-cep | Stub | 4 |
| ruleforge-dmn | Stub | 4 |
| ruleforge-hierarchy | Stub | 4 |
| ruleforge-spring-boot-starter | Complete | 2 |
| auth-service | Complete | 1 |
| management-service | Complete | 1+2 |
| solver-service | Complete | 1 |
| event-service | Complete | 1 |
| analytics-service | Stub | 3 |
| scheduler-service | Complete | 2 |
| connector-service | Complete | 2 |
| Frontend | Complete | 1+2 |
| DB migrations (V001-V020) | Complete | 1+2 |

## For More Detail

- [Full Architecture](RuleForge-Architecture-v2.md) -- Comprehensive enterprise architecture (1,660 lines)
- [Engine Architecture](RuleForge-Engine-Core-Architecture-v2.1.md) -- Deep engine internals (1,846 lines)
