# Getting Started with RuleForge

RuleForge is an enterprise Business Rule Management System (BRMS) featuring a custom rule execution engine, microservice platform, and web-based rule editor.

## Prerequisites

| Tool           | Minimum Version | Verify                   |
| -------------- | --------------- | ------------------------ |
| Java (JDK)     | 21+             | `java -version`          |
| Node.js        | 18+             | `node -v`                |
| npm            | 9+              | `npm -v`                 |
| Docker         | 24+             | `docker --version`       |
| Docker Compose | 2.20+           | `docker compose version` |

**Optional:**

- **Flyway CLI** — for running migrations outside of `make migrate`
- **GraalVM 21+** — required only for `ruleforge-script` module development (Phase 2)
- **Python 3.10+** — required only for the AI service (Phase 4)

## 1. Clone and Explore

```bash
git clone <repository-url> ruleforge
cd ruleforge
```

Directory layout:

```
ruleforge/
├── engine/          # Rule engine library (Gradle, Java 21) — 11 modules
├── platform/        # Microservices (Gradle, Spring Boot 3.3.5) — 8 services
├── frontend/        # Web UI (Vite, React 18, TypeScript, Tailwind)
├── ai-service/      # AI assistant (Python, FastAPI) — Phase 4
├── db/migrations/   # Flyway SQL migrations (V001–V020)
├── infra/docker/    # Docker Compose files, Kong gateway config
├── docs/            # Architecture and design documents
└── Makefile         # Top-level build and orchestration commands
```

## 2. Start Infrastructure

```bash
make infra-up
```

This starts the following containers:

| Service               | Port | Details                                                             |
| --------------------- | ---- | ------------------------------------------------------------------- |
| PostgreSQL 16         | 5432 | Database: `ruleforge`, User: `ruleforge`, Password: `ruleforge_dev` |
| Redis 7               | 6379 | 256 MB maxmemory, allkeys-lfu eviction policy                       |
| Apache Kafka 3.7.1    | 9092 | KRaft mode (no ZooKeeper), single node                              |
| Elasticsearch 8.13.0  | 9200 | Single node, security disabled for local dev                        |
| MinIO (S3-compatible) | 9000 | Console on 9001, User: `ruleforge` / `ruleforge_dev`                |

Verify all containers are running and healthy:

```bash
docker compose ps
```

To stop infrastructure later:

```bash
make infra-down
```

## 3. Database Setup

```bash
make migrate
```

This runs Flyway migrations V001 through V020 against the local PostgreSQL instance.

**Migration overview:**

| Migrations | Purpose                                                |
| ---------- | ------------------------------------------------------ |
| V001–V005  | Organizations, users, spaces, members, teams           |
| V006–V008  | Folders, rules, rule_versions (JSONB definitions)      |
| V009       | API keys with SHA-256 hashing                          |
| V010       | Tags and rule-tag associations                         |
| V011–V012  | Audit events, webhooks                                 |
| V013–V015  | Connectors, schedules, schedule_runs (Phase 2)         |
| V016       | SSO configurations (Phase 2)                           |
| V020       | Row-Level Security (RLS) policies on all tenant tables |

**Row-Level Security:** PostgreSQL enforces organization-level data isolation at the database layer using the session variable `app.current_org_id`. The platform sets this variable before each request via `TenantContext`, so every query automatically filters by the current tenant.

**Connect manually:**

```bash
psql -h localhost -p 5432 -U ruleforge -d ruleforge
```

## 4. Build the Engine

```bash
cd engine
./gradlew build
```

The engine is a Gradle multi-module project with 11 modules. The core module (`ruleforge-core`) has zero external dependencies beyond SLF4J API and Jackson, making it embeddable in any Java application.

**Phase 1 modules (complete):**

| Module                          | Purpose                                                           |
| ------------------------------- | ----------------------------------------------------------------- |
| `ruleforge-core`                | Core model, WorkingMemory, RFL expression parser, SPI, engine API |
| `ruleforge-table`               | Decision Table engine with 7 condition index types                |
| `ruleforge-tree`                | Decision Tree engine with BDD compilation                         |
| `ruleforge-spring-boot-starter` | Spring Boot auto-configuration                                    |

**Phase 2+ modules (planned):**

| Module                | Purpose                                |
| --------------------- | -------------------------------------- |
| `ruleforge-script`    | GraalVM sandboxed JavaScript execution |
| `ruleforge-flow`      | DAG flow orchestrator                  |
| `ruleforge-scorecard` | Weighted scoring engine                |
| `ruleforge-ensemble`  | Voting/averaging across engines        |
| `ruleforge-cep`       | Kafka Streams complex event processing |
| `ruleforge-dmn`       | DMN 1.4 / FEEL support                 |
| `ruleforge-hierarchy` | Hierarchical rule resolution           |

## 5. Build Platform Services

```bash
cd platform
./gradlew build
```

The platform uses a **composite Gradle build** that includes the engine directly (`settings.gradle.kts` contains `includeBuild("../engine")`). All 8 services are built from this single command.

Services share the `ruleforge-common` module, which provides JWT handling, security filters, DTOs (`ErrorResponse`, `PageResponse`), and Kafka event schemas.

## 6. Run Services

### Option A: Run Individually (Development)

Start services in dependency order, each in its own terminal:

```bash
# 1. Auth service (must start first — other services validate tokens against it)
cd platform && ./gradlew :ruleforge-auth-service:bootRun

# 2. Management service (rule CRUD, versioning, folders, tags)
cd platform && ./gradlew :ruleforge-management-service:bootRun

# 3. Solver service (rule execution, depends on auth for API key validation)
cd platform && ./gradlew :ruleforge-solver-service:bootRun

# 4. Event service (Kafka consumer for audit trail and webhooks)
cd platform && ./gradlew :ruleforge-event-service:bootRun
```

**Optional Phase 2 services:**

```bash
cd platform && ./gradlew :ruleforge-scheduler-service:bootRun   # Batch processing
cd platform && ./gradlew :ruleforge-connector-service:bootRun   # DB/HTTP enrichment
```

**Service ports:**

| Service            | Port |
| ------------------ | ---- |
| auth-service       | 8081 |
| management-service | 8082 |
| solver-service     | 8083 |
| event-service      | 8084 |
| analytics-service  | 8085 |
| scheduler-service  | 8086 |
| connector-service  | 8087 |

### Option B: Docker Compose (Full Stack)

```bash
make stack-up
```

This builds and starts all services, the Kong API gateway (port 8000), and the frontend (port 3000).

Stop everything with `make stack-down`.

## 7. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on **http://localhost:3000**. Vite proxies all `/v1/*` API requests to the Kong gateway at `http://localhost:8000`.

**Tech stack:** React 18, TypeScript, Tailwind CSS, Shadcn/ui components, Zustand (client state), React Query (server state), React Flow (tree/flow visual editors).

## 8. Start AI Service (Optional)

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Requires the `ANTHROPIC_API_KEY` environment variable (or your chosen LLM provider key). This is a Phase 4 feature and currently contains only a skeleton implementation.

## 9. Verify Your Setup

Walk through the full workflow: register, create an organization space, define a rule, and execute it.

### Register a User

```bash
curl -X POST http://localhost:8081/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "SecurePass123!",
    "fullName": "Admin User"
  }'
```

The response includes `accessToken`, `refreshToken`, and the created organization's `orgId`. Save these for the next steps.

### Login

```bash
curl -X POST http://localhost:8081/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "SecurePass123!"}'
```

### Create a Space

Use the `orgId` from registration:

```bash
curl -X POST http://localhost:8081/v1/organizations/{orgId}/spaces \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Production", "slug": "production"}'
```

### Create a Decision Table Rule

```bash
curl -X POST http://localhost:8082/v1/spaces/{spaceId}/rules \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Age Check",
    "type": "TABLE",
    "alias": "age-check",
    "description": "Simple age eligibility check"
  }'
```

### Execute a Rule via the Solver

```bash
curl -X POST http://localhost:8083/v1/solve/age-check \
  -H "X-API-Key: <solver-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"input": {"age": 25, "country": "US"}}'
```

The solver uses a three-tier cache: L1 Caffeine (in-process), L2 Redis, then PostgreSQL.

## 10. Makefile Reference

| Command               | Description                                                   |
| --------------------- | ------------------------------------------------------------- |
| `make infra-up`       | Start infrastructure containers (PG, Redis, Kafka, ES, MinIO) |
| `make infra-down`     | Stop infrastructure containers                                |
| `make build-engine`   | Build all engine modules                                      |
| `make build-platform` | Build all platform services                                   |
| `make test-engine`    | Run engine tests                                              |
| `make test-platform`  | Run platform tests                                            |
| `make test-frontend`  | Run frontend tests                                            |
| `make migrate`        | Run Flyway database migrations                                |
| `make stack-up`       | Build and start full stack (Docker Compose)                   |
| `make stack-down`     | Stop full stack                                               |
| `make dev-frontend`   | Start frontend dev server                                     |
| `make lint-frontend`  | Run ESLint on frontend                                        |

## Troubleshooting

### Port Already in Use

Check which process is using the port and stop it:

```bash
lsof -i :8081
kill <PID>
```

Alternatively, change the port in the service's `application.yml`.

### Kafka Takes Long to Start

Kafka's health check can take 30-60 seconds on the first startup. Wait for `docker compose ps` to show all containers as "healthy" before starting platform services.

### Flyway Migration Failures

If migrations fail partway through, check the `flyway_schema_history` table in PostgreSQL. For a clean start, drop and recreate the database:

```bash
dropdb -h localhost -U ruleforge ruleforge
createdb -h localhost -U ruleforge ruleforge
make migrate
```

### Gradle Composite Build Issues

If changes to the engine are not picked up by the platform build, publish the engine locally first:

```bash
cd engine && ./gradlew publishToMavenLocal
cd platform && ./gradlew build --refresh-dependencies
```

### Frontend Proxy Not Working

The Vite dev server proxies `/v1/*` requests to the Kong gateway on port 8000. Ensure Kong is running (`docker compose ps`). Without Kong, all API calls from the frontend will fail.

### Docker Memory Issues

The full infrastructure stack requires approximately 4 GB of RAM. If containers are being killed, increase the Docker memory limit in Docker Desktop settings.
