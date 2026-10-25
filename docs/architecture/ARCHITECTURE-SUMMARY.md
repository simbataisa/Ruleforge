# RuleForge Architecture Overview

**Document role:** Starting point and document map
**Editorial revision:** 2026-10-25
**Status:** Source-backed overview; design proposals and historical material are linked explicitly.

## Changelog

- **2026-10-25 · ARC-DOC-001:** Reorganize the architecture reading path, consolidate repeated material, and separate source evidence from earlier proposals. See the [change record and section map](ARCHITECTURE-CHANGELOG.md#arc-doc-001--2026-10-25).

RuleForge is a Business Rules Management System for authoring, governing, and executing reusable decisions. It offers an embeddable Java engine and a managed platform with a browser interface, service APIs, SDKs, and optional AI assistance. The [project brief](../project-brief.md) explains the product and intended users.

## Start here

| Your question | Read | What it owns |
|---|---|---|
| What are the main parts and current declarations? | This overview | Reading path and compact source baseline |
| How do services, data, tenancy, and integration fit together? | [Platform architecture](RuleForge-Architecture-v2.md) | Application design, schemas, event flows and integration examples |
| How does rule execution work internally? | [Engine architecture](RuleForge-Engine-Core-Architecture-v2.1.md) | Metamodel, facts, expressions, compilation, scheduling, specialized engines and extension APIs |
| What is configured for deployment and operations? | [Enterprise architecture](enterprise-architecture.md) | Deployment evidence, environments, controls, observability, recovery and costs |
| Why were the Spaces policies written that way? | [Historical Spaces requirements](enterprise-architecture-spaces-features.md) | Original feature constraints, rollback/checklist material and sign-offs |
| What changed, moved, or was superseded? | [Architecture change log](ARCHITECTURE-CHANGELOG.md) | Editorial revisions and old-to-new section map |

For a first read: overview → platform or engine according to your work → enterprise operating constraints. Consult the historical feature record when tracing a Spaces decision.

<a id="system-overview"></a>

## System boundaries

| Part | Responsibility and interactions |
|---|---|
| Browser interface | Authors and administers rules; development API traffic proxies directly to services through Vite |
| Platform services | Auth, management, solver, event, analytics, scheduler and connector services; detailed [service map](RuleForge-Architecture-v2.md#source-backed-service-map) |
| Embedded engine | Validates, compiles and evaluates rules inside a Java consumer; also used by the platform |
| Optional AI service | Assisted rule authoring and analysis; FastAPI process |
| SDKs | Java, Node.js and Python clients for integrating applications |
| Data and messaging | PostgreSQL, Redis, Kafka and Elasticsearch are declared locally; MinIO is configured with an unresolved usage/availability caveat |

Two platform execution modes are documented: local host processes with infrastructure containers, and a full Compose stack in which Nginx proxies API traffic to Kong. These are distinct routing paths. Source configuration and connectivity gaps are recorded in [deployment topology](enterprise-architecture.md#1-deployment-topology).

<a id="engine-architecture"></a>
<a id="module-dependency-graph"></a>
<a id="core-design-principles"></a>
<a id="rule-types"></a>
<a id="platform-architecture"></a>
<a id="shared-module-ruleforge-common"></a>
<a id="data-architecture"></a>
<a id="security-architecture"></a>
<a id="infrastructure"></a>

## Current source baseline

| Area | Repository declaration | Source |
|---|---|---|
| Engine and JVM runtime | Java 25; twelve engine modules including aggregate/starter | [Properties](../../engine/gradle.properties), [settings](../../engine/settings.gradle.kts) |
| Engine core dependencies | SLF4J API and Jackson | [Core build](../../engine/ruleforge-core/build.gradle.kts) |
| Platform | Spring Boot 4.1.1; seven services and one shared library | [Build](../../platform/build.gradle.kts), [settings](../../platform/settings.gradle.kts) |
| Tenant context | ScopedValue with explicit binding | [TenantContext](../../platform/ruleforge-common/src/main/java/io/ruleforge/common/tenant/TenantContext.java) |
| Script runtime | Bundled GraalVM polyglot dependencies, version baseline 25.0.5 | [Engine properties](../../engine/gradle.properties) |
| Frontend | React 18, TypeScript, Vite, Tailwind, Zustand and React Query declarations | [package.json](../../frontend/package.json) |
| Local data infrastructure | PostgreSQL 16, Redis 7, Kafka 3.7.1, Elasticsearch 9.4.5, MinIO | [Infrastructure Compose](../../infra/docker/docker-compose.infra.yml) |
| Gateway | Kong 3.6 in full Compose stack | [Application Compose](../../infra/docker/docker-compose.yml) |

This table confirms declarations and source structure. It does not assert passing tests, service availability, security-control effectiveness, support for every declared rule type, or completed release phases.

<a id="service-responsibilities"></a>

## Follow a request

1. An application calls a rule execution API, or a Java consumer invokes the engine directly.
2. Platform execution resolves the rule/version and coordinates validation, compilation, input handling, and execution.
3. The engine evaluates the selected representation against working memory and returns decision output and trace information.
4. Platform components manage persistence, lifecycle, integrations, and operational records around that execution.

Detailed pipelines and schemas are in the [platform execution chapter](RuleForge-Architecture-v2.md#execution-and-integration) and [engine compilation chapter](RuleForge-Engine-Core-Architecture-v2.1.md#facts-expressions-and-compilation). Those chapters retain design examples whose exact contracts require current-source checks.

## Evidence and design status

| Label | Meaning in this set |
|---|---|
| Source-backed overview / source inventory | Repository declarations and cited source files, inspected on the editorial revision date |
| Design reference | Intended structure or illustrative API/configuration; implementation parity is not presumed |
| Earlier production proposal | Historical capacity, cloud, security, recovery, tooling or cost assumption requiring approval and verification |
| Historical feature requirement | Requirements/sign-offs for the original feature scope, preserved for traceability |
| Unknown | Missing requirements, decision, operational evidence or verification |

The platform and engine files retain their familiar versioned filenames for link continuity. Their February 2026 baselines describe design history; the editorial revision and change log identify later document maintenance.

<a id="implementation-status"></a>

## Known gaps and decisions

- [Enterprise decision list](enterprise-architecture.md#decisions-and-handoff-boundary): hosting, regions, NFR targets, environments, recovery, controls and budgets remain unresolved.
- [Configuration gaps](enterprise-architecture.md#configuration-gaps-to-carry-forward): Kafka listener reachability and Kong webhook routing need validation.
- [README known issues](../../README.md#known-issues): event publication, seed data, and MinIO limitations affect operational claims.
- Rule-type names and counts differ between the README, core enum, and frontend declarations. Treat engine declarations, UI support, and verified behavior as separate questions.
- Historical performance targets, production costs, competitor claims, roadmap checkmarks and commercial assumptions are not current measured or approved outcomes.

<a id="for-more-detail"></a>

## Maintaining this set

Edit a topic in its owning document and link to it from other guides. Update the [shared change log](ARCHITECTURE-CHANGELOG.md) and the affected document's short changelog. Significant technical decisions use [ADRs](adr/ADR-INDEX.md); an editorial reorganization does not change Accepted decisions.

When moving a section, record its old/new location and retain an anchor or redirect for existing links. Keep source observations, design intent, approval status, and operating evidence identifiable in the same section.

<a id="ruleforge-architecture-summary"></a>
