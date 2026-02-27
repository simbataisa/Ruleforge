# BMAD Method — Shared Context for All Agents

## What is BMAD?

BMAD (Breakthrough Method of Agile AI-Driven Development) is a structured, agent-orchestrated approach to the software development lifecycle. Instead of unstructured AI prompting, BMAD uses specialized agent personas that collaborate through explicit artifact handoffs, ensuring context preservation, traceability, and quality across the entire SDLC.

## BMAD Four-Phase Cycle

1. **Analysis** — Explore the problem space, capture constraints, produce a Project Brief
2. **Planning** — Transform the brief into a PRD with functional/non-functional requirements, epics, and stories
3. **Solutioning** — Design architecture, define technical specifications, create detailed implementation stories
4. **Implementation** — Build, test, review, and deliver working software

## Collaborative Handoff Model

Agents operate in a collaborative/iterative mode:

- Any agent can request input from another agent at any point
- Artifacts are the contract — agents read and refine shared artifacts rather than relying on chat context
- Feedback loops are encouraged (e.g., Architect refines based on BA feedback, Tech Lead reviews stories before dev starts)
- All artifacts live in a shared `docs/` directory within the project

## Agent Roster

| Agent | Phase | Primary Responsibility |
|-------|-------|-----------------------|
| Business Analyst | Analysis | Problem exploration, stakeholder analysis, project brief |
| Product Owner | Planning | PRD, backlog prioritization, artifact alignment |
| Solution Architect | Solutioning | Service decomposition, API contracts, data models, ADRs |
| Enterprise Architect | Solutioning | Cloud infra, compliance, observability, CI/CD, FinOps |
| UX/UI Designer | Solutioning | Personas, user journeys, wireframes, design system, accessibility |
| Tech Lead | All Phases | Orchestration, code review standards, risk, release readiness |
| Tester & QE | All Phases | Test strategy, test cases, security testing, quality gates |
| Backend Engineer | Implementation | APIs, data layers, event-driven services |
| Frontend Engineer | Implementation | React/TypeScript components, state management, accessibility |
| Mobile Engineer | Implementation | iOS, Android, React Native, Flutter, offline-first |

## Standard Artifact Directory Structure

```
project-root/
├── docs/
│   ├── project-brief.md          # Analysis phase output
│   ├── prd.md                    # Product Requirements Document
│   ├── architecture/
│   │   ├── solution-architecture.md
│   │   ├── enterprise-architecture.md
│   │   ├── adr/                  # Architecture Decision Records
│   │   │   └── ADR-001-*.md
│   │   └── diagrams/
│   ├── ux/                       # UX/UI Designer outputs
│   │   ├── personas.md
│   │   ├── user-journeys.md
│   │   ├── information-architecture.md
│   │   ├── design-system.md
│   │   ├── ui-spec.md
│   │   ├── accessibility-audit.md
│   │   └── wireframes/
│   ├── stories/
│   │   ├── epic-1/
│   │   │   ├── story-1.1.md
│   │   │   └── story-1.2.md
│   │   └── epic-2/
│   ├── test-plans/
│   │   ├── test-strategy.md
│   │   ├── test-cases/
│   │   └── qa-checklists/
│   ├── tech-specs/
│   │   ├── api-spec.md
│   │   ├── data-model.md
│   │   └── integration-spec.md
│   └── reviews/
│       ├── code-review-checklist.md
│       └── architecture-review.md
├── src/                          # Source code
├── tests/                        # Test code
└── .bmad/                        # BMAD config and state
    ├── project-state.md          # Current phase, blockers, decisions
    └── handoff-log.md            # Record of agent-to-agent handoffs
```

## Artifact Versioning

All artifacts are markdown files under version control. When an agent modifies an artifact:

1. Note what changed and why at the top of the file in a changelog section
2. Log the handoff in `.bmad/handoff-log.md`
3. Reference the specific artifact version when handing off to the next agent

## Enterprise System Conventions

Since these agents target enterprise systems (microservices, cloud infrastructure, complex integrations):

- Always consider non-functional requirements: scalability, security, observability, compliance
- Document integration points and contract boundaries explicitly
- Use Architecture Decision Records (ADRs) for significant technical choices
- Consider multi-environment deployment (dev, staging, prod)
- Address cross-cutting concerns: authentication, authorization, logging, monitoring, rate limiting
