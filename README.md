# RuleForge

**Enterprise Business Rules Management System**

---

## What is RuleForge?

RuleForge is an enterprise BRMS for authoring, managing, and executing business rules at scale. It combines an embeddable Java rule engine with a microservices platform and a modern React-based UI.

- **Embeddable Engine** -- Zero external dependencies in the core module (SLF4J + Jackson only), suitable for embedding directly into Java applications.
- **Visual Rule Editors** -- Spreadsheet-style decision tables, drag-and-drop tree editors, DAG flow designers, and sandboxed script editors.
- **Multi-Tenant Platform** -- Organization, Space, Folder, and Rule hierarchy with PostgreSQL Row-Level Security, JWT authentication, API keys, and SSO support.
- **Production-Ready** -- Multi-layer caching, Kafka-based event streaming, audit trails, batch scheduling, and data connectors.

**Use cases:** Financial services (loan decisioning, risk scoring), insurance (underwriting, claims automation), healthcare (eligibility determination), e-commerce (dynamic pricing, promotions).

## Key Features

- **Decision Tables** -- Spreadsheet-style rule authoring with 7 specialized condition index types (HashMap, IntervalTree, SortedArray, Trie, Bitmap, and more) for high-performance evaluation.
- **Decision Trees** -- Visual tree editor with multi-branch conditions and BDD compilation.
- **Scripting Rules** -- GraalVM-sandboxed JavaScript execution with resource limits and security constraints.
- **Rule Flows** -- DAG orchestrator with a visual flow editor supporting 6 node types for complex rule pipelines.
- **RFL Expression Language** -- Custom recursive-descent parser with null-safe navigation (`?.`), lambdas, and extensible function/operator registries.
- **Multi-Tenant Architecture** -- Organization > Space > Folder > Rule hierarchy enforced at the database level via PostgreSQL RLS.
- **Multi-Layer Caching** -- L1 Caffeine (per-JVM) to L2 Redis (distributed) to L3 PostgreSQL, with Kafka-driven cache invalidation.
- **Event-Driven** -- Kafka-based audit trails, real-time cache invalidation, and configurable webhooks.
- **Import/Export** -- JSON-based single and bulk rule operations for environment promotion and backup.
- **Batch Scheduling** -- Quartz-based scheduler with configurable cron expressions, calling the solver service for bulk execution.
- **Data Connectors** -- JDBC, HTTP, and Redis enrichment sources with AES-256 encrypted credential storage.
- **Authentication** -- JWT tokens, API keys (SOLVER and MANAGEMENT types), and SSO configuration for SAML and OIDC providers.

## Architecture Overview

```
┌───────────────┐     ┌──────────────┐     ┌────────────────────────────┐
│   Frontend    │────>│ Kong Gateway │────>│     Platform Services      │
│  React 18     │     │  (port 8000) │     │                            │
│  port 3000    │     └──────────────┘     │  auth-service       (8081) │
└───────────────┘                          │  management-service (8082) │
                                           │  solver-service     (8083) │
                                           │  event-service      (8084) │
                                           │  analytics-service  (8085) │
                                           │  scheduler-service  (8086) │
                                           │  connector-service  (8087) │
                                           └─────────┬──────────────────┘
                                                     │
                               ┌─────────────────────┼─────────────────────┐
                               v                     v                     v
                        ┌────────────┐     ┌──────────────┐    ┌──────────────┐
                        │ PostgreSQL │     │   Redis 7    │    │  Kafka 3.7   │
                        │  16 + RLS  │     │   (cache)    │    │  (events)    │
                        └────────────┘     └──────────────┘    └──────────────┘
```

## Project Structure

```
ruleforge/
├── engine/                              # Embeddable rule engine (Gradle, Java 21)
│   ├── ruleforge-core/                  # Core: model, memory, SPI, RFL parser, engine API
│   ├── ruleforge-table/                 # Decision Table engine (7 index types)
│   ├── ruleforge-tree/                  # Decision Tree engine (BDD compilation)
│   ├── ruleforge-script/                # GraalVM sandboxed scripting
│   ├── ruleforge-flow/                  # DAG flow orchestrator
│   ├── ruleforge-scorecard/             # Weighted scoring (Phase 3)
│   ├── ruleforge-ensemble/              # Voting/averaging (Phase 3)
│   ├── ruleforge-cep/                   # Kafka Streams CEP (Phase 4)
│   ├── ruleforge-dmn/                   # DMN 1.4 / FEEL (Phase 4)
│   ├── ruleforge-hierarchy/             # Hierarchical resolution (Phase 4)
│   └── ruleforge-spring-boot-starter/   # Auto-configuration for Spring Boot apps
├── platform/                            # Microservices (Spring Boot 3.3)
│   ├── ruleforge-common/                # Shared: JWT, security filters, DTOs, Kafka schemas
│   ├── ruleforge-auth-service/          # Auth, users, orgs, spaces, API keys, SSO
│   ├── ruleforge-management-service/    # Rule CRUD, versioning, folders, tags, import/export
│   ├── ruleforge-solver-service/        # Rule execution with multi-layer caching
│   ├── ruleforge-event-service/         # Kafka consumers, audit events, webhooks
│   ├── ruleforge-analytics-service/     # Decision logs, aggregation (Phase 3)
│   ├── ruleforge-scheduler-service/     # Batch scheduling with Quartz
│   └── ruleforge-connector-service/     # DB/HTTP/Redis data enrichment
├── frontend/                            # React 18 + TypeScript + Vite + Tailwind
├── ai-service/                          # AI assistant (Python, FastAPI) (Phase 4)
├── db/migrations/                       # Flyway SQL migrations (V001-V020)
├── infra/                               # Docker Compose, Kong gateway, Helm charts
└── docs/                                # Architecture and developer documentation
```

## Tech Stack

| Layer          | Technology                                                                 |
| -------------- | -------------------------------------------------------------------------- |
| Engine         | Java 21, zero-dep core (SLF4J + Jackson only), GraalVM 23.1.2              |
| Platform       | Spring Boot 3.3.5, PostgreSQL 16, Redis 7, Kafka 3.7.1, Caffeine           |
| Frontend       | React 18, TypeScript 5.5, Vite 5.4, Tailwind CSS 3.4, Zustand, React Query |
| AI             | Python 3.12, FastAPI, Anthropic SDK                                        |
| Infrastructure | Docker, Kong 3.6 API Gateway, Kubernetes (Helm), Flyway                    |

## Quick Start

### Prerequisites

- Java 21+ (with `JAVA_HOME` set)
- Node.js 18+ and npm
- Docker and Docker Compose

### 1. Start Infrastructure

```bash
make infra-up
# Starts: PostgreSQL (5432), Redis (6379), Kafka (9092), Elasticsearch (9200), MinIO (9000)
```

### 2. Run Database Migrations

```bash
make migrate
```

### 3. Build the Engine

```bash
cd engine && ./gradlew build
```

### 4. Build and Run Platform Services

```bash
cd platform && ./gradlew build

# Start services individually:
cd platform && ./gradlew :ruleforge-auth-service:bootRun
cd platform && ./gradlew :ruleforge-management-service:bootRun
cd platform && ./gradlew :ruleforge-solver-service:bootRun
cd platform && ./gradlew :ruleforge-event-service:bootRun
cd platform && ./gradlew :ruleforge-scheduler-service:bootRun
cd platform && ./gradlew :ruleforge-connector-service:bootRun
```

### 5. Start the Frontend

```bash
cd frontend && npm install && npm run dev
# Runs on http://localhost:3000, proxies API calls to Kong (8000)
```

### Full Stack (Docker Compose)

```bash
make stack-up    # Builds and starts everything
make stack-down  # Stops everything
```

## Services

| Service            | Port | Purpose                                                   | Phase |
| ------------------ | ---- | --------------------------------------------------------- | ----- |
| auth-service       | 8081 | JWT auth, API keys, RBAC, SSO config, user/org management | 1     |
| management-service | 8082 | Rule CRUD, versioning, folders, tags, import/export       | 1+2   |
| solver-service     | 8083 | Rule execution with L1/L2/L3 caching                      | 1     |
| event-service      | 8084 | Kafka consumers, audit trail, webhooks                    | 1     |
| analytics-service  | 8085 | Decision logs, Elasticsearch aggregation                  | 3     |
| scheduler-service  | 8086 | Batch scheduling (Quartz), cron-based execution           | 2     |
| connector-service  | 8087 | JDBC/HTTP/Redis data enrichment, credential encryption    | 2     |
| Kong Gateway       | 8000 | API routing, CORS, rate limiting                          | 1     |

## API Gateway Routes

All API traffic is routed through Kong on port 8000.

| Path Prefix                                                                | Backend Service           |
| -------------------------------------------------------------------------- | ------------------------- |
| `/v1/auth`, `/v1/users`, `/v1/organizations`, `/v1/spaces`, `/v1/api-keys` | auth-service (8081)       |
| `/v1/rules`, `/v1/folders`, `/v1/tags`                                     | management-service (8082) |
| `/v1/solve`                                                                | solver-service (8083)     |
| `/v1/analytics`                                                            | analytics-service (8085)  |
| `/v1/ai`                                                                   | ai-service (8000)         |

## Documentation

- [Getting Started](docs/getting-started.md) -- Local development setup
- [Authentication Guide](docs/auth-guide.md) -- JWT, API keys, SSO setup
- [API Reference](docs/api-reference.md) -- All endpoints
- [Architecture Summary](docs/architecture/ARCHITECTURE-SUMMARY.md) -- System design overview
- [Full Architecture](docs/architecture/RuleForge-Architecture-v2.md) -- Detailed design document
- [Engine Architecture](docs/architecture/RuleForge-Engine-Core-Architecture-v2.1.md) -- Engine internals

## Development Roadmap

| Phase                     | Status  | Scope                                                                                         |
| ------------------------- | ------- | --------------------------------------------------------------------------------------------- |
| **Phase 1 -- MVP**        | Done    | Core engine (tables, trees), Auth, Management, Solver, Event, basic frontend                  |
| **Phase 2 -- Platform**   | Done    | Scripting (GraalVM), Flows (DAG), Connectors, Scheduler, SSO config, Import/Export            |
| **Phase 3 -- Enterprise** | Next    | Analytics pipeline, decision dashboards, Scorecard engine, Ensemble engine, SDKs, A/B testing |
| **Phase 4 -- Advanced**   | Planned | AI Assistant, DMN 1.4, CEP (Kafka Streams), Embedded JAR, WASM, GitOps                        |

## License

Proprietary. All rights reserved.
