# RuleForge

An enterprise Business Rules Management System (BRMS) with an embeddable rule engine, microservices platform, and React-based management UI.

## Architecture

RuleForge consists of three main components:

- **Engine** — A standalone, embeddable Java rule engine library with zero external dependencies (core). Supports decision tables, decision trees, and an expression language (RFL).
- **Platform** — Spring Boot microservices providing multi-tenant rule management, execution, authentication, and event processing.
- **Frontend** — React 18 + TypeScript SPA for rule authoring, testing, and administration.

## Project Structure

```
ruleforge/
├── engine/                         # Embeddable rule engine (Gradle multi-module)
│   ├── ruleforge-core/             # Core model, memory, SPI, RFL parser, engine API
│   ├── ruleforge-table/            # Decision Table engine (7 index types)
│   ├── ruleforge-tree/             # Decision Tree engine
│   └── ruleforge-spring-boot-starter/
├── platform/                       # Microservices (Spring Boot 3.3, Gradle)
│   ├── ruleforge-common/           # Shared DTOs, JWT, security filters
│   ├── ruleforge-auth-service/     # Authentication, orgs, spaces, API keys
│   ├── ruleforge-management-service/ # Rule CRUD, versioning, folders, tags
│   ├── ruleforge-solver-service/   # Rule execution with multi-layer caching
│   └── ruleforge-event-service/    # Kafka consumers, audit events, webhooks
├── frontend/                       # React 18 + TypeScript + Vite + Tailwind
├── db/migrations/                  # Flyway SQL migrations (PostgreSQL + RLS)
├── infra/                          # Docker, Helm, Terraform, Kong configs
└── docs/architecture/              # Architecture design documents
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Engine | Java 21, zero external deps (SLF4J API + Jackson only) |
| Platform | Spring Boot 3.3, PostgreSQL 16, Redis, Kafka, Caffeine |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Zustand, React Query |
| Infrastructure | Docker, Kong API Gateway, Kubernetes (Helm) |

## Building

### Prerequisites

- Java 21+
- Node.js 18+
- Gradle (wrapper included)

### Engine

```bash
cd engine
./gradlew compileJava    # compile
./gradlew test           # run tests
```

### Platform

The platform uses a composite Gradle build that includes the engine.

```bash
cd platform
./gradlew compileJava    # compile all services
./gradlew test           # run tests
```

### Frontend

```bash
cd frontend
npm install
npm run dev              # development server
npm run build            # production build
```

## Key Features

- **Decision Tables** — Spreadsheet-style rule authoring with 7 specialized index types for high-performance evaluation
- **Decision Trees** — Visual tree editor with multi-branch conditions
- **RFL Expression Language** — Hand-written recursive descent parser supporting arithmetic, comparisons, string operations, and date functions
- **Multi-Tenant** — Row-Level Security (RLS) on PostgreSQL for tenant isolation
- **Multi-Layer Caching** — L1 Caffeine (per-JVM) → L2 Redis (shared) → L3 PostgreSQL
- **Event-Driven** — Kafka-based event processing for audit trails, cache invalidation, and webhooks
- **API Key Authentication** — Separate SOLVER and MANAGEMENT key types with scoped permissions

## License

Proprietary. All rights reserved.
