# Architecture Documentation Change Log

## How to track a change

Read the short changelog at the top of the affected guide, then the record below for scope and moved sections. Git provides the exact diff. Accepted technical decisions are recorded separately in the [ADR index](adr/ADR-INDEX.md).

## ARC-DOC-001 — 2026-10-25

**Type:** Editorial reorganization and source-baseline correction.
**Base commit:** `c9ebf24c7981411ee65b424f72f2a67d21f79239`
**Approval scope:** User requested reorganization of the five architecture documents. No new platform decision, interface contract, release certification, or production authority is introduced.

### Reading roles

| Document | Before | After |
|---|---|---|
| [Overview](ARCHITECTURE-SUMMARY.md) | Old versions, ASCII topology, completion tables | Entry point, document ownership, current cited declarations, evidence legend and maintenance rules |
| [Platform guide](RuleForge-Architecture-v2.md) | 21 interleaved product, application, engine, infrastructure, and planning chapters | Seven application-oriented reading chapters; technical design retained; historical planning at the end |
| [Engine guide](RuleForge-Engine-Core-Architecture-v2.1.md) | 20 consecutive chapters | Six reading chapters ordered from foundations through compilation/execution to extension/performance references |
| [Enterprise guide](enterprise-architecture.md) | Current source inventory only | Current inventory followed by one consolidated historical production-proposal reference |
| [Historical Spaces record](enterprise-architecture-spaces-features.md) | Nested title and chronological feature material | Feature context, safety, audit, rollout, and rule-management chapters; original sign-offs preserved |

### Consolidation and replacement decisions

- Current source declarations have one compact overview baseline linked to manifests. Replaced stale Java 21, Spring Boot 3.3, Elasticsearch 8, module/service-count and ThreadLocal statements in the summary.
- The summary no longer presents “Complete” checkmarks or unverified all-tenant control claims as current evidence.
- The platform's repeated rule-engine section and JEasyRules adapter recommendation are replaced by a link to the custom-engine guide. Original text remains recoverable at the base commit; the custom-engine direction already existed in the engine guide.
- Platform quality targets, scaling/HPA examples, deployment/Kubernetes examples, infrastructure candidate tools, and AWS cost worksheet move to the enterprise guide. They are explicitly historical proposals, not current platform choices or prices.
- Engine memory/cache/pool sketch moves to the platform data chapter; the engine retains a section link.
- Current engine runtime note is corrected to Java 25 with declared SLF4J/Jackson dependencies. Design examples and benchmark targets remain explicitly distinguished from implementation evidence.
- API/schema examples, specialized-engine details, existing Mermaid bodies, and historical Spaces policy/sign-off text are retained where in scope. The duplicated platform rule-type mindmap and superseded adapter snippet are intentionally retired.
- Legacy section anchors are preserved where headings moved. Misleading original TOC fragments are replaced with chapter navigation.
- No Accepted ADR is changed. No application or infrastructure code is changed.

### Source-baseline evidence

[Engine settings](../../engine/settings.gradle.kts), [engine properties](../../engine/gradle.properties), [core dependencies](../../engine/ruleforge-core/build.gradle.kts), [platform settings](../../platform/settings.gradle.kts), [platform build](../../platform/build.gradle.kts), [TenantContext](../../platform/ruleforge-common/src/main/java/io/ruleforge/common/tenant/TenantContext.java), [Compose](../../infra/docker/docker-compose.infra.yml), and [frontend dependencies](../../frontend/package.json).

### Section map

Numbers below identify the original sections. Their local anchors remain available even when their material is now grouped under a different chapter.

| Original document | Original section | Current document | Treatment |
|---|---|---|---|
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#1-executive-summary) | 1. Executive Summary | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Moved behind current design as explicitly historical product planning |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#2-competitive-analysis--positioning) | 2. Competitive Analysis & Positioning | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Moved behind current design as explicitly historical product planning |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#3-architecture-principles--constraints) | 3. Architecture Principles & Constraints | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Split: principles remain here; quality targets moved to enterprise proposals |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#4-system-context-c4-level-1) | 4. System Context (C4 Level 1) | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#5-container-architecture-c4-level-2) | 5. Container Architecture (C4 Level 2) | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#6-component-architecture-c4-level-3) | 6. Component Architecture (C4 Level 3) | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#7-core-domain-model) | 7. Core Domain Model | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#8-rule-types--execution-engine) | 8. Rule Types & Execution Engine | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Replaced duplicate engine overview and superseded JEasyRules adapter with canonical engine link |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#9-api-design) | 9. API Design | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#10-data-architecture) | 10. Data Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#11-multi-tenancy-architecture) | 11. Multi-Tenancy Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#12-scalability--performance-architecture) | 12. Scalability & Performance Architecture | [enterprise-architecture.md](enterprise-architecture.md) | Moved to earlier production proposals |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#13-security-architecture) | 13. Security Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#14-deployment-architecture) | 14. Deployment Architecture | [enterprise-architecture.md](enterprise-architecture.md) | Moved to earlier production proposals |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#15-event-driven-architecture--audit) | 15. Event-Driven Architecture & Audit | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#16-analytics--bi-architecture) | 16. Analytics & BI Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#17-ai-assistant-architecture) | 17. AI Assistant Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#18-sdk--integration-architecture) | 18. SDK & Integration Architecture | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Regrouped design reference |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#19-technology-stack) | 19. Technology Stack | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Replaced stale application stack tables with source baseline; infrastructure candidates moved to enterprise proposals |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#20-development-roadmap) | 20. Development Roadmap | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Moved behind current design as explicitly historical product planning |
| [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md#21-appendices) | 21. Appendices | [RuleForge-Architecture-v2.md](RuleForge-Architecture-v2.md) | Operators/functions/comparison retained; cost worksheet moved to enterprise proposals |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#1-design-rationale) | 1. Design Rationale | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#2-engine-architecture-overview) | 2. Engine Architecture Overview | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#3-rule-metamodel--the-type-system) | 3. Rule Metamodel — The Type System | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#4-priority--salience-system) | 4. Priority & Salience System | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#5-hierarchical-rule-composition) | 5. Hierarchical Rule Composition | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#6-weighted--scoring-branches) | 6. Weighted & Scoring Branches | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#7-execution-strategies--conflict-resolution) | 7. Execution Strategies & Conflict Resolution | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#8-decision-table-engine) | 8. Decision Table Engine | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#9-decision-tree-engine) | 9. Decision Tree Engine | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#10-rule-flow--orchestration-engine) | 10. Rule Flow / Orchestration Engine | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#11-scripting-engine-graalvm) | 11. Scripting Engine (GraalVM) | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#12-complex-event-processing-engine) | 12. Complex Event Processing Engine | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#13-working-memory--fact-model) | 13. Working Memory & Fact Model | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#14-rule-compilation-pipeline) | 14. Rule Compilation Pipeline | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#15-expression-language-rfl) | 15. Expression Language (RFL) | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#16-engine-extension-points-spi) | 16. Engine Extension Points (SPI) | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#17-performance-architecture) | 17. Performance Architecture | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; platform memory sketch moved out |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#18-comparison-with-industry-engines) | 18. Comparison with Industry Engines | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#19-class-diagrams--interfaces) | 19. Class Diagrams & Interfaces | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md#20-appendices) | 20. Appendices | [RuleForge-Engine-Core-Architecture-v2.1.md](RuleForge-Engine-Core-Architecture-v2.1.md) | Regrouped; technical examples retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#executive-summary) | Executive Summary | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#rbac-governance) | RBAC Governance | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#cascading-delete-safety) | Cascading Delete Safety | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#slug-uniqueness-collision-handling) | Slug Uniqueness Collision Handling | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#audit-logging) | Audit Logging | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#compliance-posture) | Compliance Posture | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#observability) | Observability | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#rollback-strategy) | Rollback Strategy | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#summary-enterprise-readiness-checklist) | Summary: Enterprise Readiness Checklist | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |
| [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md#appendix-epic-2--space-scoped-rule-management-2026-03-14) | Appendix: Epic 2 — Space-Scoped Rule Management (2026-03-14) | [enterprise-architecture-spaces-features.md](enterprise-architecture-spaces-features.md) | Regrouped historical requirement; policy/sign-off text retained |

### Review boundary

Document checks cover link/anchor integrity, section accounting, preserved examples and historical requirements, and source-backed baseline corrections. No application tests, benchmark runs, deployments, recovery exercises, pricing research, or third-party capability verification are implied.

BA requirements, production choices, operational evidence, and independent architecture readiness remain outside this editorial change.
