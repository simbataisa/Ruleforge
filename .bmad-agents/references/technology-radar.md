# BMAD Technology Radar — Enterprise Stack Selection Guide

This reference document is used by the **Solution Architect** and **Enterprise Architect** agents to make context-driven technology selections. No single stack fits all projects. The right choice depends on the domain, team expertise, scale requirements, compliance constraints, and operational maturity.

## How to Use This Document

1. Identify the **decision category** (e.g., backend language, database, messaging)
2. Review the **options table** — each option lists its strengths, weaknesses, and ideal use cases
3. Use the **decision matrix template** at the bottom to document your selection with justification
4. Create an **ADR** for each significant technology choice

---

## Backend Languages & Frameworks

| Technology | Strengths | Weaknesses | Best For | Avoid When |
|-----------|-----------|------------|----------|------------|
| **Java + Spring Boot** | Mature ecosystem, excellent enterprise libraries, strong typing, massive talent pool, JVM performance tuning | Memory-heavy (512MB+ per service), slower startup (~2-5s), verbose | Complex business logic, financial systems, enterprise integrations, teams with Java expertise | Lightweight microservices needing fast cold starts, resource-constrained environments |
| **Kotlin + Spring Boot** | Java interop, null safety, coroutines for async, concise syntax, modern language features | Smaller talent pool than Java, build times slightly longer | Modern JVM services, teams migrating from Java, Android backend parity | Teams with no JVM experience, simple CRUD services |
| **Go (Golang)** | Fast compilation, tiny binaries (~10MB), low memory (~30MB), excellent concurrency (goroutines), fast cold start (<100ms) | Smaller ecosystem than JVM, no generics until recently, verbose error handling | High-throughput APIs, infrastructure tools, CLI tools, services needing fast scale-up/down | Complex domain logic with deep inheritance, teams without Go experience |
| **Python + FastAPI/Django** | Rapid development, rich ML/data libraries, huge ecosystem, easy hiring | GIL limits true concurrency, slower than compiled languages, type safety optional | Data-heavy services, ML inference, rapid prototyping, admin panels, scripting/automation | High-throughput low-latency APIs, CPU-intensive workloads |
| **Node.js + NestJS/Express** | Async I/O, shared language with frontend (TypeScript), fast development, NPM ecosystem | Single-threaded (worker_threads help), callback complexity, memory leaks in long-running processes | BFF (Backend for Frontend) layers, real-time WebSocket services, lightweight APIs, full-stack JS teams | CPU-intensive computation, heavy business logic, financial transaction processing |
| **Rust** | Zero-cost abstractions, memory safety without GC, blazing performance, tiny binaries | Steep learning curve, longer development time, smaller talent pool | Performance-critical services, infrastructure components, WASM targets, security-sensitive systems | Rapid prototyping, teams without Rust experience, CRUD-heavy services |
| **C# + .NET** | Strong typing, excellent tooling (Visual Studio/Rider), LINQ, Azure-native, good performance | Windows-centric ecosystem (though .NET 8+ is cross-platform), smaller Linux ops community | Azure-heavy environments, teams with .NET expertise, Windows enterprise shops | AWS/GCP-primary environments, teams without .NET experience |

### Decision Factors for Backend Language

Ask these questions to narrow the choice:

1. **What does the team already know?** Team expertise trumps theoretical superiority. A team shipping Java beats a team learning Go.
2. **What's the latency budget?** Sub-10ms → Go/Rust. Sub-100ms → Go/Java/Kotlin. Sub-500ms → anything works.
3. **What's the memory budget per service?** <50MB → Go/Rust. <256MB → Go/Node. <1GB → Java/Kotlin/.NET.
4. **How complex is the business logic?** Deep domain models with many rules → Java/Kotlin/C#. Simple request routing → Go/Node.
5. **Is there an ML/data component?** → Python for that specific service; other services in a different language.

---

## Frontend Frameworks

| Technology | Strengths | Weaknesses | Best For | Avoid When |
|-----------|-----------|------------|----------|------------|
| **React + TypeScript** | Largest ecosystem, mature tooling, huge talent pool, excellent component model, Next.js for SSR | Bundle size can grow, JSX learning curve, frequent ecosystem churn | Enterprise dashboards, complex SPAs, teams hiring broadly, component libraries | Simple static sites, teams preferring template-based frameworks |
| **Vue.js + TypeScript** | Gentle learning curve, excellent docs, Composition API, smaller bundle, Nuxt for SSR | Smaller talent pool than React, fewer enterprise-grade component libraries | Teams valuing simplicity, progressive adoption, Chinese market apps | Large teams needing maximum hiring pool, heavy ecosystem reliance |
| **Angular** | Opinionated (consistent across teams), built-in DI/routing/forms/HTTP, enterprise backing (Google) | Heavy framework, steep learning curve, verbose, slower innovation cycle | Large enterprise teams needing consistency, form-heavy admin apps, teams from .NET/Java background | Small teams, rapid prototyping, lightweight SPAs |
| **Svelte/SvelteKit** | Compile-time, tiny bundles, fast runtime, intuitive syntax | Small ecosystem, fewer enterprise libraries, smaller talent pool | Performance-critical UIs, small teams, developer experience focus | Large enterprise teams, projects needing extensive third-party integrations |
| **HTMX + Server Templates** | Minimal JS, server-rendered, progressive enhancement, simple mental model | Limited interactivity for complex UIs, not suitable for offline | Internal tools, admin panels, content-heavy sites, teams avoiding SPA complexity | Rich interactive dashboards, offline-capable apps, mobile-like experiences |

---

## Mobile Frameworks

| Technology | Strengths | Weaknesses | Best For | Avoid When |
|-----------|-----------|------------|----------|------------|
| **React Native** | Shared codebase (60-80%) with web React, large community, Expo for rapid dev | Bridge performance overhead, native module complexity, large app size | Teams with React expertise, apps with shared web/mobile logic, rapid cross-platform MVPs | Graphics-heavy apps (games), apps needing deep native API access |
| **Kotlin Multiplatform (KMP)** | Shared business logic (Kotlin), native UI per platform, no runtime overhead, type safety | Newer ecosystem, iOS team needs Kotlin knowledge, fewer UI libraries | Teams with Kotlin/JVM expertise, apps needing native performance + shared logic, backend-to-mobile code sharing | Teams without Kotlin experience, simple apps not justifying shared logic layer |
| **Flutter** | Single codebase for iOS/Android/Web/Desktop, hot reload, rich widget library, Dart language | Dart is niche, large app size, custom rendering (not native UI), limited native library access | Cross-platform apps with custom UI, startups shipping fast, teams willing to learn Dart | Apps requiring native look-and-feel, heavy native SDK integration |
| **Native (Swift/Kotlin)** | Best performance, full platform API access, native UX, best tooling | Two codebases, double the dev effort, separate teams needed | Performance-critical apps (fintech, health), apps with deep platform integration, large dedicated teams | Budget-constrained projects, simple CRUD apps, small teams |

---

## Databases

| Technology | Type | Strengths | Weaknesses | Best For | Avoid When |
|-----------|------|-----------|------------|----------|------------|
| **PostgreSQL** | Relational | ACID, extensions (PostGIS, pg_vector), JSONB for semi-structured, excellent query optimizer, mature replication | Vertical scaling limits, complex sharding (use Citus), connection overhead | Transactional data, user records, orders, financial data, most OLTP workloads | Time-series at massive scale, simple key-value lookups, document-centric data |
| **MySQL/MariaDB** | Relational | Widespread, simple replication, good read scaling, Vitess for sharding | Fewer advanced features than PostgreSQL, weaker JSON support | Legacy system compatibility, read-heavy workloads, WordPress/PHP ecosystems | Complex queries, advanced data types, GIS data |
| **MongoDB** | Document | Flexible schema, horizontal scaling (sharding built-in), rich query language, Atlas cloud | Eventual consistency by default, joins are expensive, no true ACID across shards until v5+ | Content management, product catalogs, user profiles, rapidly evolving schemas | Financial transactions, data requiring strong relational integrity |
| **Redis** | Key-Value/Cache | Sub-millisecond latency, rich data structures (hashes, sorted sets, streams), pub/sub | Memory-limited, persistence is secondary, not for primary data storage | Caching, session storage, rate limiting, leaderboards, real-time counters | Primary data store, data larger than RAM, complex queries |
| **ScyllaDB** | Wide-Column | CQL (Cassandra-compatible), 10x Cassandra throughput, predictable low latency, auto-tuning | Complex data modeling (denormalization required), no joins, limited ad-hoc queries | High-throughput write-heavy workloads, time-series, IoT sensor data, user activity logs | Complex relational queries, small datasets, ad-hoc analytics |
| **DynamoDB** | Key-Value/Document | Serverless, single-digit ms latency, AWS-native, auto-scaling, global tables | AWS lock-in, expensive at scale, limited query flexibility, GSI limits | AWS-native apps, serverless architectures, simple access patterns, global distribution | Complex queries, multi-cloud strategy, cost-sensitive high-volume reads |
| **SQLite** | Embedded Relational | Zero-config, embedded (no server), ACID, great for mobile/edge | Single-writer, no network access, limited concurrency | Mobile local storage, edge computing, embedded devices, testing, Litestream for replication | Multi-user concurrent write workloads, server-side primary databases |
| **TigerBeetle** | Financial ledger | Purpose-built for financial transactions, ACID, deterministic, 1M+ TPS | Very specialized (accounting only), new ecosystem, limited query capability | Double-entry accounting, financial ledgers, payment reconciliation | General-purpose data storage, non-financial workloads |
| **CockroachDB** | Distributed SQL | PostgreSQL-compatible, distributed ACID, multi-region, auto-sharding | Higher latency than single-node Postgres, operational complexity, licensing changes | Global applications needing distributed SQL, multi-region strong consistency | Single-region apps (PostgreSQL simpler), budget-constrained projects |
| **ClickHouse** | Columnar/OLAP | Blazing analytical queries, column compression, real-time ingestion | Not for OLTP, poor for point lookups, complex cluster management | Analytics, log analysis, real-time dashboards, aggregation-heavy workloads | Transactional workloads, frequent row updates, simple CRUD |

### Database Decision Framework

```
                    ┌─ Strong consistency needed?
                    │   Yes → PostgreSQL / CockroachDB (distributed)
                    │   No  → MongoDB / DynamoDB
                    │
Start ──→ Is it ───┤─ Financial/ledger data?
          relational?   Yes → PostgreSQL + TigerBeetle (ledger)
                    │
                    ├─ Write-heavy time-series?
                    │   Yes → ScyllaDB / ClickHouse (analytics)
                    │
                    ├─ Caching / real-time counters?
                    │   Yes → Redis
                    │
                    ├─ Mobile/edge local storage?
                    │   Yes → SQLite
                    │
                    └─ AWS serverless with simple access?
                        Yes → DynamoDB
```

---

## Messaging & Event Streaming

| Technology | Type | Strengths | Weaknesses | Best For | Avoid When |
|-----------|------|-----------|------------|----------|------------|
| **Apache Kafka** | Event Streaming | High throughput (millions msg/s), durable log, replay, exactly-once semantics, topic compaction | Operational complexity (ZooKeeper/KRaft), higher latency than queues (~10ms), storage costs | Event sourcing, audit trails, inter-service events, stream processing, data pipelines | Simple task queues, low-volume messaging, teams without Kafka ops experience |
| **RabbitMQ** | Message Queue | Flexible routing (exchanges, bindings), AMQP standard, low latency (<1ms), mature | Lower throughput than Kafka, no built-in replay, persistence is slower | Task queues, work distribution, RPC patterns, complex routing rules, lower-volume messaging | High-throughput event streaming, event replay requirements, audit trails |
| **AWS SQS + SNS** | Cloud Queue/PubSub | Serverless, zero ops, auto-scaling, dead-letter queues, cheap at moderate volume | AWS lock-in, limited ordering (FIFO queues have 300 msg/s limit), no replay | AWS-native apps, simple async patterns, serverless architectures, decoupling services | Multi-cloud, high-throughput streaming, event replay, complex routing |
| **Azure Service Bus** | Cloud Queue | Enterprise features (sessions, transactions, dead-letter), Azure-native | Azure lock-in, pricing complexity, lower throughput than Kafka | Azure-native apps, enterprise messaging patterns, ordered processing | Multi-cloud, extreme throughput, non-Azure environments |
| **NATS/NATS JetStream** | Messaging/Streaming | Ultra-low latency, simple ops, lightweight, JetStream adds persistence | Smaller ecosystem, fewer enterprise integrations | Edge/IoT messaging, microservice communication, lightweight pub/sub | Complex routing rules, enterprise compliance requirements |
| **Redpanda** | Kafka-compatible Streaming | Kafka API-compatible, no JVM (C++), simpler ops, lower latency | Younger project, smaller community, commercial features gated | Teams wanting Kafka semantics without Kafka ops complexity | Teams already running Kafka successfully, need for mature ecosystem |

### Messaging Decision Framework

```
Need event replay / audit trail?
  Yes → Kafka or Redpanda
  No  ↓

Need complex routing rules?
  Yes → RabbitMQ
  No  ↓

Running serverless on AWS?
  Yes → SQS + SNS
  No  ↓

Need ultra-low latency (<1ms)?
  Yes → NATS or RabbitMQ
  No  → Kafka (default for enterprise microservices)
```

---

## Design Patterns

| Pattern | When to Use | When to Avoid | Key Trade-offs |
|---------|------------|---------------|---------------|
| **BFF (Backend for Frontend)** | Multiple client types (web, mobile, TV) need different API shapes; reduce over-fetching | Single client type; simple CRUD APIs | Extra service to maintain; clearer client contracts |
| **Event-Driven Architecture** | Loose coupling between services; audit trail needed; async workflows | Simple request/response sufficient; team unfamiliar with eventual consistency | Complexity of eventual consistency; powerful decoupling and scalability |
| **SAGA (Choreography)** | Multi-service transactions; services are independent; no central orchestrator desired | Few services involved; strong consistency required across all steps | Each service must handle compensation; no single point of failure |
| **SAGA (Orchestration)** | Complex multi-step workflows; central visibility needed; conditional branching | Simple two-service interactions; overhead of orchestrator not justified | Central orchestrator can become bottleneck; easier to reason about flow |
| **TCC (Try-Confirm/Cancel)** | Two-phase reservation pattern; inventory holds, seat reservations, payment pre-auth | Simple atomic operations; latency budget doesn't allow two-phase | Resource locking during Try phase; strong guarantee if Confirm succeeds |
| **CQRS** | Read/write patterns differ significantly; read-heavy with complex projections | Simple CRUD; read and write models are identical | Separate read/write stores; eventual consistency between them |
| **Event Sourcing** | Full audit trail required; need to reconstruct state at any point in time | Simple state management; team unfamiliar with event stores | Storage grows linearly; powerful audit and replay; complex to query |
| **Strangler Fig** | Migrating from monolith to microservices incrementally | Greenfield project; full rewrite is feasible and preferred | Parallel running costs; safe incremental migration |
| **Circuit Breaker** | Calling external/unreliable services; preventing cascade failures | Internal calls with guaranteed uptime; local function calls | Added latency for health checking; prevents cascade failures |
| **Sidecar / Service Mesh** | Uniform cross-cutting concerns (mTLS, logging, tracing) across polyglot services | Single-language stack; few services; simpler alternatives exist | Resource overhead per pod; powerful uniform observability |

---

## API Gateway

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **Kong** | Open-source / Enterprise | Rich plugin ecosystem, Kubernetes-native, declarative config, Lua/Go plugins | Lua scripting can be limiting, Enterprise license for advanced features | Cloud-agnostic, Kubernetes, teams wanting extensibility |
| **Traefik** | Open-source | Auto-discovery (Docker/K8s labels), Let's Encrypt built-in, simple config | Fewer enterprise plugins than Kong, less middleware flexibility | Kubernetes/Docker environments, auto-routing, Let's Encrypt automation |
| **AWS API Gateway** | Managed | Serverless, Lambda integration, WebSocket support, usage plans | AWS lock-in, cold starts with Lambda, limited customization | AWS-native serverless architectures |
| **WSO2 API Manager** | Enterprise | Full API lifecycle management, developer portal, analytics, monetization | Heavy (Java-based), complex setup, licensing cost | Enterprise API management with developer portal and monetization |
| **Envoy / Istio** | Service Mesh | L7 proxy, gRPC-native, advanced traffic management, observability | Istio complexity, resource overhead, steep learning curve | Service mesh requirements, gRPC-heavy, advanced traffic routing |
| **APISIX** | Open-source | High performance (Nginx+Lua), rich plugins, dashboard UI, multi-language plugins | Smaller community than Kong, fewer enterprise case studies | High-performance gateway needs, teams comfortable with Nginx |
| **Azure API Management** | Managed | Azure-native, developer portal, policy engine, hybrid deployment | Azure lock-in, pricing at scale | Azure-native environments, hybrid cloud |

---

## Authentication & Authorization

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **Keycloak** | Self-hosted OSS | Full-featured (OIDC, SAML, LDAP, social login), admin console, customizable themes, realm multi-tenancy | Resource-heavy (Java), operational burden, complex clustering | Enterprise self-hosted, multi-tenant SaaS, regulatory requirements for data sovereignty |
| **Auth0** | Managed SaaS | Easy integration, Actions for custom logic, Universal Login, MFA, breached password detection | Expensive at scale, vendor lock-in, rate limits on free tier | Startups, teams wanting zero auth ops, rapid integration |
| **Clerk** | Managed SaaS | Beautiful pre-built UI components, developer-first API, session management, organizations | Newer product, less enterprise track record, US-based data only | Modern web/mobile apps, developer experience priority, Next.js/React apps |
| **Authentik** | Self-hosted OSS | Modern UI, flow-based authentication, proxy provider, LDAP/RADIUS outpost | Smaller community than Keycloak, Python-based (different ops profile) | Self-hosted alternative to Auth0, teams preferring Python ecosystem |
| **Authelia** | Self-hosted OSS | Lightweight, SSO portal, 2FA, reverse proxy integration (Nginx, Traefik) | Limited scope (auth portal, not full IdP), no OIDC provider built-in | Self-hosted SSO gateway, homelab/small enterprise, Traefik/Nginx auth companion |
| **AWS Cognito** | Managed | AWS-native, serverless, user pools + identity pools, Lambda triggers | Limited customization, poor UX for admin, inconsistent API, AWS lock-in | AWS-native serverless apps, simple auth needs |
| **Supabase Auth** | Managed OSS | PostgreSQL-based, row-level security integration, social login, magic links | Tied to Supabase ecosystem, less enterprise features | Supabase-based projects, PostgreSQL-centric architectures |

### Auth Decision Framework

```
Data sovereignty / self-hosted required?
  Yes → Keycloak (full IdP) or Authentik (modern alternative)
  No  ↓

Budget for managed service?
  Yes → Auth0 (enterprise) or Clerk (developer-first)
  No  ↓

AWS-native serverless?
  Yes → Cognito
  No  ↓

Simple SSO gateway needed?
  Yes → Authelia + reverse proxy
  No  → Keycloak (default enterprise choice)
```

---

## Workflow & Orchestration

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **Temporal.io** | Durable Workflow Engine | Durable execution (survives crashes), versioning, built-in retries, multi-language SDKs (Go, Java, Python, TypeScript) | Operational complexity (server + DB), learning curve for workflow concepts | Long-running business workflows, saga orchestration, payment pipelines, order fulfillment |
| **n8n** | Low-code Automation | Visual workflow builder, self-hostable, 400+ integrations, fair-code license | Not for high-throughput, limited custom logic, scaling challenges | Internal automation, integrations between SaaS tools, non-developer workflow builders |
| **Flowise** | AI Agent Orchestration | Visual LLM chain builder, LangChain-based, self-hostable, easy prototyping | AI-specific (not general workflow), limited production hardening | AI agent workflows, RAG pipelines, chatbot building, LLM chain prototyping |
| **Apache Airflow** | DAG Scheduler | Mature, Python-native, rich operator library, battle-tested at scale | Not for real-time, DAGs are acyclic only, complex deployment | Data pipeline orchestration, ETL/ELT, scheduled batch jobs |
| **Prefect** | Modern DAG Orchestration | Python-native, dynamic workflows, cloud or self-hosted, excellent observability | Smaller ecosystem than Airflow, commercial features gated | Modern data pipelines, teams wanting Airflow alternative with better DX |
| **Step Functions (AWS)** | Managed State Machine | Serverless, visual designer, Lambda integration, Express/Standard workflows | AWS lock-in, JSON state machine language (ASL) is verbose, debugging difficulty | AWS-native orchestration, serverless workflows, simple state machines |

---

## AI Agent Foundations

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **LangChain** | Agent Framework | Comprehensive (chains, agents, tools, memory), huge community, multi-LLM support | Abstraction bloat, frequent breaking changes, over-engineered for simple tasks | Complex multi-step agents, RAG systems, tool-using agents, teams needing rich ecosystem |
| **LlamaIndex** | Data Framework | Excellent for RAG, document ingestion/indexing, query engines, structured data extraction | Narrower scope than LangChain (data-focused), less agent capability | RAG pipelines, document Q&A, knowledge base search, structured data extraction |
| **CrewAI** | Multi-Agent | Role-based agent teams, task delegation, process orchestration (sequential/hierarchical) | Newer project, limited production case studies, LangChain dependency | Multi-agent collaboration, complex task decomposition, role-based AI workflows |
| **AutoGen (Microsoft)** | Multi-Agent | Multi-agent conversation, code execution, human-in-the-loop, group chat patterns | Complex setup, Microsoft-centric, evolving API | Research, multi-agent debates, code generation pipelines |
| **Anthropic Claude Agent SDK** | Agent SDK | Agentic loops with tool use, structured outputs, direct Claude integration | Claude-only, newer SDK | Claude-based agents, tool-using agents, agentic coding workflows |
| **Semantic Kernel (Microsoft)** | Orchestration | Plugin architecture, memory/planning, multi-LLM, .NET/Python/Java | Microsoft-centric, less community than LangChain | .NET enterprise shops, Azure OpenAI integration, plugin-based agents |

---

## Data Lake & Analytics

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **Apache Iceberg** | Table Format | ACID on data lakes, schema evolution, time travel, partition evolution, engine-agnostic | Requires compute engine (Spark/Trino/Flink), metadata management overhead | Open data lakehouse, multi-engine analytics, replacing Hive tables |
| **Delta Lake (Databricks)** | Table Format | ACID, Spark-native, Z-ordering, Liquid clustering, Unity Catalog | Databricks-centric (OSS version is subset), Spark dependency | Databricks shops, Spark-heavy pipelines, unified batch+streaming |
| **Apache Hudi** | Table Format | Upserts/deletes on data lakes, incremental processing, record-level indexing | More complex than Iceberg, smaller community, heavier Spark dependency | CDC on data lakes, incremental ETL, record-level updates at scale |
| **Databricks** | Unified Platform | Lakehouse platform, MLflow, Unity Catalog, collaborative notebooks, SQL analytics | Expensive, vendor lock-in, overkill for simple analytics | End-to-end data + ML platform, large data teams, enterprise analytics |
| **Snowflake** | Cloud Data Warehouse | Separation of storage/compute, near-zero ops, excellent SQL, data sharing | Expensive at large scale, less flexible than lakehouse for ML | SQL-centric analytics, data sharing, teams wanting managed warehouse |
| **BigQuery (Google)** | Cloud Data Warehouse | Serverless, ML built-in (BQML), geospatial, streaming ingestion | GCP lock-in, cost unpredictable with on-demand, less flexible transformations | GCP-native analytics, ad-hoc queries, ML on SQL data |

---

## BI & Visualization

| Technology | Type | Strengths | Weaknesses | Best For |
|-----------|------|-----------|------------|----------|
| **Apache Superset** | Open-source BI | Free, rich visualizations, SQL Lab, dashboard sharing, extensible | Setup complexity, limited enterprise features (no scheduling in OSS), Python-based | Self-hosted BI, budget-conscious teams, SQL-savvy analysts |
| **Metabase** | Open-source BI | Easy setup, non-technical user friendly, embedded analytics, Q&A in natural language | Limited for complex analyses, fewer chart types, scaling challenges | Simple dashboards for business users, embedded analytics, startups |
| **Power BI** | Enterprise BI (Microsoft) | Deep Excel/Office integration, DAX language, Azure-native, enterprise governance | Windows-centric, DAX learning curve, licensing complexity | Microsoft shops, Excel-heavy organizations, Azure environments |
| **Tableau** | Enterprise BI | Best-in-class visualizations, drag-and-drop, Prep Builder, server/cloud options | Expensive, Salesforce ownership concerns, resource-heavy | Data exploration, executive dashboards, organizations valuing visual analytics |
| **Looker (Google)** | Enterprise BI | LookML modeling layer, embedded analytics, GCP-native, version-controlled metrics | GCP-centric, LookML learning curve, pricing | GCP environments, metric governance, embedded analytics in products |
| **Grafana** | Monitoring + Dashboards | Time-series excellence, 150+ data sources, alerting, free OSS version | Not a traditional BI tool, limited for non-time-series analysis | Infrastructure monitoring, operational dashboards, DevOps metrics |

---

## Technology Decision Matrix Template

Use this template when documenting a technology choice in the Solution Architecture Document or as an ADR:

```markdown
## Decision: [Category] — [What are we choosing?]

### Context
[Why does this decision matter? What problem are we solving?]

### Requirements
| Requirement | Weight (1-5) | Notes |
|------------|-------------|-------|
| Team expertise | 5 | Current team knows [X] |
| Performance (latency) | 4 | Need <[X]ms p95 |
| Operational complexity | 3 | Small ops team |
| Cost | 3 | [Budget constraint] |
| Ecosystem / libraries | 3 | Need [specific libraries] |
| Scalability | 4 | Must handle [X] RPS |
| Compliance | 5 | [Specific regulation] |
| Multi-cloud portability | 2 | [Lock-in tolerance] |

### Evaluation

| Option | Expertise (5) | Perf (4) | Ops (3) | Cost (3) | Ecosystem (3) | Scale (4) | Compliance (5) | Portability (2) | **Weighted Score** |
|--------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Option A | 4 | 5 | 3 | 4 | 5 | 5 | 4 | 4 | **X.XX** |
| Option B | 5 | 3 | 4 | 3 | 4 | 3 | 5 | 3 | **X.XX** |
| Option C | 2 | 5 | 2 | 4 | 3 | 5 | 3 | 5 | **X.XX** |

### Decision
**Chosen: [Option X]**

### Rationale
[Why this option won, connected to weighted requirements]

### Rejected Alternatives
- **Option Y**: [Why not — specific weakness that mattered]
- **Option Z**: [Why not]
```
