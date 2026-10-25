# Confirmed Tech Stack: RuleForge

## Changelog

- 2026-10-25: Populate detected technology rows and distinguish configuration evidence from operational verification. Derived from repository files; runtime health and release readiness were not assessed.

> Repository detections are marked with their source. Missing decisions remain under review.
> Once a decision is recorded here, agents do NOT re-open it.

## Decision Status Legend
- ✅ Confirmed — do not re-discuss
- 🔄 Under review — architect still evaluating
- ❌ Rejected — do not suggest

---

## Application Layer (Solution Architect)

### Backend
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Primary language | Java 25 | ✅ Confirmed — auto-detected from platform/gradle.properties; engine/gradle.properties | Manifest/configuration evidence only |
| Framework | Spring Boot 4.1.1; dependency management plugin 1.1.7 | ✅ Confirmed — auto-detected from platform/build.gradle.kts | Manifest/configuration evidence only |
| API style | REST /v1 endpoints | ✅ Confirmed — auto-detected from README.md; frontend/vite.config.ts | Manifest/configuration evidence only |

### Frontend
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Framework | React ^18.3.1; TypeScript ^5.5.4; Vite ^6.4.2 | ✅ Confirmed — auto-detected from frontend/package.json | Manifest/configuration evidence only |
| Component library | Lucide icons; Monaco; React Flow; Recharts | ✅ Confirmed — auto-detected from frontend/package.json | Manifest/configuration evidence only |
| State management | Zustand; TanStack React Query | ✅ Confirmed — auto-detected from frontend/package.json | Manifest/configuration evidence only |
| CSS | Tailwind CSS ^3.4.7 | ✅ Confirmed — auto-detected from frontend/package.json | Manifest/configuration evidence only |

### Mobile
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Approach | Under review | 🔄 | Not established by scanned sources |
| Framework | Under review | 🔄 | Not established by scanned sources |

### Data
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Primary database | PostgreSQL 16 | ✅ Confirmed — auto-detected from infra/docker/docker-compose.infra.yml | Manifest/configuration evidence only |
| Cache | Redis 7; Caffeine 3.1.8 | ✅ Confirmed — auto-detected from infra/docker/docker-compose.infra.yml; platform/gradle.properties | Manifest/configuration evidence only |
| Search | Elasticsearch 9.4.5 | ✅ Confirmed — auto-detected from infra/docker/docker-compose.infra.yml | Manifest/configuration evidence only |
| Message queue | Kafka 3.7.1 | ✅ Confirmed — auto-detected from infra/docker/docker-compose.infra.yml | Manifest/configuration evidence only |

### Integration
| Category | Decision | Status | Rationale |
|---|---|---|---|
| API gateway | Kong 3.6 | ✅ Confirmed — auto-detected from infra/docker/docker-compose.yml | Manifest/configuration evidence only |
| Auth / identity | JWT via JJWT 0.12.6 | ✅ Confirmed — auto-detected from platform/ruleforge-common/build.gradle.kts; platform/gradle.properties | Manifest/configuration evidence only |
| Workflow engine | Under review | 🔄 | Not established by scanned sources |

---

## Platform Layer (Enterprise Architect)

### Infrastructure
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Cloud provider | Under review | 🔄 | Not established by scanned sources |
| Container orchestration | Docker Compose for local stack | ✅ Confirmed — auto-detected from infra/docker/docker-compose.yml | Manifest/configuration evidence only |
| Service mesh | Under review | 🔄 | Not established by scanned sources |
| CI/CD platform | Under review | 🔄 | Not established by scanned sources |

### Observability
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Metrics | Under review | 🔄 | Not established by scanned sources |
| Logging | Per-service files under logs/ | ✅ Confirmed — auto-detected from README.md; scripts/start-local.sh | Manifest/configuration evidence only |
| Tracing | Under review | 🔄 | Not established by scanned sources |
| Alerting | Under review | 🔄 | Not established by scanned sources |

### Data Platform
| Category | Decision | Status | Rationale |
|---|---|---|---|
| Data lake / warehouse | Under review | 🔄 | Not established by scanned sources |
| BI / visualisation | Recharts ^3.7.0 | ✅ Confirmed — auto-detected from frontend/package.json | Manifest/configuration evidence only |

---

## Rejected Technologies
| Technology | Category | Rejected by | Reason |
|---|---|---|---|
| None inferred | Under review | Under review | No rejection decisions established by onboarding |

## Architecture Decision Records
All ADRs are in `docs/architecture/adr/`. See index below:
| ADR | Decision | Date |
|---|---|---|
| [ADR index](../docs/architecture/adr/ADR-INDEX.md) | Existing decisions; not re-approved during onboarding | See source documents |

## Additional Detected Signals

| Area | Detection | Source |
|---|---|---|
| AI service | Python >=3.11; FastAPI; Uvicorn; Anthropic SDK | ai-service/pyproject.toml |
| Build | Gradle Kotlin DSL; wrapper distribution 9.8.2 | platform/build.gradle.kts; platform/gradle/wrapper/gradle-wrapper.properties |
| Tests | JUnit Platform; Mockito; Vitest; Testing Library; pytest | engine/build.gradle.kts; platform/build.gradle.kts; frontend/package.json; ai-service/pyproject.toml |
| Tenant context | ScopedValue binding; explicit re-binding for work on other threads | platform/ruleforge-common/src/main/java/io/ruleforge/common/tenant/TenantContext.java |
| Migrations | Versioned SQL files | db/migrations/ |
| CI | No workflow files found under .github/workflows | Tracked repository files |
| Infrastructure gaps | No Helm or Terraform configuration found under infra/ | infra/ inventory |

Dependency ranges above describe declarations, not installed versions. Unknown mobile, cloud, tracing, alerting, and service-mesh decisions remain under review.

## Known Source Caveats

- MinIO is configured in Compose; README reports image pull denial and says no service uses it. Configuration presence does not establish availability.
- README reports Kafka publication may fall back to NoOpEventPublisher because conditional component evaluation precedes Kafka auto-configuration. Event-driven audit, analytics, and invalidation are not verified by this inventory.
