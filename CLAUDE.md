# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Infrastructure (PostgreSQL, Redis, Kafka, Elasticsearch, MinIO)
make infra-up          # Start infrastructure containers
make infra-down        # Stop infrastructure

# Engine (Gradle multi-module, Java 21)
cd engine && ./gradlew build        # Build all engine modules
cd engine && ./gradlew test         # Run engine tests
cd engine && ./gradlew compileJava  # Compile only (no tests)

# Platform services (Gradle, Spring Boot 3.3)
cd platform && ./gradlew build      # Build all services
cd platform && ./gradlew test       # Run service tests

# Frontend (Vite, React 18, TypeScript)
cd frontend && npm install          # Install dependencies
cd frontend && npm run dev          # Dev server on port 3000
cd frontend && npm test             # Run Vitest tests
cd frontend && npm run lint         # ESLint

# AI service (Python, FastAPI)
cd ai-service && pip install -r requirements.txt
cd ai-service && uvicorn app.main:app --reload --port 8000

# Database migrations
make migrate           # Run Flyway migrations against local PG

# Full stack
make stack-up          # Build and start everything (Docker Compose)
make stack-down        # Stop everything
```

## Project Structure

```
ruleforge/
├── engine/          # Rule engine library (Gradle, Java 21) — standalone, embeddable
├── platform/        # Microservices (Gradle, Spring Boot 3.3)
├── frontend/        # Web UI (Vite, React 18, TypeScript, Tailwind)
├── ai-service/      # AI assistant (Python, FastAPI)
├── db/migrations/   # Flyway SQL migrations (V001-V020)
├── infra/           # Docker Compose, Kong gateway, Helm charts
└── docs/            # Architecture design documents
```

## Architecture Overview

RuleForge is an enterprise BRMS with a custom rule execution engine.

### Engine (`engine/`, Gradle multi-module, `io.ruleforge`)

11 modules — `ruleforge-core` has zero external dependencies (embeddable):

| Module | Purpose | Dependencies |
|--------|---------|-------------|
| `ruleforge-core` | Abstractions, WorkingMemory, RFL parser, SPI | SLF4J API + Jackson only |
| `ruleforge-table` | Decision Table engine (indexed conditions) | core |
| `ruleforge-tree` | Decision Tree engine (BDD compilation) | core |
| `ruleforge-scorecard` | Weighted scoring [Phase 3] | core |
| `ruleforge-flow` | DAG orchestrator [Phase 2] | core (runtime SPI for engines) |
| `ruleforge-script` | GraalVM sandboxed JS [Phase 2] | core + GraalVM SDK |
| `ruleforge-cep` | Kafka Streams CEP [Phase 4] | core + Kafka Streams |
| `ruleforge-dmn` | DMN 1.4 / FEEL [Phase 4] | core |
| `ruleforge-ensemble` | Voting/averaging [Phase 3] | core |
| `ruleforge-hierarchy` | Hierarchical resolution [Phase 4] | core |
| `ruleforge-spring-boot-starter` | Auto-configuration | all modules + Spring Boot |

Key engine design:
- Multi-dimensional priority: `(salience, specificity, weight, recency, order)`
- 7 execution modes: FIRST_MATCH, ALL_MATCH, BEST_MATCH, SEQUENTIAL, PARALLEL, FORWARD_CHAIN, CASCADING
- Condition indexing: HashMap (EQ/IN), IntervalTree (BETWEEN), SortedArray (GT/LT), Trie (CONTAINS), Bitmap (IS_NULL)
- Custom expression language "RFL" with null-safe navigation, lambdas
- Forward chaining with cycle detection (max 100 iterations)
- SPI extension points: OperatorExtension, FunctionExtension, ConflictResolutionStrategy, DataEnricherExtension, RuleLifecycleListener

### Platform Services (`platform/`, ports 8081-8087)

| Service | Port | Purpose |
|---------|------|---------|
| auth-service | 8081 | JWT, API keys, RBAC, SSO |
| management-service | 8082 | Rule CRUD, versioning, folders, tags |
| solver-service | 8083 | Rule execution (L1 Caffeine → L2 Redis → PG) |
| event-service | 8084 | Audit trail, webhooks (Kafka consumer) |
| analytics-service | 8085 | Decision logs → Elasticsearch [Phase 3] |
| scheduler-service | 8086 | Batch processing [Phase 2] |
| connector-service | 8087 | DB/HTTP enrichment [Phase 2] |

Shared module `ruleforge-common`: TenantContext, DTOs (ErrorResponse, PageResponse), Kafka schemas.

### Data Layer

- **PostgreSQL 16**: Rule definitions (JSONB), users, tenants. RLS for multi-tenancy via `app.current_org_id` session variable.
- **Redis 7**: Compiled rule cache (L2). Key pattern: `tenant:{orgId}:rule:{ruleId}:{version}:compiled`
- **Kafka 3.7**: Topics: `rule.lifecycle`, `rule.execution`, `user.activity`, `system.events`
- **Elasticsearch 8**: Decision logs, analytics aggregation
- **S3/MinIO**: Rule exports, batch results

### Frontend (`frontend/`, port 3000)

React 18 + TypeScript + Vite. Zustand for client state, React Query for server state. React Flow for tree/flow editors. Tailwind + Shadcn/ui.

### API Gateway (Kong, port 8000)

Routes `/v1/solve` → solver, `/v1/rules|folders|tags` → management, `/v1/auth|users|organizations|spaces|api-keys` → auth, `/v1/analytics` → analytics, `/v1/ai` → ai-service.

### Multi-Tenancy

Organization → Space → Folder → Rule. PostgreSQL RLS on all tenant tables. TenantContext propagated via ThreadLocal from JWT/API key.

### Development Roadmap

- **Phase 1 MVP**: Tables + Trees engine, Auth, Management, Solver, basic frontend
- **Phase 2**: Scripting (GraalVM), Flows, Connectors, SSO
- **Phase 3**: Analytics, Scorecard, Ensemble, SDKs
- **Phase 4**: AI Assistant, DMN, CEP, Embedded JAR, WASM
