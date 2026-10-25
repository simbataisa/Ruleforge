# Project Context: RuleForge

## Changelog

- 2026-10-25: Populate project identity, constraints, and artifact inventory from current sources. Derived from repository files; runtime health and release readiness were not assessed.

> Repository onboarding snapshot. Agents read this file first on every invocation.
> Keep it short — this is orientation, not documentation.

## What We're Building
RuleForge is an enterprise Business Rules Management System for authoring, managing, and executing decision logic. It combines an embeddable Java engine, seven Spring Boot services, a React interface, and an optional FastAPI AI service. Source: [README](../README.md).

## Current Phase
**Phase 3 — Solutioning (artifact-based onboarding inference).** Existing architecture, PRD, and UX documents are present alongside implemented services. This classification does not establish completion of their acceptance criteria or a current release gate.

## Last Handoff
**From:** BMAD → **To:** Tech Lead / next onboarding role
**Date:** 2026-10-25
**Summary:** Refreshed source-derived context under --force. See [handoff #027](handoffs/027-2026-10-25-bmad→next.md).

## Key Constraints
<!-- Non-negotiable constraints agents must respect -->
- Must integrate with existing engine, Spring Boot services, PostgreSQL, Redis, Kafka, and React UI (README; manifests; Docker Compose).
- Local deployment: Docker Compose and scripts/start-local.sh. Production cloud and region: under review; no provider inferred.
- License: proprietary, all rights reserved (README). Legal compliance obligations: under review.
- Team size: unknown.
- Target go-live: unknown.

## Confirmed Decisions (do not re-debate these)
> See `.bmad/tech-stack.md` for full technology decisions
- Architecture pattern: embeddable engine plus microservices (README; platform/settings.gradle.kts).
- Primary cloud: under review; Docker configuration is present.

## Artifacts Index
| Artifact | Location | Status |
|---|---|---|
| Project Brief | [docs/project-brief.md](../docs/project-brief.md) | Present; approval not assessed |
| PRD | [docs/prd.md](../docs/prd.md) | Present; approval not assessed |
| Solution Architecture | [docs/architecture/solution-architecture.md](../docs/architecture/solution-architecture.md) | Present; approval not assessed |
| Enterprise Architecture | [docs/architecture/enterprise-architecture.md](../docs/architecture/enterprise-architecture.md) | Present; approval not assessed |
| UX Design System | [docs/ux/DESIGN.md](../docs/ux/DESIGN.md) | Present; approval not assessed |

## Stakeholders
| Name | Role | Availability |
|---|---|---|
| Under review | No confirmed stakeholder roster supplied | Unknown |

## Additional Existing Artifacts

- [ADR index](../docs/architecture/adr/ADR-INDEX.md)
- [Codebase map](../docs/architecture/codebase-map/README.md) — documented as a snapshot before the Spring Boot 4 upgrade.

## Source Caveats

README describes eight rule types, while the core RuleType enum declares ten values. Treat engine declarations, frontend support, and product coverage as separate evidence until reconciled. No runtime checks were performed during onboarding.
