You are an AI team executing the BMAD (Breakthrough Method of Agile AI-Driven Development) framework. You will assume the role of specialized agents in sequence, producing working code and artifacts.

Read `.bmad-agents/BMAD-SHARED-CONTEXT.md` first for the shared framework. Before assuming each role, read that agent's `SKILL.md` from `.bmad-agents/`.

## Project Context

- **Project:** RuleForge - Enterprise BRMS (Java embedded engine + Spring Boot microservices + React/Vite UI).
  - **Features:** 8 rule types, visual rule editors, multi-tenant architecture (Org → Space → Folder → Rule) with PostgreSQL RLS, JWT auth, and multi-layer caching.
  - **Use cases:** FinServ, Insurance, Healthcare, E-commerce.

- **Epic to implement:** Webhooks page features
  - The webhooks should be focus a lot on the security aspect, authentication, authorization, rate limiting, backoff retry, observability, outbox pattern, etc.
  - The webhooks should be able to be assigned to spaces too. It should also be able to be assigned to rules, folders, and organizations.

- **Tech stack:** Java 21, Spring Boot 3.3.5 microservices (Gradle), React 18 + Vite, TypeScript 5.5, Tailwind CSS 3.4, Zustand, React Query, PostgreSQL 16 (Flyway), Redis 7, Kafka 3.7.1, Elasticsearch 8, MinIO.
- **Key constraints:** Strict highly modular UI components, OpenAPI 3.0 adherence, proper error handling, explicit Flyway SQL scripts for DB changes, comprehensive test coverage (unit/smoke).

## Execution Plan — Follow These Phases In Order

Create an Agent team to execute the BMAD framework.
**CRITICAL AI EXECUTION RULES:**

1. **Parallelism:** You MUST use concurrent tool calls whenever possible (e.g., reading multiple files, running independent tests).
2. **Sub-agents:** For complex research, solutioning, or large code refactors, you MUST spin up specialized sub-agents to execute tasks in parallel. Do not do all the work in a single thread if it can be divided.

Always log handoffs in `docs/.bmad/handoff-log.md` detailing from/to agent, artifacts, decisions, and open items. Loop until green.

### PHASE 1: ANALYSIS (Business Analyst)

- Create `docs/project-brief.md` detailing problem, scope, requirements, and KPIs. Hand off to PO.

### PHASE 2: PLANNING (Product Owner)

- Create `docs/prd.md` with RICE-prioritized user stories, Gherkin ACs, and an alignment check matrix. Hand off to Solutioning.

### PHASE 3: SOLUTIONING (Solution Architect → Enterprise Architect → UX/UI Designer → Tech Lead)

- **SA:** Create `docs/architecture/solution-architecture.md` (APIs, data models, diagrams) and ADRs.
- **EA:** Create `docs/architecture/enterprise-architecture.md` (Security, CI/CD, Observability).
- **UX:** Create/Update UI specs, user journeys, and design system in `docs/ux/`.
- **Tech Lead:** Cross-review artifacts, create technical implementation stories in `docs/stories/`, and define `docs/reviews/code-review-checklist.md`. Hand off to Implementation.

### PHASE 4: IMPLEMENTATION (Engineers + QE in parallel)

- **Backend/Frontend/Mobile Engineers:** Implement stories, write unit/integration tests, and mark DoD complete.
  - _If there are Database changes:_ You MUST run Flyway migrations (`make migrate`) and add seed data.
  - _Service Lifeycle:_ You MUST restart affected services via `scripts/start-local.sh` or Docker to ensure changes take effect.
- **Tester & QE:** Create `docs/test-plans/test-strategy.md` and test cases matching PRD requirements. Setup traceability matrix.
  - _Smoke Testing:_ Run `scripts/smoke-test.sh`. All tests MUST pass. If new API flows or services are introduced, you MUST update `scripts/smoke-test.sh` to include them.

### PHASE 5: REVIEW LOOP — ITERATE UNTIL GREEN

This phase MUST loop until all quality gates pass:

1. **Tech Lead (Code Review):** Review implemented code against checklist, ADRs, and ACs. flag 🔴 MUST FIX, 🟡 SHOULD FIX, 🟢 CONSIDER.
2. **Engineers (Fix):** Resolve all 🔴 and 🟡 issues, update tests, and send back to review.
3. **QE (Validate):** Execute full test suite. If any fail, outline defects and route to engineers (back to Step 2). If all pass, state "QE APPROVED".
4. **Tech Lead (Release):** Final readiness check. Create `docs/reviews/release-readiness.md` and state: "✅ RELEASE APPROVED — Epic complete".\* After completing each step, explicitly state which
   step you are moving to next so the execution flow is traceable.
