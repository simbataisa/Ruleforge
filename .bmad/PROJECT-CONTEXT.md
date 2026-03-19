# Project Context: RuleForge

> Agents read this file first on every invocation.
> Keep it short — this is orientation, not documentation.

## What We're Building

RuleForge is an enterprise Business Rules Management System (BRMS) for authoring, managing, and executing business rules at scale. It combines an embeddable Java rule engine with a microservices platform and a modern React-based UI. Primary users are business analysts, rule authors, and developers at financial services, insurance, healthcare, and e-commerce companies who need to manage complex decision logic without deploying code changes.

## Current Phase

Epic 6 (Webhook Auth v2 — OAuth 2.0 & Custom Header) is **IN IMPLEMENTATION** — stories ready for development.

- [x] Phase 1 — Analysis (Business Analyst)
- [x] Phase 2 — Planning (Product Owner)
- [x] Phase 3 — Solutioning (Solution Architect, UX Designer)
- [x] Phase 4 — Stories Written (Tech Lead) ← CURRENT
- [ ] Phase 5 — Implementation (Backend Engineer, Frontend Engineer)
- [ ] Phase 6 — Release Review (Tech Lead)

## Last Handoff

**From:** Tech Lead → **To:** Backend Engineer + Frontend Engineer
**Date:** 2026-03-19
**Summary:** Epic 6 stories 022–027 written. Implement in order: 024 → 022 → 023 → 025 → 026 → 027. No DB migration. Redis token cache for OAuth2 (ADR-010). AUTH_FAILURE status prevents retry storms. See `.bmad/handoff-log.md` entry #8 for full details.

## Key Constraints

- Must integrate with: existing Spring Boot 3.3 microservices, PostgreSQL 16, Kafka 3.7, React 18 frontend
- Deploy to: Docker Compose (local dev), Kubernetes via Helm (production)
- Compliance: multi-tenant data isolation via PostgreSQL Row-Level Security; JWT auth; audit trail on all mutations
- Architecture pattern: microservices (7 Spring Boot services + AI service + API gateway)
- Primary cloud: cloud-agnostic (Docker/Kubernetes)
- Proprietary — all rights reserved

## Confirmed Decisions (do not re-debate these)

> See `.bmad/tech-stack.md` for full technology decisions

- Architecture pattern: microservices — 7 Spring Boot services behind Kong API Gateway
- Engine core (`ruleforge-core`) has ZERO external dependencies beyond SLF4J + Jackson (embeddable requirement)
- Package namespace: `io.ruleforge`
- Build tools: Gradle with Kotlin DSL (engine), Gradle (platform), Vite (frontend)
- Single PostgreSQL database shared across all services; RLS for multi-tenancy via `app.current_org_id`
- Org hierarchy: Organization → Space → Folder → Rule
- Local dev: Vite proxy routes directly to service ports (Kong not running)
- All services use `flyway.enabled: false` + `ddl-auto: update` for local dev; Flyway migrations applied separately

## Artifacts Index

| Artifact | Location | Status |
|---|---|---|
| Project Brief (Spaces) | `docs/project-brief.md` | ✅ final |
| PRD (Spaces) | `docs/prd.md` | ✅ final |
| Solution Architecture (Spaces) | `docs/architecture/solution-architecture.md` | ✅ final |
| Enterprise Architecture | `docs/architecture/enterprise-architecture.md` | ✅ final |
| UX Design System | `docs/ux/design-system.md` | ✅ final |
| Project Brief (Space-Rule Mgmt) | `docs/project-brief-spaces-rules.md` | ✅ final |
| PRD (Space-Rule Mgmt) | `docs/prd-spaces-rules.md` | ✅ final |
| Architecture (Space-Rule Mgmt) | `docs/architecture/solution-architecture-spaces-rules.md` | ✅ final |
| UX Spec (Space-Rule Mgmt) | `docs/ux/ui-spec-spaces-rules.md` | ✅ final |
| Project Brief (Space Banners) | `docs/project-brief-space-banners.md` | ✅ final |
| PRD (Space Banners) | `docs/prd-space-banners.md` | ✅ final |
| Architecture (Space Banners) | `docs/architecture/solution-architecture-space-banners.md` | ✅ final |
| UX Spec (Space Banners) | `docs/ux/ui-spec-space-banners.md` | ✅ final |
| Project Brief (Space Assignment) | `docs/project-brief-space-assignment.md` | ✅ final |
| PRD (Space Assignment) | `docs/prd-space-assignment.md` | ✅ final |
| Architecture (Space Assignment) | `docs/architecture/solution-architecture-space-assignment.md` | ✅ final |
| UX Spec (Space Assignment) | `docs/ux/ui-spec-space-assignment.md` | ✅ final |
| ADR Index | `docs/architecture/adr/ADR-INDEX.md` | ✅ final |
| Project Brief (Webhooks Auth) | `docs/project-brief-webhooks-auth.md` | ✅ final |
| PRD (Webhooks Auth) | `docs/prd-webhooks-auth.md` | ✅ final |
| Architecture (Webhooks Auth) | `docs/architecture/solution-architecture-webhooks-auth.md` | ✅ final |
| ADR-007: Credential Encryption | `docs/architecture/adr/ADR-007-webhook-credential-encryption.md` | ✅ Accepted |
| ADR-008: Auth Injection Strategy | `docs/architecture/adr/ADR-008-webhook-auth-injection-strategy.md` | ✅ Accepted |
| ADR-009: Metrics Storage | `docs/architecture/adr/ADR-009-webhook-metrics-storage.md` | ✅ Accepted |
| UX Spec (Webhooks Auth) | `docs/ux/ui-spec-webhooks-auth.md` | ✅ final |
| Stories (Epic 5) | `docs/stories/story-014` through `story-021` | ✅ ready for dev |
| Full Architecture | `docs/architecture/RuleForge-Architecture-v2.md` | ✅ final |
| Engine Architecture | `docs/architecture/RuleForge-Engine-Core-Architecture-v2.1.md` | ✅ final |
| Stories (all epics) | `docs/stories/story-001` through `story-013` | ✅ final |
| Release Readiness (Spaces) | `docs/reviews/release-readiness.md` | ✅ final |
| Release Readiness (Space-Rule) | `docs/reviews/release-readiness-spaces-rules.md` | ✅ final |
| Release Readiness (Banners) | `docs/reviews/release-readiness-space-banners.md` | ✅ final |
| Release Readiness (Assignment) | `docs/reviews/release-readiness-space-assignment.md` | ✅ final |
| Project Brief (Webhook Auth v2) | `docs/project-brief-webhooks-oauth2-customheader.md` | ✅ final |
| PRD (Webhook Auth v2) | `docs/prd-webhooks-oauth2-customheader.md` | ✅ final |
| Architecture (Webhook Auth v2) | `docs/architecture/solution-architecture-webhooks-oauth2-customheader.md` | ✅ final |
| ADR-010: OAuth2 Token Caching | `docs/architecture/adr/ADR-010-oauth2-token-caching-strategy.md` | ✅ Accepted |
| UX Spec (Webhook Auth v2) | `docs/ux/ui-spec-webhooks-oauth2-customheader.md` | ✅ final |
| Stories (Epic 6) | `docs/stories/story-022` through `story-027` | ✅ ready for dev |

## Stakeholders

| Name | Role | Availability |
|---|---|---|
| admin@ruleforge.io | Owner / Decision-maker | Always |
| editor@ruleforge.io | Editor / Rule Author | Always |
| viewer@ruleforge.io | Viewer / Read-only consumer | Always |
