# RuleForge — Scalable Rule Engine Platform
## Comprehensive Architecture Design Document

**Version:** 2.0
**Date:** February 2026
**Author:** Enterprise Architecture Team
**Status:** Draft for Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Competitive Analysis & Positioning](#2-competitive-analysis--positioning)
3. [Architecture Principles & Constraints](#3-architecture-principles--constraints)
4. [System Context (C4 Level 1)](#4-system-context-c4-level-1)
5. [Container Architecture (C4 Level 2)](#5-container-architecture-c4-level-2)
6. [Component Architecture (C4 Level 3)](#6-component-architecture-c4-level-3)
7. [Core Domain Model](#7-core-domain-model)
8. [Rule Types & Execution Engine](#8-rule-types--execution-engine)
9. [API Design](#9-api-design)
10. [Data Architecture](#10-data-architecture)
11. [Multi-Tenancy Architecture](#11-multi-tenancy-architecture)
12. [Scalability & Performance Architecture](#12-scalability--performance-architecture)
13. [Security Architecture](#13-security-architecture)
14. [Deployment Architecture](#14-deployment-architecture)
15. [Event-Driven Architecture & Audit](#15-event-driven-architecture--audit)
16. [Analytics & BI Architecture](#16-analytics--bi-architecture)
17. [AI Assistant Architecture](#17-ai-assistant-architecture)
18. [SDK & Integration Architecture](#18-sdk--integration-architecture)
19. [Technology Stack](#19-technology-stack)
20. [Development Roadmap](#20-development-roadmap)
21. [Appendices](#21-appendices)

---

## 1. Executive Summary

### 1.1 Vision

RuleForge is an enterprise-grade Business Rules Management System (BRMS) designed to compete with platforms like DecisionRules.io, GoRules, and traditional engines like IBM ODM. It enables organizations to externalize, manage, and execute business rules through an intuitive visual interface while delivering sub-100ms execution performance at scale.

### 1.2 Key Differentiators

| Capability | DecisionRules.io | IBM ODM | RuleForge (Target) |
|---|---|---|---|
| **Deployment** | SaaS / Managed Cloud / On-Prem | On-Prem / OpenShift | SaaS / Managed / On-Prem / Embedded |
| **Rule Types** | Tables, Trees, Scripts, Flows | BAL, Tables, Flows | Tables, Trees, Scripts, Flows, DMN, CEP |
| **Performance** | ~100K decisions/min | Varies by infra | 500K+ decisions/min |
| **No-Code Editor** | Yes | Limited | Yes (advanced) |
| **AI Assistant** | Basic (Gemini) | None native | LLM-powered (multi-model) |
| **Open Standards** | Proprietary JSON | Proprietary | DMN 1.4 + proprietary |
| **Embeddable Engine** | No (API-only) | Java EE only | Lightweight JAR + WASM |
| **Event Processing** | No | Limited | CEP with Kafka Streams |
| **Database Connector** | Roadmap | Yes | Native (SQL + NoSQL) |
| **Pricing** | €49+/month | $100K+/year | Freemium + Enterprise |

### 1.3 Target Use Cases

- **Financial Services:** Credit scoring, fraud detection, KYC/AML compliance, loan origination
- **Insurance:** Underwriting, claims processing, premium calculation, policy rating
- **E-Commerce:** Dynamic pricing, promotion rules, product categorization, eligibility
- **Healthcare:** Clinical decision support, eligibility verification, billing rules
- **Logistics:** Route optimization rules, shipping rate calculation, customs compliance

---

## 2. Competitive Analysis & Positioning

### 2.1 DecisionRules.io Feature Matrix

Based on research, DecisionRules.io provides:

**Rule Types:**
- Decision Tables (spreadsheet-style, Excel import/export)
- Decision Trees (visual branching logic)
- Scripting Rules (JavaScript with Monaco editor)
- Rule Flows (orchestration of multiple rules)
- Integration Flows (batch processing with external data)
- Lookup Tables (reference data)

**Platform Features:**
- Multi-tenant SaaS with 30+ AWS regions
- MongoDB/CosmosDB storage backend
- Microservices architecture (independent scaling)
- Rule versioning with rollback
- Test Bench for rule testing
- Execution strategies: STANDARD, ARRAY, FIRST_MATCH
- Bulk solve (batch input processing)
- Kafka Solver API
- Management API (CRUD, folder structure, tags)
- SAML SSO (Entra ID, Okta, OAuth)
- Organization/Team/Space hierarchy
- Power BI integration for analytics
- AI Assistant (Gemini-based table generation)
- Rule Lock and Teamwork Indicator
- Audit logs and event timeline
- Webhook support
- Multi-language SDKs (JS/TS, Python, PHP, Go, Java, C#)

**Gaps We Can Exploit:**
1. No DMN standard compliance
2. No embeddable engine (API-only)
3. Limited Complex Event Processing
4. No real-time rule debugging/tracing
5. Basic AI capabilities (only Gemini for table gen)
6. No visual diff for rule versions
7. No A/B testing for rules
8. No built-in database connector (still on roadmap)
9. No rule simulation with historical data
10. No WASM-based client-side execution

---

## 3. Architecture Principles & Constraints

### 3.1 Architecture Principles

| # | Principle | Rationale |
|---|---|---|
| AP-1 | **API-First Design** | All functionality exposed via REST/gRPC APIs; UI is just another consumer |
| AP-2 | **Separation of Engine & Platform** | Rule execution engine is decoupled and embeddable independently |
| AP-3 | **Event Sourcing for Rules** | All rule changes are immutable events, enabling full audit trail |
| AP-4 | **Stateless Execution** | Rule solver services are stateless for horizontal scaling |
| AP-5 | **Cloud-Native & Kubernetes-First** | Designed for containerized deployment with operator pattern |
| AP-6 | **Multi-Tenancy by Design** | Tenant isolation at data layer with shared compute |
| AP-7 | **Open Standards Where Possible** | Support DMN 1.4, OpenAPI 3.1, CloudEvents |
| AP-8 | **Progressive Complexity** | Simple things easy, complex things possible |
| AP-9 | **Zero-Downtime Deployments** | Blue-green deployments, rule hot-reloading |
| AP-10 | **Observability Built-In** | Structured logging, distributed tracing, metrics from day one |

### 3.2 Quality Attribute Requirements

| Attribute | Target | Measurement |
|---|---|---|
| **Availability** | 99.95% | Monthly uptime |
| **Latency (P50)** | < 10ms | Single rule execution |
| **Latency (P99)** | < 100ms | Single rule execution |
| **Throughput** | 500K decisions/min per node | Load test |
| **Scalability** | Linear to 100 nodes | Horizontal scale test |
| **Recovery** | RTO < 5min, RPO < 1min | DR drill |
| **Security** | SOC 2 Type II ready | Compliance audit |

---

## 4. System Context (C4 Level 1)

```mermaid
C4Context
    title System Context Diagram — RuleForge Platform

    Person(businessUser, "Business User", "Creates and manages rules via visual editor")
    Person(developer, "Developer", "Integrates rules via API/SDK, creates scripting rules")
    Person(admin, "Platform Admin", "Manages tenants, users, and platform configuration")

    System(ruleforge, "RuleForge Platform", "Business Rules Management System providing rule authoring, versioning, execution, and analytics")

    System_Ext(clientApp, "Client Applications", "Internal/external apps consuming rule decisions via REST/gRPC/Kafka")
    System_Ext(idp, "Identity Provider", "SSO via SAML/OIDC (Entra ID, Okta, Auth0)")
    System_Ext(monitoring, "Monitoring Stack", "Prometheus, Grafana, ELK for observability")
    System_Ext(cicd, "CI/CD Pipeline", "GitOps workflow for rule promotion")
    System_Ext(bi, "BI Tools", "Power BI, Metabase, Grafana for decision analytics")
    System_Ext(kafka, "Event Streaming", "Apache Kafka for async rule solving and events")
    System_Ext(database, "External Databases", "Client databases for data enrichment during execution")

    Rel(businessUser, ruleforge, "Authors rules, reviews analytics")
    Rel(developer, ruleforge, "Integrates via SDK/API, scripts rules")
    Rel(admin, ruleforge, "Configures platform")
    Rel(clientApp, ruleforge, "Evaluates rules", "REST/gRPC/Kafka")
    Rel(ruleforge, idp, "Authenticates users", "SAML/OIDC")
    Rel(ruleforge, monitoring, "Exports metrics & traces", "OTLP/Prometheus")
    Rel(ruleforge, cicd, "Webhooks on rule publish")
    Rel(ruleforge, bi, "Exposes decision data", "SQL/API")
    Rel(ruleforge, kafka, "Publishes/consumes events")
    Rel(ruleforge, database, "Enriches data during execution", "JDBC/HTTP")
```

---

## 5. Container Architecture (C4 Level 2)

```mermaid
C4Container
    title Container Diagram — RuleForge Platform

    Person(user, "Users", "Business users, developers, admins")

    Container_Boundary(platform, "RuleForge Platform") {
        Container(webApp, "Web Application", "React 18 + TypeScript", "Visual rule editor, dashboards, admin console")
        Container(apiGateway, "API Gateway", "Kong / Envoy", "Authentication, rate limiting, routing, TLS termination")

        Container(solverSvc, "Solver Service", "Java 21 + Spring Boot", "Stateless rule execution engine. Horizontally scalable.")
        Container(mgmtSvc, "Management Service", "Java 21 + Spring Boot", "Rule CRUD, versioning, import/export, folder management")
        Container(authSvc, "Auth Service", "Java 21 + Spring Boot", "User management, RBAC, API key management, SSO")
        Container(analyticsSvc, "Analytics Service", "Java 21 + Spring Boot", "Decision logging, aggregation, reporting APIs")
        Container(eventSvc, "Event Service", "Java 21 + Spring Boot", "Audit trail, webhooks, event streaming, notifications")
        Container(aiSvc, "AI Assistant Service", "Python + FastAPI", "LLM-powered rule generation, optimization suggestions")
        Container(schedulerSvc, "Scheduler Service", "Java 21 + Spring Boot", "Batch/bulk processing, Integration Flows, cron jobs")
        Container(connectorSvc, "Connector Service", "Java 21 + Spring Boot", "Database connectors, HTTP callouts, external data enrichment")

        ContainerDb(pgDb, "PostgreSQL", "PostgreSQL 16", "Rule definitions, user data, tenant config, audit logs")
        ContainerDb(redisCache, "Redis Cluster", "Redis 7", "Compiled rule cache, session cache, rate limiting")
        ContainerDb(esIndex, "Elasticsearch", "ES 8.x", "Decision logs, full-text search, analytics aggregations")
        ContainerDb(s3Storage, "Object Storage", "S3/MinIO", "Rule exports, attachments, large batch results")

        Container(kafkaBroker, "Kafka", "Apache Kafka", "Async rule solving, event bus, CDC")
    }

    Rel(user, webApp, "HTTPS")
    Rel(user, apiGateway, "REST/gRPC")
    Rel(webApp, apiGateway, "API calls")
    Rel(apiGateway, solverSvc, "Rule execution")
    Rel(apiGateway, mgmtSvc, "Rule management")
    Rel(apiGateway, authSvc, "Auth")
    Rel(apiGateway, analyticsSvc, "Analytics queries")
    Rel(apiGateway, aiSvc, "AI features")
    Rel(solverSvc, redisCache, "Load compiled rules")
    Rel(solverSvc, kafkaBroker, "Publish decisions")
    Rel(solverSvc, connectorSvc, "Data enrichment")
    Rel(mgmtSvc, pgDb, "CRUD")
    Rel(mgmtSvc, redisCache, "Invalidate cache")
    Rel(mgmtSvc, kafkaBroker, "Publish rule changes")
    Rel(authSvc, pgDb, "User/tenant data")
    Rel(analyticsSvc, esIndex, "Query decisions")
    Rel(eventSvc, kafkaBroker, "Consume events")
    Rel(eventSvc, pgDb, "Audit logs")
    Rel(schedulerSvc, solverSvc, "Batch execution")
    Rel(schedulerSvc, s3Storage, "Store results")
```

### 5.1 Service Responsibilities

| Service | Responsibilities | Scaling Strategy |
|---|---|---|
| **Solver Service** | Rule evaluation, validation, test bench | Horizontal (CPU-bound) |
| **Management Service** | Rule CRUD, versioning, import/export, folders, tags | Horizontal (I/O-bound) |
| **Auth Service** | Users, tenants, orgs, API keys, RBAC, SSO | Horizontal (stateless) |
| **Analytics Service** | Decision logs, aggregation, reporting | Horizontal (read-heavy) |
| **Event Service** | Audit trail, webhooks, notifications | Horizontal (event-driven) |
| **AI Assistant Service** | Rule generation, optimization, natural language | Vertical (GPU/LLM) |
| **Scheduler Service** | Batch solve, integration flows, cron triggers | Horizontal (worker pool) |
| **Connector Service** | DB connectors, HTTP callouts, data transforms | Horizontal (I/O-bound) |

---

## 6. Component Architecture (C4 Level 3)

### 6.1 Solver Service — Internal Components

```mermaid
graph TB
    subgraph "Solver Service"
        API[REST/gRPC Controller]
        Router[Rule Router]
        Cache[Rule Cache Manager]

        subgraph "Execution Engines"
            DTE[Decision Table Engine]
            TRE[Decision Tree Engine]
            SCE[Scripting Rule Engine]
            FLE[Flow Orchestrator]
            DMN[DMN Engine]
            CEP[CEP Engine]
        end

        subgraph "Rule Pipeline"
            VAL[Input Validator]
            ENR[Data Enricher]
            EXEC[Rule Executor]
            TRANS[Output Transformer]
            AUDIT[Decision Logger]
        end

        subgraph "Support"
            SANDBOX[Script Sandbox / GraalVM]
            CONN[Connector Client]
            METRICS[Metrics Collector]
        end
    end

    API --> Router
    Router --> Cache
    Cache --> VAL
    VAL --> ENR
    ENR --> EXEC
    EXEC --> DTE & TRE & SCE & FLE & DMN & CEP
    EXEC --> TRANS
    TRANS --> AUDIT
    SCE --> SANDBOX
    ENR --> CONN
    EXEC --> METRICS
```

### 6.2 Rule Execution Pipeline

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant S as Solver Service
    participant RC as Redis Cache
    participant DB as PostgreSQL
    participant K as Kafka
    participant ES as Elasticsearch

    C->>GW: POST /api/v1/solve/{ruleId}
    GW->>GW: Authenticate (JWT/API Key)
    GW->>GW: Rate limit check
    GW->>S: Forward request

    S->>RC: Get compiled rule
    alt Cache HIT
        RC-->>S: Return compiled rule
    else Cache MISS
        S->>DB: Load rule definition
        DB-->>S: Rule JSON
        S->>S: Compile & optimize rule
        S->>RC: Store compiled rule (TTL)
        RC-->>S: OK
    end

    S->>S: Validate input against schema
    S->>S: Execute rule pipeline
    Note over S: Strategy: STANDARD | FIRST_MATCH | ARRAY

    opt Data Enrichment Required
        S->>S: Call Connector Service
    end

    S->>S: Transform output
    S-->>C: Return results

    S--)K: Publish decision event (async)
    K--)ES: Index for analytics
```

---

## 7. Core Domain Model

### 7.1 Domain Entities

```mermaid
erDiagram
    ORGANIZATION ||--o{ SPACE : contains
    ORGANIZATION ||--o{ TEAM : has
    ORGANIZATION ||--o{ MEMBER : has
    TEAM ||--o{ MEMBER : includes
    SPACE ||--o{ FOLDER : contains
    SPACE ||--o{ API_KEY : has
    FOLDER ||--o{ FOLDER : contains
    FOLDER ||--o{ RULE : contains
    RULE ||--o{ RULE_VERSION : has
    RULE_VERSION ||--|{ RULE_INPUT_MODEL : defines
    RULE_VERSION ||--|{ RULE_OUTPUT_MODEL : defines
    RULE_VERSION ||--o{ RULE_TEST : has
    RULE_VERSION ||--o{ RULE_TAG : tagged_with
    RULE_VERSION ||--o{ AUDIT_EVENT : generates
    RULE ||--o{ RULE_DEPENDENCY : depends_on
    FLOW ||--o{ FLOW_NODE : contains
    FLOW_NODE ||--o| RULE : references
    DECISION_LOG ||--|| RULE_VERSION : executed_by

    ORGANIZATION {
        uuid id PK
        string name
        string slug
        jsonb settings
        timestamp created_at
    }

    SPACE {
        uuid id PK
        uuid org_id FK
        string name
        string description
        jsonb limits
        timestamp created_at
    }

    RULE {
        uuid id PK
        uuid space_id FK
        string name
        string alias UK
        enum type "TABLE|TREE|SCRIPT|FLOW|DMN|CEP"
        string description
        int latest_version
        enum status "DRAFT|PUBLISHED|ARCHIVED"
        boolean locked
        uuid locked_by FK
        timestamp created_at
        timestamp updated_at
    }

    RULE_VERSION {
        uuid id PK
        uuid rule_id FK
        int version
        jsonb definition
        jsonb input_model
        jsonb output_model
        jsonb compiled_bytecode
        enum status "DRAFT|PENDING|PUBLISHED|DEPRECATED"
        uuid created_by FK
        string change_note
        timestamp created_at
        timestamp published_at
    }

    DECISION_LOG {
        uuid id PK
        uuid rule_id FK
        int rule_version
        uuid space_id FK
        string correlation_id
        jsonb input_data
        jsonb output_data
        int execution_time_ms
        enum strategy "STANDARD|FIRST_MATCH|ARRAY"
        timestamp executed_at
    }

    API_KEY {
        uuid id PK
        uuid space_id FK
        string key_hash
        string key_prefix
        enum type "SOLVER|MANAGEMENT"
        jsonb permissions
        timestamp expires_at
        timestamp created_at
    }

    FLOW {
        uuid id PK
        uuid rule_id FK
        jsonb canvas_state
        jsonb node_definitions
    }

    FLOW_NODE {
        uuid id PK
        uuid flow_id FK
        enum type "START|END|RULE|CONDITION|LOOP|AGGREGATE|HTTP|SCRIPT"
        jsonb config
        jsonb mapping
        int position_x
        int position_y
    }
```

### 7.2 Rule Definition JSON Schema

```json
{
  "$schema": "https://ruleforge.io/schema/rule/v1",
  "type": "decision-table",
  "version": 1,
  "inputModel": {
    "customer": {
      "type": "object",
      "properties": {
        "age": { "type": "integer" },
        "income": { "type": "number" },
        "creditScore": { "type": "integer" },
        "employmentType": { "type": "string", "enum": ["FULL_TIME", "PART_TIME", "SELF_EMPLOYED", "UNEMPLOYED"] }
      }
    }
  },
  "outputModel": {
    "decision": {
      "type": "object",
      "properties": {
        "eligible": { "type": "boolean" },
        "maxLoanAmount": { "type": "number" },
        "interestRate": { "type": "number" },
        "riskCategory": { "type": "string" }
      }
    }
  },
  "executionStrategy": "FIRST_MATCH",
  "table": {
    "columns": [
      { "id": "col_1", "type": "INPUT", "path": "customer.age", "operator": "BETWEEN" },
      { "id": "col_2", "type": "INPUT", "path": "customer.creditScore", "operator": "GTE" },
      { "id": "col_3", "type": "INPUT", "path": "customer.employmentType", "operator": "IN" },
      { "id": "col_4", "type": "OUTPUT", "path": "decision.eligible" },
      { "id": "col_5", "type": "OUTPUT", "path": "decision.maxLoanAmount" },
      { "id": "col_6", "type": "OUTPUT", "path": "decision.riskCategory" }
    ],
    "rows": [
      {
        "id": "row_1",
        "active": true,
        "cells": {
          "col_1": { "value": [25, 65] },
          "col_2": { "value": 750 },
          "col_3": { "value": ["FULL_TIME"] },
          "col_4": { "value": true },
          "col_5": { "value": 500000 },
          "col_6": { "value": "LOW" }
        }
      },
      {
        "id": "row_2",
        "active": true,
        "cells": {
          "col_1": { "value": [25, 65] },
          "col_2": { "value": 650 },
          "col_3": { "value": ["FULL_TIME", "PART_TIME"] },
          "col_4": { "value": true },
          "col_5": { "value": 200000 },
          "col_6": { "value": "MEDIUM" }
        }
      }
    ]
  }
}
```

---

## 8. Rule Types & Execution Engine

### 8.1 Supported Rule Types

```mermaid
mindmap
  root((Rule Types))
    Decision Table
      Spreadsheet-style
      Excel/CSV import-export
      Operators: EQ, NEQ, GT, GTE, LT, LTE, BETWEEN, IN, CONTAINS, REGEX, IS_NULL
      Functions: Math, Date, String, Array, Logical
      Strategies: STANDARD, FIRST_MATCH, ARRAY
    Decision Tree
      Visual branching logic
      IF-THEN-ELSE chains
      Nested conditions
      Drag-and-drop editor
    Scripting Rule
      JavaScript (GraalVM sandboxed)
      Monaco editor with IntelliSense
      Access to input/output models
      Custom function library
      Call embedded rules (DR.solve)
    Rule Flow / Workflow
      Orchestrate multiple rules
      Conditional branching
      Parallel execution
      Loop/ForEach
      Aggregation nodes
      HTTP callout nodes
      Script transform nodes
    DMN (Decision Model & Notation)
      Standard DMN 1.4
      FEEL expression language
      Import/export standard XML
      Interop with Camunda/jBPM
    Complex Event Processing
      Temporal patterns
      Sliding windows
      Event correlation
      Kafka Streams integration
    Lookup Table
      Reference data storage
      Key-value pairs
      Used within other rules
```

### 8.2 Execution Engine Architecture

The core engine uses a **compiled rule** approach for performance:

```
Rule Definition (JSON) → Compiler → Compiled Rule (Bytecode/AST) → Cache → Execute
```

**Per rule type:**

| Rule Type | Compilation Strategy | Execution Model |
|---|---|---|
| Decision Table | Compile to optimized Rete-like network with indexed conditions | Row-scan with short-circuit (FIRST_MATCH) or full-scan (STANDARD) |
| Decision Tree | Compile to optimized binary decision diagram (BDD) | Tree traversal with lazy evaluation |
| Scripting Rule | Compile to GraalVM polyglot context | Sandboxed JS execution with CPU/memory limits |
| Rule Flow | Compile to DAG with topological sort | Parallel executor with Futures |
| DMN | Parse FEEL → compile to expression tree | Standard DMN evaluation |
| CEP | Compile pattern to state machine | Kafka Streams processor |

### 8.3 JEasyRules Integration Points

While the architecture is engine-agnostic, JEasyRules can serve as the **foundation for simple rule evaluation**:

```java
// Core engine abstraction
public interface RuleEngine {
    RuleResult evaluate(CompiledRule rule, RuleInput input, ExecutionContext context);
    CompiledRule compile(RuleDefinition definition);
    ValidationResult validate(RuleDefinition definition);
}

// JEasyRules adapter for basic rules
public class JEasyRulesAdapter implements RuleEngine {
    private final RulesEngine easyRulesEngine;

    @Override
    public RuleResult evaluate(CompiledRule rule, RuleInput input, ExecutionContext ctx) {
        Facts facts = new Facts();
        facts.put("input", input.getData());
        facts.put("output", new HashMap<>());
        facts.put("context", ctx);

        Rules rules = rule.getEasyRules();
        easyRulesEngine.fire(rules, facts);

        return new RuleResult(facts.get("output"));
    }
}

// High-performance compiled engine for Decision Tables
public class CompiledTableEngine implements RuleEngine {
    // Uses pre-compiled condition indexes for O(1) lookups
    // Falls back to linear scan for complex operators
}
```

**Recommendation:** Use JEasyRules as the **simple rule adapter** but build custom high-performance engines for Decision Tables and Trees, as JEasyRules' linear evaluation model won't meet the 500K decisions/min target for complex tables.

---

## 9. API Design

### 9.1 API Overview

```
Base URL: https://api.ruleforge.io/v1

Authentication:
  - Bearer Token (JWT) for UI sessions
  - API Key (x-api-key header) for machine-to-machine
  - API keys scoped: SOLVER (execute only) vs MANAGEMENT (CRUD)
```

### 9.2 Solver API (Rule Execution)

```yaml
# Execute a rule (single input)
POST /v1/solve/{ruleIdOrAlias}/{version?}
Headers:
  Authorization: Bearer <solver-api-key>
  Content-Type: application/json
  X-Correlation-Id: <optional>
  X-Audit: true|false
  X-Strategy: STANDARD|FIRST_MATCH|ARRAY
Body:
  {
    "data": {
      "customer": { "age": 35, "creditScore": 720, "employmentType": "FULL_TIME" }
    },
    "options": {
      "strategy": "FIRST_MATCH",
      "excludeColumns": ["col_5"],
      "audit": true
    }
  }
Response 200:
  {
    "correlationId": "abc-123",
    "ruleId": "rule-uuid",
    "version": 2,
    "executionTimeMs": 3,
    "results": [
      {
        "decision": { "eligible": true, "maxLoanAmount": 200000, "riskCategory": "MEDIUM" }
      }
    ]
  }

# Bulk solve (multiple inputs)
POST /v1/solve/bulk/{ruleIdOrAlias}/{version?}
Body:
  {
    "data": [
      { "customer": { "age": 35, "creditScore": 720 } },
      { "customer": { "age": 22, "creditScore": 580 } }
    ]
  }
Response 200:
  {
    "correlationId": "abc-124",
    "results": [
      [{ "decision": { "eligible": true, "riskCategory": "MEDIUM" } }],
      [{ "decision": { "eligible": false, "riskCategory": "HIGH" } }]
    ]
  }

# Async solve via Kafka
POST /v1/solve/async/{ruleIdOrAlias}
Body:
  {
    "data": { ... },
    "callbackUrl": "https://client.app/webhook/result",
    "callbackHeaders": { "Authorization": "Bearer client-token" }
  }
Response 202:
  { "jobId": "job-uuid", "status": "QUEUED" }
```

### 9.3 Management API (Rule CRUD)

```yaml
# Rules
GET    /v1/rules                           # List rules (paginated, filterable)
POST   /v1/rules                           # Create rule
GET    /v1/rules/{id}                      # Get rule metadata
PUT    /v1/rules/{id}                      # Update rule metadata
DELETE /v1/rules/{id}                      # Delete rule (soft delete)

# Rule Versions
GET    /v1/rules/{id}/versions             # List versions
POST   /v1/rules/{id}/versions             # Create new version (clone latest)
GET    /v1/rules/{id}/versions/{v}         # Get specific version definition
PUT    /v1/rules/{id}/versions/{v}         # Update version definition
POST   /v1/rules/{id}/versions/{v}/publish # Publish version
POST   /v1/rules/{id}/versions/{v}/deprecate
GET    /v1/rules/{id}/versions/{v1}/diff/{v2} # Visual diff between versions

# Rule Testing
POST   /v1/rules/{id}/versions/{v}/test   # Run test bench
POST   /v1/rules/{id}/versions/{v}/validate # Validate rule definition

# Folders & Organization
GET    /v1/folders/{nodeId?}               # Get folder tree
POST   /v1/folders                         # Create folder
PUT    /v1/folders/{id}                    # Update folder / move items
DELETE /v1/folders/{id}                    # Delete folder

# Tags
GET    /v1/tags                            # List all tags in space
POST   /v1/rules/{id}/tags                 # Add tags
DELETE /v1/rules/{id}/tags                 # Remove tags

# Import / Export
POST   /v1/rules/import                    # Import rules (JSON/Excel/DMN)
GET    /v1/rules/{id}/export?format=json|excel|dmn # Export rule
POST   /v1/spaces/export                   # Export entire space
POST   /v1/spaces/import                   # Import space snapshot

# Rule Lock
POST   /v1/rules/{id}/lock                 # Lock rule for editing
DELETE /v1/rules/{id}/lock                 # Unlock rule

# Webhooks
GET    /v1/webhooks                        # List webhooks
POST   /v1/webhooks                        # Create webhook
PUT    /v1/webhooks/{id}                   # Update webhook
DELETE /v1/webhooks/{id}                   # Delete webhook
```

### 9.4 Analytics API

```yaml
GET    /v1/analytics/decisions              # Query decision logs
GET    /v1/analytics/decisions/aggregate    # Aggregated stats
GET    /v1/analytics/rules/{id}/stats       # Per-rule statistics
GET    /v1/analytics/rules/{id}/heatmap     # Row hit frequency heatmap
POST   /v1/analytics/reports                # Generate custom report
GET    /v1/analytics/dashboard              # Dashboard metrics
```

### 9.5 AI Assistant API

```yaml
POST   /v1/ai/generate-table               # Generate decision table from natural language
POST   /v1/ai/suggest-optimization          # Suggest rule optimization
POST   /v1/ai/explain-rule                  # Explain rule in natural language
POST   /v1/ai/generate-test-data            # Generate test data for rule
POST   /v1/ai/detect-conflicts              # Detect conflicting rules
POST   /v1/ai/natural-language-query        # Query rules in natural language
```

---

## 10. Data Architecture

### 10.1 Storage Strategy

```mermaid
graph LR
    subgraph "Hot Path (Real-Time)"
        Redis[("Redis Cluster<br/>• Compiled rules cache<br/>• Rate limiting counters<br/>• Session tokens<br/>• Feature flags")]
    end

    subgraph "Warm Path (Operational)"
        PG[("PostgreSQL 16<br/>• Rule definitions<br/>• User/tenant data<br/>• API keys<br/>• Audit events<br/>• Workflow state")]
    end

    subgraph "Cold Path (Analytics)"
        ES[("Elasticsearch 8<br/>• Decision logs<br/>• Full-text search<br/>• Aggregations<br/>• Dashboards")]
    end

    subgraph "Archive Path (Storage)"
        S3[("S3 / MinIO<br/>• Rule exports<br/>• Batch results<br/>• Backups<br/>• Large payloads")]
    end

    Solver -->|Read| Redis
    Solver -->|Fallback| PG
    Solver -->|Log| ES
    Mgmt -->|CRUD| PG
    Mgmt -->|Invalidate| Redis
    Analytics -->|Query| ES
    Scheduler -->|Results| S3
```

### 10.2 Why PostgreSQL over MongoDB

DecisionRules.io uses MongoDB, but for RuleForge we recommend PostgreSQL:

| Criteria | PostgreSQL | MongoDB |
|---|---|---|
| **JSONB Support** | Native, indexed, queryable | Native JSON |
| **ACID Transactions** | Full multi-table | Limited multi-document |
| **Rule Versioning** | Strong with CTEs, window functions | Requires careful schema |
| **Audit Trail** | Triggers + event sourcing | Change streams |
| **Multi-Tenancy** | Row-level security (RLS) | Database-per-tenant or field |
| **Analytics Queries** | Advanced aggregation, CTEs | Aggregation pipeline |
| **Operational Maturity** | Battle-tested in banking | Good, but less in regulated |
| **JSON Schema Validation** | CHECK constraints on JSONB | Built-in validation |

**Decision:** PostgreSQL with JSONB columns for rule definitions gives us the best of both worlds — flexible JSON storage with relational integrity, transactions, and row-level security for multi-tenancy.

### 10.3 Caching Strategy

```mermaid
graph TB
    subgraph "Multi-Layer Cache"
        L1["L1: In-Process (Caffeine)<br/>Per JVM, 1000 rules<br/>TTL: 60s, Eviction: LRU"]
        L2["L2: Distributed (Redis)<br/>Shared cluster<br/>TTL: 5min, Eviction: LFU"]
        L3["L3: Source (PostgreSQL)<br/>Rule definitions<br/>Always fresh"]
    end

    L1 -->|Miss| L2
    L2 -->|Miss| L3

    subgraph "Invalidation"
        PUB["Kafka Topic: rule.updated"]
        INV1["Invalidate L2 key"]
        INV2["Broadcast L1 invalidation"]
    end

    PUB --> INV1
    PUB --> INV2
```

**Cache Key Pattern:**
```
rule:{spaceId}:{ruleId}:{version}:compiled
rule:{spaceId}:{ruleAlias}:latest:compiled
```

---

## 11. Multi-Tenancy Architecture

### 11.1 Tenant Hierarchy

```mermaid
graph TB
    ORG["Organization<br/>(Tenant)"]
    ORG --> SPACE1["Space: Production"]
    ORG --> SPACE2["Space: Staging"]
    ORG --> SPACE3["Space: Development"]

    SPACE1 --> FOLDER1["Folder: Lending"]
    SPACE1 --> FOLDER2["Folder: KYC"]
    FOLDER1 --> R1["Rule: Credit Scoring v3"]
    FOLDER1 --> R2["Rule: Loan Eligibility v5"]
    FOLDER2 --> R3["Rule: Customer Risk v2"]

    ORG --> TEAM1["Team: Business Analysts"]
    ORG --> TEAM2["Team: Developers"]
    ORG --> TEAM3["Team: Ops"]

    TEAM1 -.->|Read/Write Space Prod| SPACE1
    TEAM2 -.->|Admin Space Dev| SPACE3
    TEAM3 -.->|Read-Only All| SPACE1 & SPACE2 & SPACE3
```

### 11.2 Isolation Model

| Layer | Strategy | Implementation |
|---|---|---|
| **Compute** | Shared (namespaced) | Tenant ID in JWT, propagated to all services |
| **Data** | Row-Level Security | PostgreSQL RLS policies on all tables |
| **Cache** | Key-prefixed | `tenant:{orgId}:rule:{ruleId}:...` |
| **Search** | Index-per-tenant | Elasticsearch index aliases per org |
| **Storage** | Prefix-partitioned | S3 path: `/{orgId}/{spaceId}/...` |
| **Network** | Shared (throttled) | Per-tenant rate limits in API Gateway |
| **Encryption** | Per-tenant keys | AES-256 with tenant-specific KEK |

### 11.3 Row-Level Security Example

```sql
-- Enable RLS on rules table
ALTER TABLE rules ENABLE ROW LEVEL SECURITY;

-- Policy: users can only see rules in their space
CREATE POLICY rules_tenant_isolation ON rules
    USING (space_id IN (
        SELECT s.id FROM spaces s
        JOIN org_members om ON om.org_id = s.org_id
        WHERE om.user_id = current_setting('app.current_user_id')::uuid
    ));

-- Set context per request
SET app.current_user_id = '<user-uuid>';
SET app.current_org_id = '<org-uuid>';
```

---

## 12. Scalability & Performance Architecture

### 12.1 Performance Targets

| Metric | Target | Strategy |
|---|---|---|
| Single rule latency (P50) | < 5ms | In-memory compiled rules, no I/O in hot path |
| Single rule latency (P99) | < 50ms | Warm cache, connection pooling |
| Flow execution (P50) | < 50ms | Parallel node execution, cached sub-rules |
| Bulk solve 1000 inputs | < 2s | Thread pool, batch processing |
| Throughput per node | 8,000+ req/s | Async I/O, non-blocking execution |
| Cluster throughput | 500K+ decisions/min | 10-node cluster |
| Rule compilation | < 500ms | Async background compilation |

### 12.2 Horizontal Scaling Architecture

```mermaid
graph TB
    LB["Load Balancer<br/>(L7 / Envoy)"]

    subgraph "Solver Pool (Auto-Scaled)"
        S1["Solver-1<br/>CPU: 4, RAM: 8G"]
        S2["Solver-2<br/>CPU: 4, RAM: 8G"]
        S3["Solver-N<br/>CPU: 4, RAM: 8G"]
    end

    subgraph "Management Pool"
        M1["Mgmt-1"]
        M2["Mgmt-2"]
    end

    subgraph "Worker Pool (Batch)"
        W1["Worker-1"]
        W2["Worker-N"]
    end

    LB --> S1 & S2 & S3
    LB --> M1 & M2

    KAFKA["Kafka"] --> W1 & W2

    subgraph "Shared Data"
        REDIS["Redis Cluster (3-node)"]
        PG["PostgreSQL (Primary + 2 Replicas)"]
    end

    S1 & S2 & S3 --> REDIS
    M1 & M2 --> PG
    S1 & S2 & S3 -.->|Fallback| PG
```

### 12.3 Auto-Scaling Policy

```yaml
# Kubernetes HPA for Solver Service
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: solver-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: solver-service
  minReplicas: 3
  maxReplicas: 50
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 65
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "5000"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 30
      policies:
        - type: Pods
          value: 4
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 2
          periodSeconds: 120
```

### 12.4 Performance Optimization Techniques

1. **Compiled Rules:** Rules are compiled to optimized AST/bytecode on publish, not on every execution
2. **Rete-like Indexing:** Decision table conditions are indexed for O(log n) matching
3. **GraalVM Polyglot:** Scripting rules run in pre-warmed GraalVM contexts (not V8/Nashorn)
4. **Connection Pooling:** HikariCP for PostgreSQL, Lettuce for Redis
5. **Async I/O:** Non-blocking Kafka producers, async audit logging
6. **Zero-Copy:** Avoid unnecessary serialization in the execution pipeline
7. **Virtual Threads:** Java 21 virtual threads for high concurrency with low overhead
8. **Result Streaming:** Large bulk results streamed to S3, not buffered in memory

---

## 13. Security Architecture

### 13.1 Authentication & Authorization

```mermaid
graph LR
    subgraph "Authentication"
        JWT["JWT Tokens<br/>(UI Sessions)"]
        APIKEY["API Keys<br/>(Machine-to-Machine)"]
        SSO["SSO/SAML/OIDC<br/>(Enterprise)"]
    end

    subgraph "Authorization"
        RBAC["Role-Based Access Control"]
        ABAC["Attribute-Based Policies"]
        RLS["Row-Level Security"]
    end

    subgraph "Roles"
        OWNER["Owner: Full control"]
        ADMIN["Admin: Manage space & users"]
        EDITOR["Editor: Create & edit rules"]
        VIEWER["Viewer: Read-only access"]
        SOLVER["Solver: Execute only (API key)"]
    end

    JWT --> RBAC
    APIKEY --> RBAC
    SSO --> RBAC
    RBAC --> OWNER & ADMIN & EDITOR & VIEWER & SOLVER
    RBAC --> ABAC
    ABAC --> RLS
```

### 13.2 Security Controls

| Control | Implementation |
|---|---|
| **Encryption at Rest** | AES-256 for PostgreSQL (TDE), Redis encryption, S3 SSE-KMS |
| **Encryption in Transit** | TLS 1.3 everywhere, mTLS between services |
| **API Key Management** | Hashed storage (bcrypt), prefix for identification, rotation support |
| **Secret Management** | HashiCorp Vault / AWS Secrets Manager |
| **Script Sandboxing** | GraalVM isolated contexts, CPU/memory limits, no filesystem/network |
| **Input Validation** | JSON Schema validation on all inputs, SQL injection prevention |
| **Rate Limiting** | Per-tenant, per-API-key, sliding window in Redis |
| **Audit Trail** | Immutable audit log for all rule changes and decisions |
| **Vulnerability Scanning** | Trivy for containers, Snyk for dependencies |
| **SOC 2 Compliance** | Audit logging, access controls, encryption, monitoring |

### 13.3 Script Sandboxing Detail

```java
// GraalVM sandbox configuration for scripting rules
Context context = Context.newBuilder("js")
    .allowHostAccess(HostAccess.NONE)           // No Java interop
    .allowIO(false)                               // No file system
    .allowCreateThread(false)                     // No threading
    .allowNativeAccess(false)                     // No native calls
    .allowEnvironmentAccess(EnvironmentAccess.NONE) // No env vars
    .option("engine.WarnInterpreterOnly", "false")
    .resourceLimits(
        ResourceLimits.newBuilder()
            .statementLimit(100_000, null)         // Max 100K statements
            .build()
    )
    .build();

// Inject safe API
context.getBindings("js").putMember("input", inputData);
context.getBindings("js").putMember("output", outputData);
context.getBindings("js").putMember("DR", drApiProxy); // Safe rule-calling API
context.getBindings("js").putMember("Math", mathFunctions);
context.getBindings("js").putMember("Date", safeDateFunctions);
```

---

## 14. Deployment Architecture

### 14.1 Deployment Options

```mermaid
graph TB
    subgraph "Option 1: SaaS (Multi-Tenant)"
        SAAS_LB["Global LB<br/>(CloudFlare)"]
        SAAS_R1["Region: US-East"]
        SAAS_R2["Region: EU-West"]
        SAAS_R3["Region: AP-Southeast"]
        SAAS_LB --> SAAS_R1 & SAAS_R2 & SAAS_R3
    end

    subgraph "Option 2: Managed Private Cloud"
        MPC["Dedicated K8s Namespace<br/>Customer's preferred region<br/>Managed by RuleForge team"]
    end

    subgraph "Option 3: Self-Hosted (On-Premise)"
        ONPREM["Customer K8s Cluster<br/>Helm chart + Operator<br/>Air-gapped capable"]
    end

    subgraph "Option 4: Embedded Engine"
        EMBED["ruleforge-engine.jar<br/>Lightweight library<br/>No platform dependency<br/>+ WASM for browser/edge"]
    end
```

### 14.2 Kubernetes Deployment

```yaml
# Helm values.yaml (simplified)
global:
  domain: ruleforge.example.com
  storageClass: gp3-encrypted

solver:
  replicas: 3
  resources:
    requests: { cpu: "2", memory: "4Gi" }
    limits: { cpu: "4", memory: "8Gi" }
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 50
    targetCPUUtilization: 65
  env:
    CACHE_L1_SIZE: "2000"
    CACHE_L2_TTL: "300"
    GRAALVM_SCRIPT_TIMEOUT_MS: "5000"

management:
  replicas: 2
  resources:
    requests: { cpu: "1", memory: "2Gi" }

postgresql:
  primary:
    persistence:
      size: 100Gi
  readReplicas:
    replicaCount: 2

redis:
  architecture: replication
  sentinel:
    enabled: true
  master:
    persistence:
      size: 10Gi

elasticsearch:
  replicas: 3
  persistence:
    size: 200Gi

kafka:
  replicaCount: 3
  persistence:
    size: 100Gi
```

### 14.3 CI/CD & GitOps for Rules

```mermaid
graph LR
    DEV["Dev Space<br/>Edit rules in UI"] -->|Promote| STG["Staging Space<br/>Automated testing"]
    STG -->|Approve| PROD["Production Space<br/>Live traffic"]

    GIT["Git Repository<br/>Rule definitions as code"] -->|PR| CI["CI Pipeline<br/>Lint + Test + Validate"]
    CI -->|Merge| CD["CD Pipeline<br/>Deploy to target space"]
    CD --> STG

    WEBHOOK["Webhook on Publish"] --> GIT
```

---

## 15. Event-Driven Architecture & Audit

### 15.1 Event Topology

```mermaid
graph LR
    subgraph "Producers"
        MGMT["Management Service"]
        SOLVER["Solver Service"]
        AUTH["Auth Service"]
    end

    subgraph "Kafka Topics"
        T1["rule.lifecycle<br/>(created, updated, published, archived)"]
        T2["rule.execution<br/>(decisions, errors)"]
        T3["user.activity<br/>(login, actions)"]
        T4["system.events<br/>(health, alerts)"]
    end

    subgraph "Consumers"
        AUDIT["Audit Service<br/>→ PostgreSQL"]
        ANALYTICS["Analytics Service<br/>→ Elasticsearch"]
        WEBHOOK["Webhook Dispatcher<br/>→ Customer endpoints"]
        CACHE_INV["Cache Invalidator<br/>→ Redis"]
        NOTIFY["Notification Service<br/>→ Email/Slack"]
    end

    MGMT --> T1
    SOLVER --> T2
    AUTH --> T3

    T1 --> AUDIT & CACHE_INV & WEBHOOK & NOTIFY
    T2 --> ANALYTICS & AUDIT
    T3 --> AUDIT
```

### 15.2 Audit Event Schema

```json
{
  "eventId": "evt-uuid",
  "eventType": "rule.version.published",
  "timestamp": "2026-02-20T10:30:00Z",
  "actor": {
    "userId": "user-uuid",
    "email": "analyst@company.com",
    "type": "USER"
  },
  "tenant": {
    "orgId": "org-uuid",
    "spaceId": "space-uuid"
  },
  "resource": {
    "type": "RULE_VERSION",
    "ruleId": "rule-uuid",
    "ruleName": "Credit Scoring",
    "version": 3
  },
  "changes": {
    "previousStatus": "DRAFT",
    "newStatus": "PUBLISHED"
  },
  "metadata": {
    "ipAddress": "203.0.113.1",
    "userAgent": "Mozilla/5.0...",
    "correlationId": "corr-123"
  }
}
```

---

## 16. Analytics & BI Architecture

### 16.1 Decision Analytics Pipeline

```mermaid
graph LR
    SOLVER["Solver Service<br/>(Decision Events)"]
    SOLVER -->|Kafka| STREAM["Kafka Streams<br/>Real-time aggregation"]
    STREAM --> ES["Elasticsearch<br/>Decision log index"]
    STREAM --> METRICS["Prometheus<br/>Decision counters"]

    ES --> API["Analytics API"]
    ES --> KIBANA["Kibana Dashboards"]
    API --> UI["RuleForge Dashboard"]
    API --> POWERBI["Power BI<br/>(via connector)"]
    API --> METABASE["Metabase"]

    subgraph "Dashboard Metrics"
        M1["Decisions/minute"]
        M2["Avg latency"]
        M3["Rule hit rates"]
        M4["Row/branch heatmap"]
        M5["Error rates"]
        M6["A/B test results"]
    end

    UI --> M1 & M2 & M3 & M4 & M5 & M6
```

### 16.2 Built-In Analytics Features

| Feature | Description |
|---|---|
| **Decision Dashboard** | Real-time view of decision volume, latency, error rates |
| **Rule Heatmap** | Which rows/branches are triggered most frequently |
| **Rule Comparison** | Side-by-side comparison of two rule versions' decision outcomes |
| **A/B Testing** | Route traffic between rule versions with statistical analysis |
| **Impact Analysis** | "What-if" simulation: replay historical decisions against new rule version |
| **Export to BI** | ODBC/JDBC connector, Power BI dataset, API export |

---

## 17. AI Assistant Architecture

### 17.1 AI Capabilities

```mermaid
graph TB
    subgraph "AI Assistant Service"
        NLI["Natural Language Interface"]
        GEN["Rule Generator"]
        OPT["Rule Optimizer"]
        EXP["Rule Explainer"]
        TEST["Test Data Generator"]
        CONF["Conflict Detector"]
    end

    subgraph "LLM Backend"
        CLAUDE["Claude API<br/>(Primary)"]
        LOCAL["Local LLM<br/>(On-Prem option)"]
    end

    NLI --> GEN & OPT & EXP & TEST & CONF
    GEN --> CLAUDE
    GEN --> LOCAL
```

### 17.2 AI Use Cases

| Use Case | Input | Output |
|---|---|---|
| **Generate Table** | "Create a credit scoring table with age, income, and credit score" | Complete decision table JSON |
| **Optimize Rule** | Existing rule + decision logs | Suggestions for redundant rows, unreachable branches, performance |
| **Explain Rule** | Rule definition | Plain English explanation of the rule logic |
| **Generate Tests** | Rule definition | Edge case test inputs with expected outputs |
| **Detect Conflicts** | Set of rules in a flow | List of conflicting/overlapping conditions |
| **Natural Language Query** | "Show me all rules that check credit score > 700" | Filtered rule list |

---

## 18. SDK & Integration Architecture

### 18.1 Multi-Language SDK Strategy

```
SDKs generated from OpenAPI 3.1 spec using openapi-generator:

  @ruleforge/sdk-node      (TypeScript/JavaScript)
  ruleforge-sdk-python      (Python)
  ruleforge-sdk-java        (Java)
  ruleforge-sdk-go          (Go)
  ruleforge-sdk-dotnet      (C#)
  ruleforge-sdk-php         (PHP)
```

### 18.2 SDK Usage Example

```typescript
import { RuleForge } from '@ruleforge/sdk-node';

const rf = new RuleForge({
  apiKey: process.env.RULEFORGE_SOLVER_KEY,
  baseUrl: 'https://api.ruleforge.io',
});

// Simple solve
const result = await rf.solve('credit-scoring', {
  customer: { age: 35, creditScore: 720, employmentType: 'FULL_TIME' }
});

console.log(result.data);
// { eligible: true, maxLoanAmount: 200000, riskCategory: 'MEDIUM' }

// Bulk solve
const bulkResult = await rf.solveBulk('credit-scoring', [
  { customer: { age: 35, creditScore: 720 } },
  { customer: { age: 22, creditScore: 580 } },
]);

// Async solve with callback
const job = await rf.solveAsync('batch-scoring', largeDataset, {
  callbackUrl: 'https://my-app.com/webhook/result'
});
```

### 18.3 Embedded Engine

```xml
<!-- Maven dependency for embedded usage -->
<dependency>
    <groupId>io.ruleforge</groupId>
    <artifactId>ruleforge-engine</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// Embedded usage — no platform dependency
RuleForgeEngine engine = RuleForgeEngine.builder()
    .withCache(CaffeineCache.defaultConfig())
    .withScripting(GraalVMConfig.sandboxed())
    .build();

// Load rule from file or string
Rule rule = engine.loadRule(Files.readString(Path.of("credit-scoring.json")));

// Execute
RuleResult result = engine.evaluate(rule, Map.of(
    "customer", Map.of("age", 35, "creditScore", 720)
));
```

### 18.4 Integration Patterns

```mermaid
graph TB
    subgraph "Sync Integrations"
        REST["REST API<br/>(Primary)"]
        GRPC["gRPC<br/>(High-performance)"]
        GRAPHQL["GraphQL<br/>(Flexible queries)"]
    end

    subgraph "Async Integrations"
        KAFKA_IN["Kafka Consumer<br/>(Event-driven)"]
        WEBHOOK_OUT["Webhooks<br/>(Push notifications)"]
        QUEUE["Message Queue<br/>(RabbitMQ/SQS)"]
    end

    subgraph "Data Integrations"
        DB_CONN["Database Connectors<br/>(PostgreSQL, MySQL, Oracle)"]
        HTTP_CALL["HTTP Callouts<br/>(REST/SOAP enrichment)"]
        S3_BATCH["S3 Batch Import<br/>(CSV/JSON files)"]
    end

    subgraph "Platform Integrations"
        TEMPORAL["Temporal.io<br/>(Workflow orchestration)"]
        CAMUNDA["Camunda<br/>(BPMN integration)"]
        POWERBI_INT["Power BI<br/>(Analytics connector)"]
    end
```

---

## 19. Technology Stack

### 19.1 Backend

| Component | Technology | Rationale |
|---|---|---|
| **Language** | Java 21 (LTS) | Virtual threads, GraalVM compat, enterprise ecosystem |
| **Framework** | Spring Boot 3.3 | Mature, battle-tested, great observability |
| **Rule Engine** | Custom + JEasyRules adapter | JEasyRules for simple rules, custom for performance |
| **Scripting** | GraalVM CE 23 | Polyglot, sandboxed, high performance JS |
| **Database** | PostgreSQL 16 | JSONB, RLS, CTE, proven in banking |
| **Cache** | Redis 7 Cluster | Sub-ms reads, pub/sub for invalidation |
| **Search** | Elasticsearch 8 | Decision log analytics, full-text search |
| **Messaging** | Apache Kafka 3.7 | Event streaming, async solve, CDC |
| **Object Store** | S3 / MinIO | Exports, batch results, backups |
| **API Gateway** | Kong / Envoy | Rate limiting, auth, routing |
| **AI Service** | Python + FastAPI | LLM integration, ML model serving |

### 19.2 Frontend

| Component | Technology | Rationale |
|---|---|---|
| **Framework** | React 18 + TypeScript | Mature, large ecosystem |
| **State** | Zustand + React Query | Lightweight, server-state focused |
| **Table Editor** | Custom (AG Grid inspired) | Spreadsheet-style rule editing |
| **Tree Editor** | React Flow | Visual decision tree builder |
| **Flow Editor** | React Flow | DAG-based workflow designer |
| **Code Editor** | Monaco Editor | VS Code engine for scripting rules |
| **Charts** | Recharts + D3 | Analytics dashboards |
| **UI Kit** | Shadcn/ui + Tailwind | Modern, accessible components |

### 19.3 Infrastructure

| Component | Technology | Rationale |
|---|---|---|
| **Container Orchestration** | Kubernetes 1.29+ | Standard for cloud-native |
| **Service Mesh** | Istio (optional) | mTLS, traffic management |
| **CI/CD** | GitHub Actions / ArgoCD | GitOps deployment |
| **Monitoring** | Prometheus + Grafana | Metrics and dashboards |
| **Logging** | OpenTelemetry + Loki | Structured logging |
| **Tracing** | OpenTelemetry + Jaeger | Distributed tracing |
| **Secret Mgmt** | HashiCorp Vault | Secrets, encryption keys |
| **IaC** | Terraform / Pulumi | Infrastructure provisioning |

---

## 20. Development Roadmap

### 20.1 Phased Delivery

```mermaid
gantt
    title RuleForge Development Roadmap
    dateFormat  YYYY-MM
    axisFormat  %Y Q%q

    section Phase 1: Foundation (MVP)
    Core engine (Tables + Trees)           :p1a, 2026-03, 3M
    Management API (CRUD, versioning)       :p1b, 2026-03, 3M
    Auth service (JWT, API keys, RBAC)     :p1c, 2026-04, 2M
    Basic Web UI (table editor, tree editor):p1d, 2026-04, 3M
    PostgreSQL + Redis setup               :p1e, 2026-03, 1M
    Solver API (single + bulk)             :p1f, 2026-04, 2M
    MVP Release                            :milestone, m1, 2026-06, 0d

    section Phase 2: Platform
    Scripting rules (GraalVM)              :p2a, 2026-07, 2M
    Rule Flow orchestrator                 :p2b, 2026-07, 3M
    Flow editor UI (React Flow)            :p2c, 2026-08, 2M
    Multi-tenancy (Org/Space/Team)         :p2d, 2026-07, 2M
    SSO integration (SAML/OIDC)            :p2e, 2026-08, 1M
    Import/Export (JSON, Excel)            :p2f, 2026-09, 1M
    Platform Release                       :milestone, m2, 2026-10, 0d

    section Phase 3: Enterprise
    Analytics pipeline (Kafka → ES)        :p3a, 2026-10, 2M
    Decision dashboards                    :p3b, 2026-11, 2M
    Audit trail & compliance               :p3c, 2026-10, 2M
    Webhook system                         :p3d, 2026-11, 1M
    SDK generation (5 languages)           :p3e, 2026-11, 2M
    Rule A/B testing                       :p3f, 2026-12, 2M
    Enterprise Release                     :milestone, m3, 2027-01, 0d

    section Phase 4: Advanced
    AI Assistant (LLM integration)         :p4a, 2027-01, 3M
    DMN 1.4 support                        :p4b, 2027-02, 2M
    CEP engine (Kafka Streams)             :p4c, 2027-02, 3M
    Database connectors                    :p4d, 2027-03, 2M
    Embedded engine JAR                    :p4e, 2027-03, 2M
    WASM runtime                           :p4f, 2027-04, 2M
    GitOps rule promotion                  :p4g, 2027-04, 1M
    Full Platform GA                       :milestone, m4, 2027-06, 0d
```

### 20.2 Team Structure

| Role | Count | Phase |
|---|---|---|
| Backend Engineers (Java/Spring) | 4-5 | Phase 1+ |
| Frontend Engineers (React/TS) | 2-3 | Phase 1+ |
| DevOps/SRE Engineer | 1-2 | Phase 1+ |
| UI/UX Designer | 1 | Phase 1+ |
| ML/AI Engineer | 1 | Phase 4 |
| QA Engineer | 1-2 | Phase 1+ |
| Product Manager | 1 | Phase 1+ |
| Technical Writer | 1 | Phase 2+ |
| **Total** | **12-16** | — |

### 20.3 Phase 1 MVP Success Criteria

- [ ] Decision Tables with 10+ operators, 3 execution strategies
- [ ] Decision Trees with visual editor
- [ ] Rule versioning with publish/draft states
- [ ] Solver API < 10ms P50 latency
- [ ] Test Bench with inline testing
- [ ] JWT + API Key authentication
- [ ] RBAC with 4 roles
- [ ] 10 concurrent tenants, 1000 rules each
- [ ] 99.9% uptime in staging
- [ ] OpenAPI 3.1 spec published

---

## 21. Appendices

### Appendix A: Decision Table Operators

| Operator | Symbol | Description | Applies To |
|---|---|---|---|
| Equals | `EQ` | Exact match | All types |
| Not Equals | `NEQ` | Not equal | All types |
| Greater Than | `GT` | Greater than | Number, Date |
| Greater Than or Equal | `GTE` | >= | Number, Date |
| Less Than | `LT` | Less than | Number, Date |
| Less Than or Equal | `LTE` | <= | Number, Date |
| Between | `BETWEEN` | Inclusive range [a, b] | Number, Date |
| In | `IN` | Value in set | String, Number |
| Not In | `NOT_IN` | Value not in set | String, Number |
| Contains | `CONTAINS` | Substring match | String |
| Starts With | `STARTS_WITH` | Prefix match | String |
| Ends With | `ENDS_WITH` | Suffix match | String |
| Regex | `REGEX` | Regular expression | String |
| Is Null | `IS_NULL` | Null check | All types |
| Is Not Null | `IS_NOT_NULL` | Not null check | All types |
| Is True | `IS_TRUE` | Boolean true | Boolean |
| Is False | `IS_FALSE` | Boolean false | Boolean |
| Array Contains | `ARR_CONTAINS` | Array includes value | Array |
| Array Any | `ARR_ANY` | Any element matches | Array |
| Array All | `ARR_ALL` | All elements match | Array |

### Appendix B: Built-In Functions

| Category | Functions |
|---|---|
| **Math** | `ABS`, `CEIL`, `FLOOR`, `ROUND`, `MIN`, `MAX`, `POW`, `SQRT`, `MOD`, `SUM`, `AVG` |
| **String** | `UPPER`, `LOWER`, `TRIM`, `CONCAT`, `SUBSTRING`, `LENGTH`, `REPLACE`, `SPLIT`, `MATCH` |
| **Date** | `NOW`, `TODAY`, `DATE_ADD`, `DATE_DIFF`, `DATE_FORMAT`, `YEAR`, `MONTH`, `DAY`, `IS_BEFORE`, `IS_AFTER` |
| **Logical** | `IF`, `AND`, `OR`, `NOT`, `COALESCE`, `SWITCH`, `IS_NULL` |
| **Array** | `COUNT`, `FIRST`, `LAST`, `FILTER`, `MAP`, `REDUCE`, `FLATTEN`, `UNIQUE`, `SORT` |
| **Data** | `LOOKUP`, `SOLVE` (call another rule), `HTTP_GET`, `HTTP_POST` |
| **Aggregation** | `SUM`, `AVG`, `MIN`, `MAX`, `COUNT`, `MEDIAN`, `PERCENTILE` |

### Appendix C: Comparison with DecisionRules.io API

| Endpoint | DecisionRules.io | RuleForge |
|---|---|---|
| Solve rule | `POST /rule/solve/:ruleId/:version` | `POST /v1/solve/:ruleIdOrAlias/:version` |
| Bulk solve | Same endpoint, array body | `POST /v1/solve/bulk/:ruleIdOrAlias` (dedicated) |
| Async solve | Kafka Solver API | `POST /v1/solve/async/:ruleId` + callback |
| Get rules | `GET /api/rule` | `GET /v1/rules` |
| Create rule | `POST /api/rule` | `POST /v1/rules` |
| Rule flow solve | Deprecated (merged into solver) | Unified solver endpoint |
| Folder tree | `GET /api/folder/:nodeId` | `GET /v1/folders/:nodeId` |
| Tags | `DELETE /api/tags/:id/:version` | `POST/DELETE /v1/rules/:id/tags` |
| Import | Various per type | `POST /v1/rules/import` (unified) |
| **Additional** | — | gRPC endpoint, GraphQL, version diff, A/B testing, AI endpoints |

### Appendix D: Infrastructure Cost Estimate (AWS, Production)

| Component | Spec | Monthly Cost (est.) |
|---|---|---|
| EKS Cluster | 3 control plane | $220 |
| Solver nodes (3x) | c6g.xlarge (4 vCPU, 8G) | $330 |
| Management nodes (2x) | m6g.large (2 vCPU, 8G) | $150 |
| RDS PostgreSQL | db.r6g.large, Multi-AZ, 100GB | $450 |
| ElastiCache Redis | cache.r6g.large, 3-node | $400 |
| OpenSearch | 3x r6g.large, 200GB | $600 |
| MSK Kafka | kafka.m5.large, 3 broker | $500 |
| ALB | Application Load Balancer | $30 |
| S3 | 500GB + requests | $20 |
| CloudWatch/Monitoring | Logs + metrics | $100 |
| **Total** | | **~$2,800/month** |

*Note: This is a baseline production setup. Costs scale with traffic and tenants.*

---

**Document Version:** 2.0
**Classification:** Internal — Architecture Team
**Last Updated:** February 2026
**Next Review:** March 2026
