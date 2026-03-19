# Enterprise Architecture Governance Checklist

**Document Version:** 1.0
**Last Updated:** [YYYY-MM-DD]
**Owner:** Chief Architect / Enterprise Architecture Office
**Review Frequency:** Quarterly
**Next Review:** [YYYY-MM-DD]

This document provides comprehensive checklists and governance policies that serve as gatekeeping criteria for architecture decisions, technology adoption, and solution approvals in the enterprise.

---

## Table of Contents

1. [Solution Intake Checklist](#solution-intake-checklist)
2. [Technology Adoption Governance](#technology-adoption-governance)
3. [Integration Governance](#integration-governance)
4. [Data Governance](#data-governance)
5. [Architecture Decision Records (ADR) Governance](#architecture-decision-records-governance)

---

## Solution Intake Checklist

All proposed solutions (new projects, major enhancements, system replacements) must complete this checklist before architecture review. Answers "NO" to any question may indicate need for further work or escalation.

### Business Case and Alignment (10 items)

**Purpose:** Validate that the solution addresses a real business need and aligns with enterprise strategy.

- [ ] **Business problem clearly defined:** Is the business problem documented in detail? (Example: "Current manual order fulfillment takes 7 days; customers expect 2-day delivery. Losing market share to competitors offering faster delivery.")
  - If NO: Work with business sponsor to clarify the problem before proceeding.

- [ ] **Business case prepared:** Is there a documented business case including problem statement, proposed solution, expected benefits, cost estimate, and timeline?
  - If NO: Finance/Business must prepare business case. Proceed to architecture only after approval.

- [ ] **ROI or value justification:** Can benefits be quantified (ROI, payback period, cost savings, revenue increase, risk reduction)?
  - If NO: Clarify what success looks like. Qualitative benefits (customer satisfaction) must still be measurable.

- [ ] **Strategic alignment:** Is the solution aligned with 3-year IT roadmap or 5-year business strategy?
  - If NO: Escalate to steering committee. May reject or defer based on strategic priorities.

- [ ] **Stakeholder buy-in:** Has business sponsor approved? Have all impacted department heads (e.g., operations, support, finance) been consulted?
  - If NO: Conduct stakeholder reviews. Address concerns. Obtain written approval from sponsor before architecture review.

- [ ] **Success criteria defined:** Are measurable success criteria defined? (Example: "Reduce fulfillment time to < 24 hours. Achieve 99.9% order accuracy. Support 500 orders/day by year 2.")
  - If NO: Define metrics with business owner. Success criteria will be basis for post-implementation review.

- [ ] **Timeline realistic:** Is the timeline (design, development, testing, go-live) achievable given team size and complexity?
  - If NO: Extend timeline or reduce scope. Unrealistic timelines lead to technical debt.

- [ ] **Funding secured:** Is budget allocated and approved? Is funding available for all phases (development, operations, support)?
  - If NO: Obtain budget approval from CFO before commencing work.

- [ ] **Risks identified:** Have key risks been identified (technical, business, schedule, resource)?
  - If NO: Conduct risk assessment. Identify mitigations.

- [ ] **Dependencies documented:** Are external dependencies documented? (Example: "Depends on CRM data availability. Requires 3 FTE from ops team for 6 months.")
  - If NO: Identify all dependencies. Confirm availability.

---

### Technical Standards and Requirements (10 items)

**Purpose:** Validate that the solution will comply with enterprise architecture standards.

- [ ] **Architecture style selected:** Is the architecture style documented? (Example: "Microservices architecture using event-driven communication," or "3-tier monolithic application.")
  - If NO: Architecture team must select appropriate style based on requirements and constraints.

- [ ] **Technology stack within approved list:** Are all proposed technologies on the approved technology radar? (Check corporate technology standards.)
  - If NO: Submit Technology Adoption form for new technology (see Technology Adoption Governance section).

- [ ] **APIs designed:** Are all service boundaries and APIs designed and documented? (OpenAPI, gRPC proto, GraphQL schema)
  - If NO: API design required before development. Use enterprise API design standards.

- [ ] **Data model defined:** Is the logical data model documented? Are entities, relationships, and key attributes defined?
  - If NO: Data architect must design data model. Document before development.

- [ ] **Non-functional requirements specified:** Are performance, availability, scalability, security requirements defined with measurable targets?
  - Example: "P99 latency < 200ms, Availability 99.9%, Support 1000 RPS"
  - If NO: Define NFRs with product owner and architects.

- [ ] **Integration points identified:** Are all integration points with other systems documented? (APIs called, events consumed/published, data syncs)
  - If NO: Map all integration dependencies. Confirm contracts with dependent systems.

- [ ] **Deployment model decided:** Is the deployment model documented? (On-premise, cloud, hybrid; containerised or not; Kubernetes or other orchestration)
  - If NO: Architecture team and infrastructure to decide deployment model.

- [ ] **Scalability approach defined:** Is the approach to scaling documented? (Horizontal/vertical, stateless design, caching strategy, database scaling)
  - If NO: Design scalability approach. Plan load testing.

- [ ] **Disaster recovery plan outlined:** Are RTO and RPO targets defined? Backup and failover strategies sketched?
  - If NO: Define RTO/RPO based on business criticality. Plan backup/restore.

- [ ] **Testing strategy defined:** Is the testing strategy documented? (Unit, integration, end-to-end, performance, security tests; test data strategy)
  - If NO: QA lead and architects define testing strategy before development.

---

### Security and Compliance (10 items)

**Purpose:** Validate that the solution meets security requirements and regulatory obligations.

- [ ] **Data classification defined:** Are all data types in the solution classified? (Public, Internal, Confidential, Restricted)
  - If NO: Data architect and security team classify data. Assign controls based on classification.

- [ ] **Authentication approach selected:** Is the authentication mechanism documented? (OAuth2, SAML, API key, mTLS, passwordless)
  - If NO: Security architect selects appropriate auth mechanism. Configure identity provider integration.

- [ ] **Authorization model defined:** Is the access control model documented? (RBAC, ABAC, resource-based policies)
  - If NO: Define roles and permissions. Document in authorization policy.

- [ ] **Encryption requirements specified:** Are encryption requirements defined for data at rest and in transit?
  - If NO: Define encryption standards (AES-256 at rest, TLS 1.3+ in transit). Update security architecture.

- [ ] **PII handling identified:** Are personally identifiable information fields identified? How is PII protected?
  - If NO: Identify PII fields. Define masking, encryption, access controls for PII.

- [ ] **Audit logging planned:** Will audit logs be created? How are they retained and protected?
  - If NO: Plan audit logging for state-changing operations. Define retention (typically 7 years).

- [ ] **Compliance requirements mapped:** Are relevant regulations identified? (GDPR, HIPAA, PCI-DSS, SOC 2, etc.)
  - If NO: Legal/compliance team identifies applicable regulations. Document requirements.

- [ ] **Vulnerability scanning planned:** Will code be scanned for vulnerabilities? Is a SAST/DAST tool in place?
  - If NO: Configure code scanning (SonarQube, Snyk). Plan security testing.

- [ ] **Access control implemented:** Is access to sensitive resources restricted? (Network policies, service accounts, credential management)
  - If NO: Define principle of least privilege. Implement network policies, IAM policies.

- [ ] **Third-party risk assessed:** Have all third-party services and vendors been assessed? Are contracts (BAA, DPA, SLA) in place?
  - If NO: Conduct vendor assessment. Execute required contracts before integration.

---

### Data Governance (8 items)

**Purpose:** Validate that the solution follows master data management and data ownership principles.

- [ ] **Data ownership defined:** For each major data entity, is the owning service/team identified?
  - Example: "Customer master data owned by Customer Service. Order data owned by Order Service."
  - If NO: Data architect defines ownership. Document in data governance register.

- [ ] **Master data governance:** For master data (customer, product, vendor), is the MDM approach documented?
  - If NO: Define which service is system of record (SoR). Define data sync strategy if multiple copies needed.

- [ ] **Data quality rules defined:** Are data quality rules documented? (Completeness, accuracy, timeliness, consistency)
  - If NO: Define quality rules. Plan data quality monitoring.

- [ ] **Data retention policy:** Is the retention policy for each data type documented? (How long kept? Archival? Deletion?)
  - If NO: Define retention periods. Plan archival and deletion processes.

- [ ] **Data lineage documented:** Can data lineage be traced? (Where does data come from? How is it transformed? Where does it go?)
  - If NO: Document data lineage. Critical for audit and compliance.

- [ ] **Cross-service data sharing:** If data is shared across services, is the approach documented? (API calls, event subscriptions, shared database, ETL)
  - If NO: Prefer APIs or events over shared databases. Document approach.

- [ ] **Sensitive data protection:** Is sensitive data (passwords, API keys, payment info) handled securely?
  - If NO: Define secure handling. Never store sensitive data at rest; use external vaults.

- [ ] **GDPR / privacy compliance:** If handling EU personal data, are GDPR requirements addressed? (Data subject rights, DPA, privacy notices)
  - If NO: Legal/compliance to assess GDPR applicability. Plan data subject right fulfillment.

---

### Operational Readiness (8 items)

**Purpose:** Validate that operations team can support the solution in production.

- [ ] **Runbooks prepared:** Are operational runbooks prepared for common scenarios? (Service restart, database failover, error recovery)
  - If NO: Ops team and dev team to prepare runbooks before go-live. Store in central wiki.

- [ ] **Monitoring and alerting:** Are key metrics defined? Are dashboards and alerts configured?
  - If NO: Define critical metrics (latency, availability, error rate). Configure monitoring tool. Set alert thresholds.

- [ ] **On-call rotation:** Is on-call rotation established? Are escalation paths defined?
  - If NO: Establish on-call rotation in PagerDuty (or equivalent). Define escalation procedures.

- [ ] **Incident response plan:** Is incident response procedure documented? Are roles defined?
  - If NO: Create incident response playbook. Define who responds, how communication happens, post-mortem process.

- [ ] **Disaster recovery tested:** Has the disaster recovery plan been tested? Does it work?
  - If NO: Test DR plan before go-live. Document and sign off results.

- [ ] **Capacity planning:** Have capacity needs been assessed? Can the infrastructure handle peak load?
  - If NO: Conduct capacity analysis. Right-size infrastructure. Plan for growth.

- [ ] **Log aggregation:** Are logs centrally aggregated? Can ops team search logs?
  - If NO: Configure log aggregation (ELK, Splunk, Datadog). Ensure ops team trained.

- [ ] **Performance baseline:** Has a performance baseline been established? Are SLAs defined and monitored?
  - If NO: Establish baseline through load testing. Define SLAs. Monitor against baseline.

---

## Technology Adoption Governance

### Technology Lifecycle Stages

All technologies (languages, frameworks, databases, tools) follow a lifecycle in the enterprise:

```
ASSESS → TRIAL → ADOPT → HOLD → RETIRE
```

**Stage definitions:**

| Stage | Definition | Action |
|-------|-----------|--------|
| **ASSESS** | New technology under evaluation; not yet approved for use. | Proof of concept or trial project in isolated environment. Careful evaluation. Small team. Limited risk. |
| **TRIAL** | Technology approved for trial; limited use in non-critical projects. | Up to 2-3 projects can use. Must monitor closely. Ops team trained. Easy rollback plan. |
| **ADOPT** | Technology fully approved and supported; mainstream use across enterprise. | Can be used in new projects. Ops team trained and ready. Vendor support secured. |
| **HOLD** | Technology still supported but no new projects should start; existing projects allowed to continue. | Planned replacement technology in progress. No new adoption. Existing systems maintained. |
| **RETIRE** | Technology no longer supported; all systems must migrate off. | Timeline communicated. Vendor support ended. Migration plan mandatory. |

**Example technology radar:**

```
ADOPT
- Java 17 LTS
- Python 3.11
- PostgreSQL 15
- Kubernetes 1.27
- React 18
- Kafka

TRIAL
- Go 1.20
- Rust (for performance-critical components)
- DynamoDB (for specific use cases)

ASSESS
- WebAssembly
- Temporal (workflow orchestration)
- GraphQL

HOLD
- PHP
- Oracle (new projects should use PostgreSQL)

RETIRE
- Python 2 (sunset date: 2025-06-30)
- Java 8 (sunset date: 2025-12-31)
```

### Approved vs. Restricted vs. Prohibited

| Category | Definition | Example |
|----------|-----------|---------|
| **APPROVED** | Preferred; use in new projects. Enterprise has expertise and support. | Java, Python, PostgreSQL, Kubernetes, AWS |
| **RESTRICTED** | Can be used with enterprise architect approval; requires justification. | PHP (restricted; mostly legacy), Cassandra (restricted; requires ops review), Oracle (restricted; use PostgreSQL instead) |
| **PROHIBITED** | Cannot be used. Risk too high or vendor no longer supported. | IE6 (deprecated), Python 2 (end of life), Node-RED (not approved for enterprise use) |

### Technology Adoption Request Process

**When you want to use a new technology:**

1. **Submit Technology Adoption Request Form:**
   - Technology name and version
   - Use case (what problem does it solve?)
   - Comparison with approved alternatives (why not use approved tech?)
   - Risk assessment (licensing, vendor stability, community support, security)
   - Team capability (do we have expertise or training plan?)
   - Timeline and scope (pilot project with limited scope)

2. **Architecture review:**
   - Enterprise Architect evaluates against standards.
   - Risk assessment conducted.
   - Decision: Approve, Conditional Approval, Reject, Request more info.

3. **If approved:**
   - Technology status changed to TRIAL.
   - Team conducts pilot project with careful monitoring.
   - 6-month review to assess.

4. **Post-pilot review (6 months later):**
   - Pilot results reviewed.
   - Decision: Promote to ADOPT, extend TRIAL, or RETIRE experiment.

**Example approval email:**

```
Subject: Technology Adoption Request APPROVED - Go 1.20

Technology: Go (Golang) 1.20
Status: APPROVED for TRIAL (pilot project: Fulfillment API Rewrite)
Conditions:
  - Pilot limited to Fulfillment API; no other projects without re-review
  - Ops team training by Q2 2024
  - 6-month post-pilot review required
  - Architecture review required before production deployment

Approved by: Chief Architect
Date: 2024-03-14
Review date: 2024-09-14
```

---

## Integration Governance

### Approved Integration Patterns

**Approved patterns (use these):**

| Pattern | Protocol | Use Case | Advantages | Cautions |
|---------|----------|----------|-----------|----------|
| **Synchronous REST** | HTTP/HTTPS | Real-time queries, simple CRUD operations | Simple, well-understood, good tooling | Tightly coupled; latency-sensitive |
| **Synchronous gRPC** | HTTP/2 | High-performance service-to-service; low-latency requirements | Fast, binary protocol, strong typing | Steeper learning curve |
| **Asynchronous Events** | Message Broker (Kafka, RabbitMQ, AWS SQS) | Notifications, long-running operations, decoupling | Loose coupling, scalable, resilient | Eventual consistency |
| **Batch/ETL** | File transfer, database replication | Data warehouse, reporting, nightly batch jobs | Simple for large data volumes | Latency (not real-time) |

**Prohibited patterns (avoid these):**

| Pattern | Why Prohibited | Alternative |
|---------|---|---|
| **Point-to-point service calls** | Creates spaghetti architecture; tight coupling; hard to scale | Use API Gateway, message broker, or event-driven |
| **Shared database** | Tight coupling; schema changes affect multiple teams | Use Database per Service; APIs for cross-service queries |
| **Custom messaging protocol** | Hard to maintain; security risks; reinventing the wheel | Use standard message broker (Kafka, RabbitMQ) |
| **FTP/SFTP file transfers** | Unreliable; hard to track; security risk | Use APIs or message brokers |

### API Standards Governance

**All APIs must follow these standards:**

**REST API standards:**
- Endpoint naming: Use nouns for resources (/orders, /products), not verbs (/getOrders is wrong)
- HTTP methods: GET (retrieve), POST (create), PUT/PATCH (update), DELETE (delete)
- Status codes: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Server Error
- Versioning: Use URL path versioning (/api/v1/orders) or Accept header versioning
- Documentation: All APIs must have OpenAPI 3.0 specification
- Authentication: All APIs authenticate caller (OAuth2, API key, or service account)
- Rate limiting: Define rate limits (e.g., 100 RPS per client)
- Error responses: Consistent error format with error code and message

**Example REST API standards:**

```
GET /api/v1/orders/{orderId}
  Response: 200 OK
  {
    "id": "ORD-123",
    "customerId": "CUST-456",
    "status": "CONFIRMED",
    "items": [...],
    "total": 1250.00,
    "createdAt": "2024-03-14T10:30:00Z"
  }

GET /api/v1/orders?customerId=CUST-456&limit=10&offset=0
  Response: 200 OK
  {
    "items": [...],
    "total": 42,
    "limit": 10,
    "offset": 0
  }

POST /api/v1/orders
  Request:
  {
    "customerId": "CUST-456",
    "items": [{productId: "PROD-1", quantity: 5}]
  }
  Response: 201 Created
  Location: /api/v1/orders/ORD-789
```

### Event Schema Registry

**All events must be registered in central event schema registry.**

**Event schema requirements:**
- Event name: Descriptive, past tense (OrderCreated, PaymentCaptured)
- Schema version: Major.minor (e.g., 1.0, 2.1) for backward compatibility
- Owning service: Which service publishes this event?
- Consumers: Which services subscribe to this event?
- Retention: How long events retained in topic (30 days, 1 year, indefinite)?
- Key fields: Document key fields and types
- Example: Provide sample event payload

**Event schema template:**

```yaml
eventName: OrderCreated
version: 1.0
owningService: OrderService
consumers:
  - BillingService
  - InventoryService
  - NotificationService
retention: 30 days
schema:
  type: object
  required: [orderId, customerId, totalAmount, createdAt]
  properties:
    orderId:
      type: string
      description: "Unique order ID"
    customerId:
      type: string
      description: "Customer ID (link to customer master)"
    items:
      type: array
      items:
        type: object
        properties:
          productId: {type: string}
          quantity: {type: integer, minimum: 1}
          unitPrice: {type: number}
    totalAmount:
      type: number
      description: "Order total in major currency units"
    createdAt:
      type: string
      format: "date-time"

example:
  orderId: "ORD-123"
  customerId: "CUST-456"
  items:
    - productId: "PROD-1"
      quantity: 5
      unitPrice: 99.99
  totalAmount: 499.95
  createdAt: "2024-03-14T10:30:00Z"
```

### Point-to-Point Anti-Pattern Escalation

**If a team proposes point-to-point service integration (Service A → Service B → Service C):**

1. **Notification:** Architecture team identifies pattern in review.
2. **Escalation:** Meeting with team leads to discuss implications (tight coupling, complexity).
3. **Justification required:** Team must justify why event-driven or API Gateway not feasible.
4. **Architect approval:** Only enterprise architect can approve point-to-point integration (rare).
5. **Documented:** Integration mapped in architecture; flagged for future refactoring.

---

## Data Governance

### Data Classification Levels

All data in the enterprise is classified into one of four levels. Controls and handling vary by level.

| Level | Definition | Examples | At-Rest Security | In-Transit Security | Access Control | Retention |
|-------|-----------|----------|---|---|---|---|
| **PUBLIC** | Data safe to share with anyone; no confidentiality concerns. | Marketing materials, public APIs, product info | No encryption required | No encryption required | No restrictions | Business rules |
| **INTERNAL** | Data for internal use only; no PII; not confidential. | HR policies, internal memos, financial data | Standard encryption (optional) | TLS 1.2+ | Employees | 5 years typical |
| **CONFIDENTIAL** | Data with confidentiality; PII or proprietary; business sensitive. | Customer data, payment info, source code, employee records | AES-256 encryption REQUIRED | TLS 1.3 REQUIRED | Need-to-know; access logging | 7 years typical |
| **RESTRICTED** | Highly sensitive; strict regulatory requirements; high liability if breached. | Financial account numbers, SSN, healthcare records, credentials | AES-256 + key encryption REQUIRED | TLS 1.3 + mTLS REQUIRED | Minimal access; audited; MFA | 7-10 years (regulatory) |

**Classification decision matrix:**

| Question | Answer | Classification |
|----------|--------|---|
| Contains PII (name, email, address, phone)? | Yes | CONFIDENTIAL or RESTRICTED |
| Contains regulated data (health, financial)? | Yes | RESTRICTED |
| Contains passwords, keys, credentials? | Yes | RESTRICTED |
| Contains business proprietary info? | Yes | CONFIDENTIAL |
| Safe to publish on public website? | Yes | PUBLIC |
| Safe to share with employees? | Yes | INTERNAL |

### Master Data Management

**Master data entities:**

| Entity | System of Record (Owner) | Access Model | Sync Frequency | Consumers |
|--------|---|---|---|---|
| Customer | Customer Service | Read-only API; CustomerMasterData events | Real-time event + daily batch | Order Service, Billing Service, Support, CRM |
| Product | Catalog Service | Read-only API; ProductMasterData events | Real-time event + daily batch | Order Service, Inventory Service, Pricing Service |
| Vendor/Supplier | Procurement Service | Read-only API; VendorMasterData events | Real-time event + weekly batch | Procurement, Finance, Inventory |
| Account (GL) | Finance System | Read-only API; AccountMasterData events | Daily batch | Finance reporting, AP/AR |

**Rules for master data:**

1. **Single source of truth:** Only the owning service writes to master data. Other services read only.
2. **No local copies:** Don't duplicate master data in multiple databases. Use APIs or events to reference.
3. **Sync strategy:** Define how data is synchronised to other services (real-time events, daily batch, on-demand API call).
4. **Data quality:** Master data must meet quality standards (completeness, accuracy, timeliness).
5. **Change management:** Changes to master data schema require notification to all consumers.

### Cross-Border Data Transfer Rules

**If data is transferred across country borders:**

1. **Legal assessment:** Determine data residency requirements (EU GDPR, China, India regulations).
2. **Data transfer agreement:** Execute Data Transfer Agreement or Standard Contractual Clauses.
3. **Encryption:** Encrypt data in transit; encryption key remains in source country.
4. **Documentation:** Document all transfers; audit trail maintained.

**Restricted data transfers:**

| Scenario | Rule |
|----------|------|
| EU personal data → Non-EU countries | Adequacy decision or Standard Contractual Clauses required (GDPR Article 46) |
| China data | Must be stored and processed within China; no transfer out without government approval |
| India data | No transfer across borders without explicit customer consent and data localization law compliance |
| US data | Subject to CFAA and data breach notification laws; cross-border transfers require assessment |

### Data Retention and Deletion

**Default retention policy:**

| Data Type | Retention Period | Reason |
|-----------|---|---|
| Transactional data (orders, invoices, payments) | 7 years | Tax compliance (IRS, SOX) |
| Customer personal data | Active + 2 years after deletion request | GDPR retention; customer re-engagement window |
| Audit logs | 7 years | Compliance, forensics |
| System logs | 90 days | Operational debugging |
| Temporary/session data | 30 days of inactivity | Automatic cleanup |
| User activity logs | 1 year | Analysis, security audits |
| Backup data | 30 days full + 90 days incremental | Disaster recovery |

**Deletion process:**

1. **Data subject requests deletion:** Customer exercises GDPR "right to be forgotten" or business policy deletion.
2. **Pseudonymization:** If possible, pseudonymize instead of delete (maintain data utility for analytics).
3. **Hard deletion:** If required, securely delete from all systems:
   - Production database
   - Backups (mark for future deletion in retention period)
   - Search indexes
   - Caches
   - Audit logs (pseudonymize if possible)
4. **Verification:** Confirm deletion; no recovery possible.
5. **Documentation:** Log deletion in audit trail.

---

## Architecture Decision Records (ADR) Governance

### When an ADR is Required

An ADR is required for significant architectural decisions. Use judgment; not every tiny decision needs an ADR.

**Decisions that REQUIRE an ADR:**

- [ ] Selection of major architecture pattern (microservices vs. monolith, CQRS, event-driven)
- [ ] Choice of primary technology for a service (language, database, framework)
- [ ] Design of critical API or data contract
- [ ] Security or compliance decision with lasting impact
- [ ] Significant trade-off or constraint decision
- [ ] Deviations from enterprise standards
- [ ] Decisions that will be expensive to reverse

**Decisions that DON'T need an ADR:**

- Implementation details (which library to use, variable naming)
- Tactical improvements to existing systems
- Changes to local configuration files
- Minor bug fixes or enhancements

**Example decisions requiring ADR:**

- "We will use PostgreSQL instead of Oracle for order database."
- "We will adopt Kafka for event streaming instead of RabbitMQ."
- "We will implement CQRS for reporting to separate read and write models."
- "We will shared customer master data across services via API instead of database replication."
- "We will use OpenID Connect (OIDC) for user authentication instead of custom session tokens."

### ADR Template

All ADRs follow this template and are stored in central Git repository (`/architecture/decisions/`):

**Format:** Markdown file named `ADR-XXXX-[title-in-kebab-case].md`

Example: `ADR-0001-use-postgresql-for-order-service.md`

```markdown
# ADR-0001: Use PostgreSQL for Order Service Database

**Date:** 2024-03-14
**Status:** ACCEPTED
**Deciders:** [Architect Names]
**Stakeholders:** [Team names, affected services]
**Revision:** 1.0

## Context

The Order Service requires a database for storing order data. We need to select between two options:
1. PostgreSQL (open source, ACID, strong community, self-hosted cost)
2. Oracle (commercial, more features, higher licensing cost, more operational complexity)

### Requirements
- ACID transactions required for order consistency
- Strong query language needed (complex order queries)
- Scalability to 10,000 orders/day
- Cost-effective solution
- Team expertise available in-house or available in market

## Decision

We will use PostgreSQL 15 for the Order Service database.

## Rationale

1. **ACID compliance:** PostgreSQL fully ACID-compliant; meets requirement for order consistency.
2. **Cost:** PostgreSQL free; Oracle licensing $15K+/year. No licensing cost.
3. **Scalability:** PostgreSQL proven at scale; many enterprise deployments handling 1000s TPS. Meets 10K orders/day easily.
4. **Team capability:** Multiple team members have PostgreSQL experience. Ops team trained on PostgreSQL.
5. **Community:** Large active community; good tooling; easy to find resources and developers.
6. **Cloud support:** AWS RDS PostgreSQL, Azure Database for PostgreSQL, Google Cloud SQL PostgreSQL all available and well-supported.

### Alternatives considered

#### Oracle Database
- **Advantages:** Mature, many enterprise features
- **Disadvantages:** High licensing cost ($15K+/year), operational complexity, overkill for our use case
- **Decision:** REJECTED due to cost and unnecessary complexity

#### MongoDB (NoSQL)
- **Advantages:** Flexible schema, horizontal scaling, good for document storage
- **Disadvantages:** ACID transactions added only in v4.0; eventual consistency model not ideal for orders; team less familiar
- **Decision:** REJECTED due to eventual consistency and team expertise gaps

## Consequences

### Positive
- No licensing costs
- Strong ACID guarantees for order data
- Team can support and debug issues
- Easy to hire PostgreSQL developers

### Negative
- Self-hosted costs (server infrastructure, backups, replication)
- Operational overhead (monitoring, patching, upgrades)
- Scaling requires sharding (eventual need if growth exceeds 100K orders/day)

## Compliance

- Data classification: CONFIDENTIAL. Encryption at rest (AES-256) and in transit (TLS 1.3) required.
- Backup: Daily incremental, weekly full backups. 7-year retention for compliance.
- GDPR: GDPR data subject rights (export, deletion) must be supported. Implementation plan in place.

## Related ADRs
- ADR-0002: Use Kafka for event streaming (related; events published to Kafka)
- ADR-0003: API design standards (affects how Order Service is queried)

## Version History

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2024-03-14 | [Architect Name] | Initial decision |
| 1.1 | [Date] | [Name] | [Change] |
```

### ADR Lifecycle

ADRs move through states as context changes:

| Status | Meaning | Action |
|--------|---------|--------|
| **PROPOSED** | Under discussion; not yet decided | Team discusses pros/cons; collects feedback |
| **ACCEPTED** | Decision made; approved by architecture board | Implement the decision; communicate to stakeholders |
| **DEPRECATED** | Decision still applies but newer approach available; existing systems stay as-is | Document alternative in new ADR; mark original as DEPRECATED |
| **SUPERSEDED** | Decision replaced by newer decision | Link to new ADR (ADR-0009 supersedes ADR-0001) |

### ADR Review and Approval Process

1. **Author drafts ADR:** Solution architect writes ADR using template above.
2. **Discussion:** ADR shared in architecture review; team comments/discussion.
3. **Architecture board review:** Enterprise architect + security architect review; vote.
4. **Approval:** If majority approve, status changed to ACCEPTED.
5. **Implementation:** Decision is now binding; teams implement accordingly.
6. **Deprecation/Supersession:** As time passes and context changes, older ADRs may be marked DEPRECATED or SUPERSEDED.

**Example ADR lifecycle:**

```
ADR-0001 (2020): Use monolithic architecture
  Status: ACCEPTED → DEPRECATED (2023: decided to migrate to microservices)

ADR-0006 (2023): Migrate to microservices architecture
  Status: PROPOSED → ACCEPTED → SUPERSEDED (2024: refined with event-driven pattern)

ADR-0010 (2024): Use event-driven microservices with Kafka
  Status: ACCEPTED (current approach)
```

### ADR Storage and Accessibility

**Location:** All ADRs stored in Git repository: `https://github.com/company/architecture/decisions/`

**Directory structure:**
```
/architecture/decisions/
  ADR-0001-use-postgresql-for-order-service.md
  ADR-0002-use-kafka-for-event-streaming.md
  ADR-0003-rest-api-design-standards.md
  ...
  /deprecated/
    ADR-0000-old-decision.md (marked DEPRECATED)
```

**Access:**
- All team members can read ADRs.
- Only architects and tech leads can create/modify ADRs.
- Changes tracked in Git history.
- ADRs linked in architecture documentation.

---

## Escalation Paths

**When you need an exception to standards:**

1. **Document the reason:** Write request form explaining why exception is needed, what risk is accepted.
2. **Risk assessment:** Security team, architect team assess risk.
3. **Approval:** Chief Architect makes approval decision.
4. **Documentation:** Exception documented; linked to ADR or policy.
5. **Sunset:** Exception has sunset date (e.g., "Approved through 2025-12-31, then must conform").

**Example exception:**

```
Exception Request: Use Python 2 for Legacy Billing System
Requested by: Billing Team Lead
Date: 2024-03-14
Reason: Legacy billing system written in Python 2; full rewrite would cost $2M and take 12 months.
Rewrite not in FY2024 budget.
Risk accepted: Python 2 EOL (2020); no more security patches. Isolated system (not internet-facing);
limited risk. Sunset plan: Migrate to Python 3.11 in FY2025.

Approved by: Chief Architect
Sunset date: 2025-12-31
Condition: Billing team must have migration plan to Python 3.11 in place by 2025-06-30
```

---

## Governance Review Schedule

| Artifact | Review Frequency | Owner | Next Review |
|----------|---|---|---|
| This governance checklist | Quarterly | Chief Architect | [Date] |
| Technology radar | Biannually | CTO Office | [Date] |
| Approved technology list | Quarterly | Architecture Team | [Date] |
| Data classification policy | Annually | Chief Data Officer | [Date] |
| Integration standards | Biannually | Enterprise Architect | [Date] |
| ADR repository | Quarterly (audit) | Architecture Team | [Date] |

---

## Document Metadata

| Item | Value |
|------|-------|
| Document Owner | Chief Architect |
| Last Updated | [YYYY-MM-DD] |
| Review Frequency | Quarterly |
| Next Review Date | [YYYY-MM-DD] |
| Approval Authority | CTO, Chief Architect, Chief Data Officer |
| Distribution | Enterprise Architecture Board, All Tech Leads, Solution Architects |

