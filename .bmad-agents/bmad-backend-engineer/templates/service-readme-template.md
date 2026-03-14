# [Service Name] Microservice

[![Build Status](https://ci.bmad.io/api/v1/projects/bmad-[service-name]/status.svg)](https://ci.bmad.io/bmad-[service-name])
[![Code Coverage](https://codecov.io/gh/bmad/[service-name]/branch/main/graph/badge.svg)](https://codecov.io/gh/bmad/[service-name])
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](./CHANGELOG.md)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](./LICENSE)

## Overview

[One-line description of what this service does]

This microservice is responsible for [specific business capability]. It provides [main functional areas] and integrates with [key upstream/downstream services].

**Owner:** [Team Name/Slack channel]
**SLO:** 99.9% availability, P99 latency < 200ms
**Status:** Production

---

## Architecture Context

### Upstream Dependencies
| Service | Purpose | Integration |
|---|---|---|
| Auth Service | User authentication & authorization | gRPC, sync |
| User Service | User profile & preferences | REST API, async via events |
| Config Service | Feature flags & configuration | gRPC client library |

### Downstream Dependencies
| Service | Purpose | How We Integrate |
|---|---|---|
| Notification Service | Sends emails, SMS, push notifications | Kafka events: `[service].*.events` |
| Analytics Service | Event ingestion & data warehouse | Kafka events: `[service].*.events` |
| Audit Service | Compliance & audit trail | Kafka events: `[service].audit` |

### Event Sources/Sinks

**Events Produced:**
- `service.resource.created` - Resource creation events
- `service.resource.updated` - Resource update events
- `service.resource.deleted` - Resource deletion events

**Events Consumed:**
- `auth.user.deactivated` - Handle user deactivation cleanup
- `billing.subscription.cancelled` - Cleanup on subscription cancellation

---

## Tech Stack

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Language** | [Java/Kotlin/Go/Python] | [x.x.x] | [Rationale or migration notes] |
| **Framework** | Spring Boot / Gin / FastAPI | [x.x.x] | Primary application framework |
| **Database** | PostgreSQL | 14+ | Primary data store; replicated to standby |
| **Cache** | Redis | 6.2+ | Session cache, rate limit counters |
| **Message Queue** | Apache Kafka | 2.8+ | Event streaming for async operations |
| **Authentication** | OAuth 2.0 + JWT | - | Bearer token validation via Auth Service |
| **Service Discovery** | Kubernetes DNS | - | k8s-native DNS service discovery |
| **Observability Stack** | Prometheus, Grafana, Jaeger, ELK | Latest | Metrics, dashboards, traces, logs |
| **Container Runtime** | Docker | 20.10+ | Multi-stage builds for optimization |
| **Orchestration** | Kubernetes | 1.24+ | EKS cluster in production |

---

## Getting Started

### Prerequisites
- Java 17+ / Go 1.19+ / Python 3.11+ (depending on tech stack)
- Docker 20.10+
- Docker Compose 2.0+
- PostgreSQL 14+ (or use Docker Compose)
- Redis 6.2+ (or use Docker Compose)
- [Service CLI] if applicable
- Git

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ENVIRONMENT` | Yes | `development` | Deployment environment (development, staging, production) |
| `DATABASE_URL` | Yes | `postgres://localhost/service_dev` | PostgreSQL connection string |
| `DATABASE_MAX_CONNECTIONS` | No | 20 | Connection pool size |
| `REDIS_URL` | Yes | `redis://localhost:6379/0` | Redis connection string |
| `KAFKA_BROKERS` | Yes | `localhost:9092` | Comma-separated list of Kafka brokers |
| `KAFKA_GROUP_ID` | No | `[service-name]-consumer` | Kafka consumer group ID |
| `AUTH_SERVICE_URL` | Yes | `http://localhost:8081` | Auth Service endpoint |
| `LOG_LEVEL` | No | `INFO` | Logging verbosity (DEBUG, INFO, WARN, ERROR) |
| `LOG_FORMAT` | No | `json` | Log format (json or text) |
| `PORT` | No | 8080 | HTTP server listening port |
| `METRICS_PORT` | No | 9090 | Prometheus metrics port |
| `JAEGER_AGENT_HOST` | No | `localhost` | Jaeger agent hostname |
| `JAEGER_AGENT_PORT` | No | 6831 | Jaeger agent port |
| `FEATURE_FLAG_URL` | Yes | `http://localhost:8085` | Config Service URL for feature flags |
| `SECRET_VAULT_URL` | Yes | `https://vault.bmad.io` | HashiCorp Vault endpoint |
| `SECRET_VAULT_TOKEN` | Yes | - | Vault authentication token (use K8s auth in prod) |

### Local Setup

1. **Clone the repository:**
```bash
git clone git@github.com:bmad/[service-name].git
cd [service-name]
```

2. **Copy environment configuration:**
```bash
cp .env.example .env
# Edit .env with your local values
```

3. **Start dependencies via Docker Compose:**
```bash
docker-compose up -d postgres redis kafka
# Wait for containers to be healthy
docker-compose ps
```

4. **Install dependencies:**
```bash
# For Java/Kotlin
./mvnw clean install

# For Go
go mod download

# For Python
pip install -r requirements.txt
```

5. **Initialize database schema:**
```bash
./scripts/db-migrate.sh
```

6. **Run the service:**
```bash
# For Java/Kotlin
./mvnw spring-boot:run

# For Go
go run ./cmd/server

# For Python
python -m uvicorn main:app --reload
```

The service will be available at `http://localhost:8080`

---

## Running Locally

### Docker Compose (Complete Stack)
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Clean up volumes
docker-compose down -v
```

**Docker Compose Services:**
- `postgres` - PostgreSQL database on port 5432
- `redis` - Redis cache on port 6379
- `kafka` - Apache Kafka broker on port 9092
- `zookeeper` - Kafka coordination on port 2181
- `[service-name]` - Application service on port 8080
- `jaeger` - Distributed tracing on port 16686 (UI)

### Standalone Commands
```bash
# Build Docker image
docker build -t bmad/[service-name]:latest .

# Run container with local dependencies
docker run -d \
  -p 8080:8080 \
  -e DATABASE_URL="postgres://postgres:password@host.docker.internal:5432/service_dev" \
  -e REDIS_URL="redis://host.docker.internal:6379" \
  -e KAFKA_BROKERS="host.docker.internal:9092" \
  --name [service-name] \
  bmad/[service-name]:latest

# View logs
docker logs -f [service-name]

# Stop container
docker stop [service-name]
docker rm [service-name]
```

---

## API Documentation

**Full API specification:** See [./api-contract.md](./api-contract.md)

**Interactive API documentation:**
- Swagger UI: `http://localhost:8080/swagger-ui.html` (when running locally)
- OpenAPI JSON: `http://localhost:8080/v1/openapi.json`

### Quick Reference - Core Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Service health check |
| `GET` | `/health/live` | Liveness probe for Kubernetes |
| `GET` | `/health/ready` | Readiness probe for Kubernetes |
| `GET` | `/v1/resources` | List resources (paginated) |
| `POST` | `/v1/resources` | Create a new resource |
| `GET` | `/v1/resources/{id}` | Get resource details |
| `PUT` | `/v1/resources/{id}` | Update a resource |
| `DELETE` | `/v1/resources/{id}` | Delete a resource |
| `GET` | `/metrics` | Prometheus metrics |

---

## Testing

### Running Tests

#### Unit Tests
```bash
# Run all unit tests
./mvnw test  # Java
go test ./... -v  # Go
pytest tests/unit -v  # Python

# Run with coverage
./mvnw test jacoco:report  # Java
go test ./... -cover  # Go
pytest --cov=src tests/unit  # Python
```

#### Integration Tests
```bash
# Integration tests (requires Docker dependencies)
docker-compose up -d

# Run integration suite
./mvnw verify -DskipUnitTests  # Java
go test ./... -tags=integration -v  # Go
pytest tests/integration -v -m integration  # Python

# Clean up
docker-compose down
```

#### End-to-End Tests
```bash
# Run service and dependencies
docker-compose up -d

# Run e2e tests
./scripts/run-e2e.sh  # Custom script
# or
pytest tests/e2e -v  # Python

docker-compose down
```

### Coverage Targets
- **Unit Test Coverage:** 80% minimum (strictly enforced by CI)
- **Integration Test Coverage:** 60% minimum
- **Critical Paths:** 95% coverage required (payment, auth, data deletion)

### Testing Best Practices
- Use Arrange-Act-Assert pattern for clarity
- Mock external dependencies (databases, APIs) in unit tests
- Use test containers (Testcontainers) for database integration tests
- Test error cases and edge conditions
- Avoid testing implementation details; test behavior
- Use meaningful assertion messages

---

## Deployment

### Environments

| Environment | URL | Database | Scale | Auto-scaling | Backup |
|---|---|---|---|---|---|
| **Development** | https://api-dev.bmad.io | Shared RDS | 1 replica | No | Daily |
| **Staging** | https://api-staging.bmad.io | RDS (prod-like schema) | 2 replicas | Yes (2-5) | Daily |
| **Production** | https://api.bmad.io | RDS Multi-AZ + replicas | 5+ replicas | Yes (5-20) | Hourly + point-in-time |

### Deploy Command
```bash
# Deploy to development
make deploy ENV=dev VERSION=$(git rev-parse --short HEAD)

# Deploy to staging (requires code review approval)
make deploy ENV=staging VERSION=$(git rev-parse --short HEAD)

# Deploy to production (requires two approvals, canary + full rollout)
make deploy ENV=prod VERSION=$(git rev-parse --short HEAD) STRATEGY=canary
```

**Deployment Process:**
1. Build Docker image and push to ECR
2. Update ECS task definition with new image
3. Canary deployment: 10% of traffic for 5 minutes
4. Monitor error rates, latency, and application metrics
5. Automatic rollback if error rate > 5% or latency P99 > 500ms
6. If canary succeeds, roll out to 100% over 10 minutes

### Rollback Command
```bash
# Rollback to previous version (automatic or manual)
make rollback ENV=prod VERSION=[previous-version]

# Rollback by timestamp
make rollback ENV=prod TIMESTAMP="2026-03-14T10:30:00Z"
```

**Rollback SLA:** 2 minutes maximum time to restore previous version

---

## Observability

### Metrics Endpoint
- **URL:** `http://localhost:9090/metrics`
- **Format:** Prometheus text format
- **Scrape Interval:** 30 seconds (configured in Prometheus)

### Key Service Metrics
```
# Latency
http_request_duration_seconds{method="GET", path="/v1/resources", le="0.1"} 2341
http_request_duration_seconds{method="GET", path="/v1/resources", le="0.5"} 2589
http_request_duration_seconds{method="GET", path="/v1/resources", le="+Inf"} 2600

# Throughput
http_requests_total{method="GET", path="/v1/resources", status="200"} 2600

# Errors
http_requests_total{method="GET", path="/v1/resources", status="500"} 5
http_requests_total{method="GET", path="/v1/resources", status="4xx"} 15

# Dependencies
database_connection_pool_size{pool="primary"} 20
database_connection_pool_active{pool="primary"} 8
kafka_consumer_lag{group="[service-name]-consumer", topic="auth.user.events"} 42
```

### Log Format (Structured JSON)
```json
{
  "timestamp": "2026-03-14T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.bmad.service.ResourceService",
  "message": "Resource created successfully",
  "service": "resource-service",
  "environment": "production",
  "trace_id": "123e4567-e89b-12d3-a456-426614174000",
  "span_id": "9a3a3a3a3a3a3a3a",
  "user_id": "user-123",
  "resource_id": "res-550e8400-e29b-41d4-a716-446655440000",
  "duration_ms": 145,
  "status": "success"
}
```

### Required Log Fields
- `timestamp` - ISO 8601 format with millisecond precision
- `level` - DEBUG, INFO, WARN, ERROR, CRITICAL
- `logger` - Source logger/module name
- `message` - Human-readable message (no PII)
- `service` - Service identifier
- `environment` - Deployment environment
- `trace_id` - Distributed trace correlation ID
- `span_id` - Individual operation span ID
- Context fields: `user_id`, `request_id`, `session_id` as applicable

### Alerting & Runbooks

**Critical Alerts:**
1. **High Error Rate** (> 5% of requests)
   - Runbook: [./docs/runbooks/high-error-rate.md](./docs/runbooks/high-error-rate.md)
   - PagerDuty trigger: Immediate (P1)

2. **High Latency** (P99 > 500ms)
   - Runbook: [./docs/runbooks/high-latency.md](./docs/runbooks/high-latency.md)
   - PagerDuty trigger: 5 minutes (P2)

3. **Database Connection Pool Exhaustion**
   - Runbook: [./docs/runbooks/db-pool-exhaustion.md](./docs/runbooks/db-pool-exhaustion.md)
   - PagerDuty trigger: Immediate (P1)

4. **Kafka Consumer Lag > 10 minutes**
   - Runbook: [./docs/runbooks/kafka-lag.md](./docs/runbooks/kafka-lag.md)
   - PagerDuty trigger: 10 minutes (P2)

---

## Contributing

### Development Workflow
1. Create feature branch: `git checkout -b feature/TICKET-123-description`
2. Make changes following [coding standards](./docs/CODING_STANDARDS.md)
3. Write tests for new functionality
4. Ensure tests pass: `./mvnw test`
5. Create pull request with detailed description
6. Address code review feedback
7. Merge when approved by at least one reviewer
8. Push to staging for integration testing

### Code Review Checklist
- Tests written for new code (unit + integration)
- All tests pass locally and in CI
- No PII or secrets committed
- Code follows style guide
- Changes documented (API contracts, config, data models)
- Database migrations included if needed
- Performance impact assessed (especially queries)
- Backward compatibility maintained or breaking change documented

### Commit Message Convention
```
TICKET-123: Brief description of change

Longer explanation of why this change was made, what problem it solves,
and any notable implementation details. Reference related tickets.

Breaking changes:
- List any breaking changes with migration path
```

---

## Changelog

### [1.0.0] - 2026-03-14
- Initial production release
- Core CRUD operations for resources
- Event publishing for resource lifecycle
- Health check and metrics endpoints
- Full API documentation

### [0.2.0] - 2026-03-01
- Added filtering and sorting to list endpoint
- Implemented cursor-based pagination
- Event consumption for external events

### [0.1.0] - 2026-02-15
- Initial beta release
- Basic service scaffold
- Health check endpoint

---

## Support

- **Documentation:** [./docs/README.md](./docs/README.md)
- **API Reference:** [./docs/api-contract.md](./docs/api-contract.md)
- **Runbooks:** [./docs/runbooks/](./docs/runbooks/)
- **Slack Channel:** #[service-name] or #[team-name]
- **On-call:** Check PagerDuty for current responder
