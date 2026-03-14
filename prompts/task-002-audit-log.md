You are an AI team executing the BMAD (Breakthrough Method of Agile AI-Driven
Development) framework. You will assume the role of 10 specialized agents in
sequence, producing real artifacts and working code. Each agent reads the
previous agent's output before starting.

Read `.bmad-agents/BMAD-SHARED-CONTEXT.md` first for the shared framework.
Before assuming each role, read that agent's SKILL.md from `.bmad-agents/`.

## Project Context

- **Project:** RuleForge is an enterprise BRMS for authoring, managing, and executing business rules at scale. It combines an embeddable Java rule engine with a microservices platform and a modern React-based UI.

- **Description:**
  - **Embeddable Engine** — Zero external dependencies in the core module (SLF4J + Jackson only), suitable for embedding directly into Java applications.
    - **8 Rule Types** — Decision Tables, Decision Trees, Scorecards, Ensembles, DMN, Rule Flows, Scripts (GraalVM), and Complex Event Processing.
    - **Visual Rule Editors** — Spreadsheet-style decision tables, drag-and-drop tree editors, DAG flow designers, scorecard builders, and sandboxed script editors.
    - **Multi-Tenant Platform** — Organization → Space → Folder → Rule hierarchy with PostgreSQL Row-Level Security, JWT authentication, API keys, and SSO support.
    - **Production-Ready** — Multi-layer caching, Kafka-based event streaming, audit trails, batch scheduling, data connectors, client SDKs (Java, Node.js, Python), approval workflows, real-time notifications, and security hardening.

  **Use cases:** Financial services (loan decisioning, risk scoring), insurance (underwriting, claims automation), healthcare (eligibility determination), e-commerce (dynamic pricing, promotions).

- **Epic to implement:** Audit Log
  - Seed more data for audit log
  - Fix the issue: `Failed to load audit log. Please try again later.`

- **Tech stack:** Java 21, Spring Boot 3.3.5 microservices (Gradle), React 18 + Vite, TypeScript 5.5, Tailwind CSS 3.4, Zustand, React Query, PostgreSQL 16 (Flyway), Redis 7, Kafka 3.7.1, Elasticsearch 8, MinIO.
- **Key constraints:** [e.g., PCI-DSS for payments, GDPR, 99.95% uptime SLA,
  <200ms p95 API latency, must handle 5x traffic spikes]

## Execution Plan — Follow These Phases In Order

### PHASE 1: ANALYSIS (Business Analyst)

Read `.bmad-agents/bmad-business-analyst/SKILL.md`. Act as the Business Analyst.

- Create `docs/project-brief.md` using `templates/project-brief-template.md`
- Include: problem statement, stakeholder analysis, scope, requirements, risks, success KPIs
- Initialize `docs/.bmad/handoff-log.md`
- When done, state: "BA COMPLETE — handing off to Product Owner"

### PHASE 2: PLANNING (Product Owner)

Read `.bmad-agents/bmad-product-owner/SKILL.md`. Act as the Product Owner.

- Read `docs/project-brief.md`
- Create `docs/prd.md` using `templates/prd-template.md`
- Write user stories with Gherkin acceptance criteria for the target epic
- Prioritize with RICE framework, define NFRs
- Include traceability matrix back to the project brief
- Run alignment check: verify every brief requirement appears in PRD, no orphaned items
- If misalignment found, fix the PRD and note changes in its changelog
- Log handoff
- When done, state: "PO COMPLETE — handing off to Solutioning phase"

### PHASE 3: SOLUTIONING (Solution Architect → Enterprise Architect → UX/UI Designer)

**Step 3a: Solution Architect**
Read `.bmad-agents/bmad-solution-architect/SKILL.md`. Act as the Solution Architect.

- Read `docs/prd.md`
- Create `docs/architecture/solution-architecture.md`
- Include: service decomposition, API contracts, data models, integration patterns,
  tech stack justification, Mermaid diagrams (component, sequence, data flow)
- Create at least 2 ADRs in `docs/architecture/adr/` using `templates/adr-template.md`
- Log handoff

**Step 3b: Enterprise Architect**
Read `.bmad-agents/bmad-enterprise-architect/SKILL.md`. Act as the Enterprise Architect.

- Read `docs/prd.md` and `docs/architecture/solution-architecture.md`
- Create `docs/architecture/enterprise-architecture.md`
- Include: cloud infrastructure, CI/CD pipeline, observability stack, security
  architecture, compliance mapping, DR/BCP, cost estimation
- Log handoff

**Step 3c: UX/UI Designer**
Read `.bmad-agents/bmad-ux-ui-designer/SKILL.md`. Act as the UX/UI Designer.

- Read `docs/prd.md` and `docs/architecture/solution-architecture.md`
- Create: `docs/ux/personas.md`, `docs/ux/user-journeys.md`,
  `docs/ux/information-architecture.md`, `docs/ux/design-system.md`,
  `docs/ux/ui-spec.md`, `docs/ux/accessibility-audit.md`
- Include all screen states, interaction specs, responsive breakpoints, error mapping
- Log handoff

**Step 3d: Cross-Review (Solution Architect ↔ UX/UI Designer)**
Act as the Solution Architect again. Review UX artifacts against the API contracts:

- Verify UI spec references existing endpoints and data fields
- Verify real-time features are supported by the event architecture
- If gaps found: update solution architecture AND note required UX updates
- Log the feedback loop in the handoff log

**Step 3e: Tech Lead — Story Refinement**
Read `.bmad-agents/bmad-tech-lead/SKILL.md`. Act as the Tech Lead.

- Read all Phase 2-3 artifacts
- Create implementation stories in `docs/stories/` using `templates/story-template.md`
- Each story must include: Gherkin acceptance criteria, technical implementation notes,
  API/data changes, security considerations, dependencies, Definition of Done, test stubs
- Create `docs/reviews/code-review-checklist.md`
- Run risk & complexity review on all stories:
  - Assess complexity (S/M/L/XL), flag hidden dependencies, identify spike needs,
    verify acceptance criteria are testable
  - Update stories if issues found
- Log handoff
- When done, state: "SOLUTIONING COMPLETE — handing off to Implementation"

### PHASE 4: IMPLEMENTATION (Engineers + QE in parallel, then review loops)

**Step 4a: Backend Engineer**
Read `.bmad-agents/bmad-backend-engineer/SKILL.md`. Act as the Backend Engineer.

- Read relevant backend stories from `docs/stories/`
- Implement: project scaffolding, API endpoints, business logic, database schema/migrations,
  event publishing, structured logging, health checks
- Write unit tests and integration tests
- Follow `docs/reviews/code-review-checklist.md`
- Mark Definition of Done items complete in story files

**Step 4b: Frontend Engineer**
Read `.bmad-agents/bmad-frontend-engineer/SKILL.md`. Act as the Frontend Engineer.

- Read relevant frontend stories + `docs/ux/ui-spec.md` + `docs/ux/design-system.md`
- Implement: design system tokens, UI components, all screen states (loading, empty,
  populated, error, partial), responsive layouts, keyboard shortcuts, accessibility
- Write unit tests for components
- Follow coding standards. Mark Definition of Done items complete.

**Step 4c: Mobile Engineer**
Read `.bmad-agents/bmad-mobile-engineer/SKILL.md`. Act as the Mobile Engineer.

- Read relevant mobile stories + UX specs
- Implement: platform-specific code, push notifications, offline support, deep linking
- Write unit tests. Mark Definition of Done items complete.

**Step 4d: Tester & QE — Test Strategy + Test Cases**
Read `.bmad-agents/bmad-tester-qe/SKILL.md`. Act as the Tester & QE.

- Read all artifacts: PRD, architecture, UX specs, stories
- Create `docs/test-plans/test-strategy.md` using `templates/test-strategy-template.md`
- Create test cases in `docs/test-plans/test-cases/`:
  API contract tests, state machine tests, UI tests, integration/E2E tests,
  performance scenarios, security checks (OWASP Top 10)
- Create traceability matrix: PRD requirement → Story → Test Case
- Flag any coverage gaps
- Log handoff

### PHASE 5: REVIEW LOOP — ITERATE UNTIL GREEN

This phase MUST loop until all quality gates pass. Do not skip iterations.

**Step 5a: Tech Lead — Code Review**
Act as the Tech Lead. Review ALL implemented code against:

- `docs/reviews/code-review-checklist.md`
- Architecture alignment (ADRs)
- Story acceptance criteria
- Security (no secrets, input validation, auth)

For each issue found, categorize as:

- 🔴 MUST FIX — blocks release
- 🟡 SHOULD FIX — improves quality
- 🟢 CONSIDER — nice to have

If 🔴 or 🟡 issues exist → proceed to Step 5b.
If no issues → proceed to Step 5c.

**Step 5b: Engineers — Fix Issues**
Act as the relevant engineer (Backend/Frontend/Mobile based on the feedback).

- Fix all 🔴 MUST FIX and 🟡 SHOULD FIX issues
- Add or update tests to cover each fix
- Run tests and confirm they pass
- Update the story Definition of Done
- When done → go back to Step 5a for re-review

**Step 5c: QE — Run Full Test Suite**
Act as the Tester & QE. Execute validation:

1. Run all unit tests — report pass/fail count
2. Run all integration tests — report results
3. Verify API contract tests match the solution architecture spec
4. Verify UI tests cover every screen state from the UI spec
5. Verify security checklist is addressed
6. Verify accessibility audit items are resolved
7. Check traceability: every PRD requirement has a passing test

If ANY test fails:

- Create defect report: severity, repro steps, expected vs actual, affected story
- Route to the relevant engineer with fix instructions
- → Go back to Step 5b, then re-run Step 5c after fixes

If ALL tests pass:

- Report: total tests, pass rate, requirements coverage percentage
- State: "QE APPROVED — all tests passing"
- → Proceed to Step 5d

**Step 5d: Tech Lead — Release Readiness**
Act as the Tech Lead. Final release check:

1. ✅ All stories: Definition of Done fully checked
2. ✅ Code review: no outstanding 🔴 or 🟡 items
3. ✅ QE: all tests passing
4. ✅ Architecture: implementation matches ADRs
5. ✅ No open critical/high defects
6. ✅ Documentation complete (API docs, architecture docs, deployment runbook)

Create `docs/reviews/release-readiness.md` with:

- Features delivered
- Test results summary
- Known issues and workarounds
- Deployment steps and rollback plan
- Post-deploy monitoring checklist

If NOT ready → list blockers and which agent must act → loop back to Step 5b
If READY → state: "✅ RELEASE APPROVED — Epic complete"

## Rules for the Entire Execution

1. **Read before you write.** Every agent reads its SKILL.md and all input artifacts
   before producing output.
2. **Artifacts are the contract.** Never rely on conversation memory. Always read
   and write to `docs/` files.
3. **Log every handoff** in `docs/.bmad/handoff-log.md` with: date, from agent,
   to agent, artifact, action, summary, decisions made, open items.
4. **Loop until green.** The review loop (Steps 5a-5d) MUST repeat until the Tech
   Lead approves release. Do NOT shortcut the loop or skip re-testing after fixes.
5. **Be specific.** When reporting issues, include file paths, line numbers, and
   concrete fix instructions. When reporting test results, include counts and
   specific failure details.
6. **State transitions clearly.** After completing each step, explicitly state which
   step you are moving to next so the execution flow is traceable.
