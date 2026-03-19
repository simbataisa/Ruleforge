# Domain Glossary: RuleForge

> Agents use these exact terms — no paraphrasing.
> Maintained by Business Analyst.

## Core Domain Terms

| Term | Definition | Also known as | Never say |
|---|---|---|---|
| Rule | A named, versioned unit of business logic that evaluates facts and produces an output | RuleUnit | "business logic", "policy", "condition" |
| Rule Type | The execution model of a rule: TABLE, TREE, SCORECARD, ENSEMBLE, DMN, FLOW, SCRIPT, CEP, HIERARCHY | Engine type | "rule kind", "rule format" |
| Working Memory | The in-memory fact store populated from input data before rule evaluation | Fact store | "context", "input map" |
| Compiled Rule | A parsed and optimized rule ready for evaluation — stored in cache | Cached rule | "processed rule", "built rule" |
| Space | An organizational grouping of rules within an organization (analogous to a project or environment) | — | "workspace", "namespace", "bucket" |
| Organization | The top-level tenant entity; owns users, spaces, and settings | Org, Tenant | "company", "account" |
| Folder | A sub-grouping within a space for organizing rules hierarchically | — | "directory", "category" |
| RFL | RuleForge Language — a custom recursive-descent expression language with null-safe navigation and lambdas | — | "expression language", "scripting" |
| Salience | A rule's explicit priority weight; higher salience fires first | Priority | "weight" (weight is a separate dimension) |
| Execution Mode | How the engine applies matching rules: FIRST_MATCH, ALL_MATCH, BEST_MATCH, SEQUENTIAL, PARALLEL, FORWARD_CHAIN, CASCADING | — | "execution strategy" |
| Outbox | Database table storing pending webhook delivery events for reliable at-least-once delivery | — | "event queue", "delivery queue" |
| DLQ | Dead Letter Queue — failed webhook deliveries after maximum retry attempts | Dead letter | "failed queue" |
| Circuit Breaker | Failure detection mechanism that pauses webhook delivery to endpoints returning repeated errors | — | "failsafe", "retry limiter" |

## Entity Names (Canonical)

These are the canonical names for domain objects. Use them in code, docs, and conversations:

| Entity | Description | Related entities |
|---|---|---|
| `OrgSpace` | A space within an organization (frontend TypeScript type) | `Organization`, `Rule`, `Folder` |
| `RuleUnit` | Core engine model for a rule definition | `CompiledRule`, `RuleType`, `PriorityVector` |
| `CompiledRule` | Engine-ready compiled form of a `RuleUnit`, stored in L1/L2 cache | `RuleUnit`, `WorkingMemory` |
| `WorkingMemory` | Holds facts for a single rule evaluation session | `Fact`, `FactChange` |
| `RuleResult` | The output of a rule evaluation, containing matched rules and trace | `DecisionTrace`, `TraceNode` |
| `PriorityVector` | Multi-dimensional priority: `(salience, specificity, weight, recency, order)` | `RuleUnit` |
| `TenantContext` | ThreadLocal holder for the current org ID, propagated from JWT | `JwtAuthenticationFilter` |
| `AuditEvent` | A recorded event in the audit trail for a rule lifecycle action | `EventType` |
| `WebhookEntity` | A registered webhook endpoint with scope, events, and secret | `WebhookDelivery`, `OutboxEvent` |
| `SpaceEntity` | Backend JPA entity for an `OrgSpace` | `OrgEntity`, `RuleEntity` |
| `DecisionTrace` | Full execution trace of a rule evaluation for debugging | `TraceNode`, `RuleResult` |

## Process Terms

| Process | Description | Trigger | Outcome |
|---|---|---|---|
| Rule Evaluation | Executing a compiled rule against a populated WorkingMemory | `POST /v1/solve` | `RuleResult` with matched rules and decision trace |
| Approval Workflow | Multi-step review process before a rule version is published | Rule version submitted for approval | DRAFT → PENDING_APPROVAL → PUBLISHED or REJECTED |
| Cache Invalidation | Removing stale compiled rules from L1 (Caffeine) and L2 (Redis) | Rule publish/update event via Kafka | Fresh rule loaded from L3 (PostgreSQL) on next solve |
| Webhook Delivery | Outbox poller picks up pending events and POSTs to registered endpoints | Rule lifecycle event (created, updated, published, etc.) | Delivery logged; retry on failure; DLQ after max attempts |
| Forward Chaining | Iterative rule re-evaluation when an action modifies working memory | Execution mode = FORWARD_CHAIN | Up to 100 re-evaluation cycles before cycle detection stops |
| Space Assignment | Moving an existing rule to a different space | `PATCH /rules/{id}/move` | Rule's `spaceId` updated; full history preserved; UUID unchanged |

## Status / State Values

| Entity | States | Transitions allowed |
|---|---|---|
| Rule Version | DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED, ARCHIVED | DRAFT→PENDING_APPROVAL, PENDING_APPROVAL→PUBLISHED, PENDING_APPROVAL→REJECTED, PUBLISHED→ARCHIVED |
| Webhook | ACTIVE, PAUSED, CIRCUIT_OPEN | ACTIVE↔PAUSED, ACTIVE→CIRCUIT_OPEN (auto), CIRCUIT_OPEN→ACTIVE (manual reset) |
| Delivery | PENDING, SUCCESS, FAILED, IN_DLQ | PENDING→SUCCESS, PENDING→FAILED, FAILED→IN_DLQ (after max retries) |
| A/B Test | RUNNING, STOPPED, COMPLETED | RUNNING→STOPPED, RUNNING→COMPLETED |

## Abbreviations

| Abbreviation | Full form | Context |
|---|---|---|
| BRMS | Business Rules Management System | Product category |
| RFL | RuleForge Language | Custom expression language in the engine |
| DLQ | Dead Letter Queue | Failed webhook deliveries |
| RLS | Row-Level Security | PostgreSQL multi-tenant isolation mechanism |
| CEP | Complex Event Processing | Rule type for pattern detection over event streams |
| DMN | Decision Model and Notation | OMG standard (version 1.4) for rule tables |
| BDD | Binary Decision Diagram | Compilation target for Decision Tree rules |
| L1/L2/L3 | Cache levels | L1 = Caffeine (per-JVM), L2 = Redis (distributed), L3 = PostgreSQL |
| FEEL | Friendly Enough Expression Language | Expression language within DMN standard |
| SSE | Server-Sent Events | Transport for real-time notifications |
| SSO | Single Sign-On | SAML/OIDC authentication via external identity providers |
| RBAC | Role-Based Access Control | Permission model: OWNER > EDITOR > VIEWER |
| SA | Solution Architect | BMAD agent role |
| EA | Enterprise Architect | BMAD agent role |
| BA | Business Analyst | BMAD agent role |
| PO | Product Owner | BMAD agent role |
| TL | Tech Lead | BMAD agent role |
| QE | Quality Engineer | BMAD agent role (also "Tester/QE") |

## Ubiquitous Language Rules

- A **"rule"** is always a versioned, named artifact with a type (TABLE, TREE, etc.) — not a raw SQL condition or if/else branch.
- A **"space"** is an organizational container for rules within an org — not a "workspace", "environment", or "project".
- A **"solve"** is the act of executing rules against input data — not "running", "triggering", or "evaluating" (use "evaluate" only at the engine API level).
- **"Moving"** a rule changes its `spaceId` assignment — it is NOT the same as copying, and it preserves the rule's UUID, version history, and audit trail.
- **"Publishing"** a rule advances it from DRAFT to PUBLISHED through the approval workflow — not "deploying" or "activating".
- A **"tenant"** = an **"organization"** — use "org" in code and "organization" in UI copy.
- **"Salience"** is the explicit priority number on a rule. **"Priority"** is the full multi-dimensional `PriorityVector`. Do not use them interchangeably.
