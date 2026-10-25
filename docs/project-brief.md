# Project Brief — RuleForge

## Changelog

- 2026-10-25: Rewrite the brief around the complete RuleForge product. Preserve the original approved Spaces feature brief in [project-brief-spaces-page-features.md](project-brief-spaces-page-features.md). Replace the previous feature-led document and appended inventory with one coherent project overview.

**Status:** Draft product overview, grounded in the current repository. Business targets and stakeholder sign-off remain to be agreed.
**As of:** 2026-10-25
**Product:** Enterprise Business Rules Management System (BRMS)

## Executive Summary

RuleForge enables organizations to author, manage, and execute business rules as reusable, versioned decision logic. Its purpose is to make changing decisions—such as eligibility, pricing, scoring, and routing—manageable through dedicated authoring, testing, approval, and execution workflows.

The project combines an embeddable Java rule engine, a managed rules platform, a browser-based authoring experience, and client SDKs. Teams can embed decision execution in a Java application or call the platform from their own systems. The platform brings rule organization, collaboration, version management, approvals, integrations, and decision visibility around that execution capability.

The repository also includes an optional AI assistant for rule generation and analysis. Spaces, folders, and tags organize the rules managed through the broader product.

Sources: [README product description](../README.md#what-is-ruleforge), [project structure](../README.md#project-structure), and [engine API](../engine/ruleforge-core/src/main/java/io/ruleforge/core/RuleForgeEngine.java).

## Problem and Product Purpose

Business decisions change as organizations adjust policies, offers, eligibility conditions, and operating procedures. When these decisions are embedded throughout application code or maintained in separate documents and spreadsheets, teams need a way to make them explicit, reusable, reviewable, and executable.

RuleForge addresses that need by giving decision logic a managed lifecycle: define a rule, exercise it against inputs, review and publish a version, integrate its execution into business applications, and inspect its results. This is the product rationale derived from the documented capabilities; customer-specific pain, costs, and adoption baselines have not yet been validated.

## Intended Users and Their Work

The following roles are derived from documented workflows. Named customers, sponsors, and validated personas still need confirmation.

| Role | What they need to accomplish | Relevant product capabilities |
|---|---|---|
| Business analyst / rule author | Express and maintain decision logic, test examples, and understand changes | Visual editors, expressions, test inputs, rule versions, version comparison |
| Developer / integration engineer | Execute reusable decisions from applications and connect required data | Embeddable engine, execution APIs, client SDKs, connectors |
| Reviewer / approver | Evaluate proposed rule changes before publication | Approval queue, version history, comparison |
| Organization administrator | Organize access and rule assets for teams | Organizations, membership, roles, spaces, folders, tags, API keys, SSO configuration |
| Operator / decision analyst | Inspect execution behavior and investigate decision activity | Execution traces, analytics, audit history, schedules, webhook deliveries |

Sources: [README features](../README.md#key-features), [frontend routes](../frontend/src/app/routes.tsx), [API reference](api-reference.md), and [rule types guide](rule-types-guide.md).

## Core Product Capabilities

| Capability | User-facing purpose | Repository evidence |
|---|---|---|
| Rule authoring | Model decisions using tables, trees, scorecards, ensembles, decision models, flows, scripts, and event patterns | [Rule types guide](rule-types-guide.md), [engine modules](../engine/settings.gradle.kts) |
| Decision execution | Validate, compile, and evaluate rules against supplied inputs, including bulk evaluation | [Engine API](../engine/ruleforge-core/src/main/java/io/ruleforge/core/RuleForgeEngine.java), [execution APIs](api-reference.md#rule-execution) |
| Rule lifecycle and governance | Create and organize rules; manage versions, approvals, publication, archival, and comparisons | [Features](../README.md#key-features), [rule API hooks](../frontend/src/api/rules.api.ts), [frontend routes](../frontend/src/app/routes.tsx) |
| Team and tenant administration | Manage organizations, members, permissions, spaces, and access credentials | [Authentication guide](auth-guide.md), [API reference](api-reference.md) |
| Application and data integration | Call decisions through APIs or SDKs, enrich inputs using connectors, and exchange rule definitions | [API reference](api-reference.md), [SDKs](../sdks), [import/export](../README.md#key-features) |
| Operational workflows and visibility | Schedule work, compare rule versions through experiments, inspect decision results, record activity, and deliver event notifications | [Features](../README.md#key-features), [services](../README.md#services) |
| AI assistance | Assist rule authoring and analysis through generation, optimization suggestions, and natural-language interaction | [AI assistant guide](ai-assistant-guide.md), [AI service](../ai-service/pyproject.toml) |

This capability inventory reflects documentation and source structure. Runtime behavior, supported combinations, performance, and release readiness require their own verification.

## Principal User Journeys

1. **Author and maintain a decision:** choose a rule representation, define conditions and outcomes, test inputs, compare revisions, and submit changes for review where required.
2. **Integrate a decision into an application:** select a rule and version, supply input data, execute through the embedded engine or platform interface, and consume the result.
3. **Govern change:** organize assets and permissions, review proposed versions, approve publication, and retain a history of changes.
4. **Operate and improve:** inspect results and execution activity, investigate failures, manage schedules and integrations, and compare alternative rule versions.

These journeys summarize the repository's current product surfaces. They do not define new sprint assignments or acceptance criteria.

## Product Delivery Surfaces and Boundaries

| Surface | Responsibility | Main consumers |
|---|---|---|
| Embeddable engine | Rule validation, compilation, and execution inside a Java application | Application developers |
| Managed platform | Rule storage and lifecycle, organization administration, execution services, integrations, and operational workflows | Business teams and integrating applications |
| Browser interface | Rule authoring, testing, administration, review, and visibility | Authors, administrators, reviewers, operators |
| Java, Node.js, and Python SDKs | Client access to platform capabilities | Integration developers |
| Optional AI service | Assisted authoring and analysis | Authors using AI-enabled workflows |

External applications provide business inputs and consume decision outputs. Domain policies and decision definitions belong to the organizations configuring RuleForge. The repository describes financial services, insurance, healthcare, and e-commerce examples; those examples do not establish specific customer commitments or regulatory applicability.

## Project Scope and Current State

The repository contains engine modules, seven platform services, a frontend application, an AI service, SDKs, migrations, and local infrastructure scripts. Recent source changes include the Java 25 / Spring Boot upgrade and rule archival, locking, organization, and list-navigation improvements.

This brief describes that existing product. A future release's MVP, priorities, delivery date, and approved non-goals have not been established by this revision. Existing feature briefs and their approvals retain their own scope.

The current [PRD](prd.md) still leads with the historical Spaces feature and includes a later product-inventory supplement. It needs a separate product-wide restructuring before it can serve as the full RuleForge requirements baseline.

## Intended Business Outcomes and Measurement

The following are proposed outcomes for stakeholder validation. No numeric targets or current performance claims are inferred.

| Proposed outcome | Candidate measurement | What remains to agree |
|---|---|---|
| Shorter time to change business decisions | Elapsed time from an agreed rule change to an approved published version | Baseline, target, owner, measurement window |
| Reusable decision logic across applications | Active consuming applications and reuse of governed rules | Adoption baseline, target users, counting method |
| More controlled rule changes | Share of applicable rule publications following the agreed review policy | Policy, exceptions, reporting owner, target |
| Reliable decision execution | Correctness against agreed examples; execution success rate and latency under a defined workload | Reference cases, workload, service objectives, evidence owner |

Product Owner and business stakeholders must select the measures and targets. Business Analyst can then refine prioritized requirements and acceptance criteria.

## Constraints and Known Gaps

| Area | Current evidence / open decision |
|---|---|
| Consumer compatibility | The current engine targets Java 25; embedding consumers must support it. See [README Java notes](../README.md#java-25-notes-for-developers). |
| Delivery environment | Local scripts and Docker Compose are present. Production deployment ownership, region, availability commitments, and budget need confirmation. |
| Tenant access and governance | Authentication, permissions, tenant context, and documented data-isolation controls are product concerns. Their effectiveness is outside this document review. |
| Product licensing | README states proprietary, all rights reserved. Commercial terms and legal conclusions are not established here. |
| Regulatory obligations and data sensitivity | No project-specific obligations or data classification have been confirmed. Example industry use cases are insufficient evidence. |
| Operational limitations | README reports Kafka publication, fresh-database seed, and MinIO image issues affecting aspects of the local stack. See [known issues](../README.md#known-issues). |
| Capability consistency | README, engine declarations, and frontend types differ in their rule-type catalogs. Supported representations and surfaces need reconciliation. |

## Decisions Still Required

| Decision | Suggested decision owner | Downstream effect |
|---|---|---|
| Confirm primary users, sponsor, and customer problems | Product Owner / business stakeholders | Establishes whose outcomes the product optimizes |
| Agree business metrics and measurable targets | Product Owner / sponsor | Establishes success criteria |
| Define next-release scope, priorities, and non-goals | Product Owner / stakeholders | Enables a focused requirements baseline |
| Confirm data classification and regulatory obligations | Product Owner / InfoSec | Establishes applicable controls and acceptance evidence |
| Reconcile capability support and operational claims | Solution Architect / Tech Lead / QE | Establishes an accurate supported-capability baseline |

Decision dates and named owners have not been supplied. The overview can orient readers now; these gaps prevent treating it as a signed-off delivery mandate.

## Sources and Related Artifacts

- [README](../README.md): product description, feature catalog, integration surfaces, and known issues.
- [Technology inventory](../.bmad/tech-stack.md): current repository detections and their limitations.
- [Rule types guide](rule-types-guide.md) and [API reference](api-reference.md): documented authoring and integration behavior.
- [Solution architecture](architecture/solution-architecture.md), [full architecture](architecture/RuleForge-Architecture-v2.md), and [engine architecture](architecture/RuleForge-Engine-Core-Architecture-v2.1.md): existing design context; target statements require current source and runtime validation.
- [PRD](prd.md): historical Spaces requirements plus draft product inventory.
- [Spaces Page Features brief](project-brief-spaces-page-features.md): preserved March 2026 feature scope and approval.
- [Other feature briefs](project-brief-webhooks-auth.md): examples of separately scoped product changes.

**Evidence classification:** The product description and capability inventory are derived_from_source. The requested correction to make this the whole-product brief is confirmed by the user's instruction. Proposed business measures and inferred user roles require validation; commercial commitments, quantitative outcomes, future-release scope, and stakeholder sign-off remain unknown.
