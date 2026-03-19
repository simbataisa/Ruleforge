# BMAD Squad: Full Project Analysis & Design

The BMAD agent squad is already installed. The project context is in .bmad/.
Coordinate all 10 agents across the following phases:

## Project Context (already loaded via CLAUDE.md)

- .bmad/PROJECT-CONTEXT.md — project vision and goals
- .bmad/tech-stack.md — technology choices
- .bmad/team-conventions.md — project rules and standards

## Analysis Phase (Business Analyst → Product Owner)

**Business Analyst** (`/business-analyst`):

- Review .bmad/PROJECT-CONTEXT.md
- Identify stakeholders, constraints, risks
- Generate project brief — output to docs/project-brief.md

**Product Owner** (`/product-owner`):

- Take Business Analyst brief
- Create PRD with prioritized user stories
- Output to docs/stories/ and docs/prd.md

## Solutioning Phase (Solution Architect → UX Designer → Enterprise Architect)

**Solution Architect** (`/solution-architect`):

- Take PRD and tech-stack.md
- Propose system architecture, service boundaries
- Define API contracts, data models
- Create ADRs — output to docs/architecture/

**UX Designer** (`/ux-designer`):

- Work from PRD and user personas
- Create wireframes, user journeys, design system
- Output to docs/ux/

**Enterprise Architect** (`/enterprise-architect`):

- Review Solution Architect proposal
- Propose cloud infrastructure, CI/CD, monitoring
- Address compliance, cost optimization
- Output to docs/architecture/

## Implementation Phase (Tech Lead → Backend → Frontend → Mobile)

**Tech Lead** (`/tech-lead`):

- Coordinate backend, frontend, mobile teams
- Identify integration points, flag risks and dependencies
- Review for architectural consistency

**Backend Engineer** (`/backend-engineer`):

- Take architecture ADRs and tech-stack.md
- Design API endpoints, data access layer, event-driven services
- Output to docs/architecture/

**Frontend Engineer** (`/frontend-engineer`):

- Take UX wireframes and tech-stack.md
- Design component architecture, state management, accessibility strategy
- Output to docs/ux/

**Mobile Engineer** (`/mobile-engineer`):

- Take UX wireframes and tech-stack.md
- Decide native vs. cross-platform, mobile architecture, device/network constraints
- Output to docs/ux/

## Quality & Testing

**Tester & QE** (`/tester-qe`):

- Take all artifacts (stories, architecture, designs, code plans)
- Propose test strategy (unit, integration, e2e, security, performance)
- Define quality gates — output to docs/testing/

## Handoff & Documentation

All agents:

- Write decisions to .bmad/handoff-log.md
- Update .bmad/domain-glossary.md with new business terms

## Output Format

- **Analysis Artifacts:** project-brief, backlog, user stories (docs/stories/)
- **Architecture Artifacts:** ADRs, API specs, data models (docs/architecture/)
- **Design Artifacts:** Wireframes, personas, journeys (docs/ux/)
- **Implementation Plans:** Service breakdown, sprint-level tasks, integration checklist
- **Testing Artifacts:** Test strategy, test plan, automation roadmap (docs/testing/)

---

## Your Task

Enhance webhooks creation page to support different authentication types

- API Key
- Basic Auth
- Bearer Token
- No Auth
- Custom Header
- OAuth 2.0
- Webhook Signing Secret
- Backoff Retry
- Observability
