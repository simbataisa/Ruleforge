# BMAD SDLC Agent Skills

A complete set of 10 specialized AI agent skills implementing the **BMAD Method** (Breakthrough Method of Agile AI-Driven Development) for enterprise software development. Each agent is a self-contained markdown skill that can be plugged into any AI coding tool.

## Agent Team

| Agent                    | Skill Directory              | BMAD Phase     | Role                                                         |
| ------------------------ | ---------------------------- | -------------- | ------------------------------------------------------------ |
| **Business Analyst**     | `bmad-business-analyst/`     | Analysis       | Problem exploration, stakeholder analysis, project brief     |
| **Product Owner**        | `bmad-product-owner/`        | Planning       | PRD, backlog prioritization, artifact alignment              |
| **Solution Architect**   | `bmad-solution-architect/`   | Solutioning    | Service decomposition, API contracts, data models, ADRs      |
| **Enterprise Architect** | `bmad-enterprise-architect/` | Solutioning    | Cloud infra, compliance, observability, CI/CD, FinOps        |
| **UX/UI Designer**       | `bmad-ux-ui-designer/`       | Solutioning    | Personas, journeys, wireframes, design system, accessibility |
| **Tech Lead**            | `bmad-tech-lead/`            | All Phases     | Orchestration, code review, risk, release readiness          |
| **Tester & QE**          | `bmad-tester-qe/`            | All Phases     | Test strategy, quality gates, security testing               |
| **Backend Engineer**     | `bmad-backend-engineer/`     | Implementation | APIs, data layers, event-driven services                     |
| **Frontend Engineer**    | `bmad-frontend-engineer/`    | Implementation | React/TypeScript, state management, a11y                     |
| **Mobile Engineer**      | `bmad-mobile-engineer/`      | Implementation | iOS, Android, React Native, Flutter                          |

## BMAD Four-Phase Workflow

```
Analysis → Planning → Solutioning ──────→ Implementation
  BA          PO       SA / EA / UX          BE / FE / ME
                       Tech Lead ←──────────→ QE (all phases)
```

1. **Analysis** — Business Analyst explores problem space, produces Project Brief
2. **Planning** — Product Owner creates PRD, prioritizes backlog, aligns artifacts
3. **Solutioning** — Architects + UX/UI Designer design the system; Tech Lead refines stories
4. **Implementation** — Engineers build; QE validates; Tech Lead coordinates

## Shared Resources

| Resource                              | Purpose                                                                          |
| ------------------------------------- | -------------------------------------------------------------------------------- |
| `BMAD-SHARED-CONTEXT.md`              | Shared context all agents reference (phases, handoff model, directory structure) |
| `templates/project-brief-template.md` | Analysis phase output template                                                   |
| `templates/prd-template.md`           | Product Requirements Document template                                           |
| `templates/adr-template.md`           | Architecture Decision Record template                                            |
| `templates/story-template.md`         | Implementation story with full context                                           |
| `templates/test-strategy-template.md` | QE test strategy template                                                        |
| `templates/handoff-log-template.md`   | Agent-to-agent handoff tracking                                                  |

---

## Setup Guide — How to Enable These Skills

Each AI coding tool has its own mechanism for loading custom instructions and agent personas. Below are setup instructions for the most popular tools. The core idea is always the same: place the SKILL.md files where your tool can read them, and reference them in the tool's configuration.

### Claude Code (CLI)

Claude Code reads project instructions from a `CLAUDE.md` file at the project root. It also supports `.claude/` directory for additional configuration. There is no separate "skills folder" to install into — you simply reference the agent files from `CLAUDE.md` and Claude Code reads them on demand.

**Step 1: Copy agents into your project**

```bash
# From your project root
cp -r bmad-sdlc-agents/ .bmad-agents/
```

**Step 2: Reference agents from CLAUDE.md**

Create or append to `CLAUDE.md` at your project root:

```bash
cat >> CLAUDE.md << 'EOF'

## BMAD Agent Skills

This project uses BMAD method agents for structured SDLC. Agent skill definitions
are in `.bmad-agents/`. When working on:

- Requirements or analysis: read `.bmad-agents/bmad-business-analyst/SKILL.md`
- Product planning or PRD: read `.bmad-agents/bmad-product-owner/SKILL.md`
- System architecture: read `.bmad-agents/bmad-solution-architect/SKILL.md`
- Enterprise/cloud architecture: read `.bmad-agents/bmad-enterprise-architect/SKILL.md`
- UX/UI design: read `.bmad-agents/bmad-ux-ui-designer/SKILL.md`
- Technical leadership or code review: read `.bmad-agents/bmad-tech-lead/SKILL.md`
- Testing or QA: read `.bmad-agents/bmad-tester-qe/SKILL.md`
- Backend development: read `.bmad-agents/bmad-backend-engineer/SKILL.md`
- Frontend development: read `.bmad-agents/bmad-frontend-engineer/SKILL.md`
- Mobile development: read `.bmad-agents/bmad-mobile-engineer/SKILL.md`

Always read `.bmad-agents/BMAD-SHARED-CONTEXT.md` first for the overall workflow
and artifact structure. All artifacts go in `docs/` per the BMAD directory convention.
EOF
```

Claude Code will read the relevant SKILL.md file when you prompt it with a matching task (e.g., "Act as the Solution Architect and design the system architecture").

### Cowork (Claude Desktop App)

Cowork has a built-in skill system that auto-triggers skills based on the `description` field in each SKILL.md frontmatter. Skills live inside a managed `.skills/skills/` directory within your workspace.

To install, copy each agent folder into your workspace's skill directory:

```bash
# Cowork manages the .skills/skills/ path within your selected folder
cp -r bmad-sdlc-agents/bmad-business-analyst/ .skills/skills/bmad-business-analyst/
cp -r bmad-sdlc-agents/bmad-product-owner/ .skills/skills/bmad-product-owner/
cp -r bmad-sdlc-agents/bmad-solution-architect/ .skills/skills/bmad-solution-architect/
cp -r bmad-sdlc-agents/bmad-enterprise-architect/ .skills/skills/bmad-enterprise-architect/
cp -r bmad-sdlc-agents/bmad-ux-ui-designer/ .skills/skills/bmad-ux-ui-designer/
cp -r bmad-sdlc-agents/bmad-tech-lead/ .skills/skills/bmad-tech-lead/
cp -r bmad-sdlc-agents/bmad-tester-qe/ .skills/skills/bmad-tester-qe/
cp -r bmad-sdlc-agents/bmad-backend-engineer/ .skills/skills/bmad-backend-engineer/
cp -r bmad-sdlc-agents/bmad-frontend-engineer/ .skills/skills/bmad-frontend-engineer/
cp -r bmad-sdlc-agents/bmad-mobile-engineer/ .skills/skills/bmad-mobile-engineer/
```

Once installed, skills auto-trigger when your prompts match their description keywords.

---

### Cursor

Cursor uses `.cursor/rules/` for project-level rules and supports `.cursorrules` at the project root.

**Option A: As Cursor Rules (per-agent files)**

```bash
mkdir -p .cursor/rules/

# Copy each agent as a separate rule file
for agent in bmad-business-analyst bmad-product-owner bmad-solution-architect \
  bmad-enterprise-architect bmad-ux-ui-designer bmad-tech-lead bmad-tester-qe \
  bmad-backend-engineer bmad-frontend-engineer bmad-mobile-engineer; do
  cp "bmad-sdlc-agents/$agent/SKILL.md" ".cursor/rules/$agent.md"
done

# Copy shared context
cp bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md .cursor/rules/000-bmad-shared-context.md
```

**Option B: As .cursorrules (single file)**

Concatenate all agents into a single `.cursorrules` file at project root. Prefix each section with a clear header so Cursor knows which persona to invoke:

```bash
echo "# BMAD SDLC Agent Framework" > .cursorrules
echo "" >> .cursorrules
cat bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md >> .cursorrules
for agent in bmad-sdlc-agents/bmad-*/; do
  echo -e "\n---\n" >> .cursorrules
  cat "$agent/SKILL.md" >> .cursorrules
done
```

**Option C: Using Cursor Notepads**

Create a Notepad for each agent in Cursor's sidebar. Copy the SKILL.md content into each notepad. Reference the relevant notepad with `@notepad-name` when prompting.

---

### Windsurf (Codeium)

Windsurf uses `.windsurfrules` at the project root or `.windsurf/rules/` directory.

```bash
# Single file approach
echo "# BMAD SDLC Agent Framework" > .windsurfrules
cat bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md >> .windsurfrules
for agent in bmad-sdlc-agents/bmad-*/; do
  echo -e "\n---\n" >> .windsurfrules
  cat "$agent/SKILL.md" >> .windsurfrules
done

# Or as individual rule files
mkdir -p .windsurf/rules/
for agent in bmad-sdlc-agents/bmad-*/; do
  name=$(basename "$agent")
  cp "$agent/SKILL.md" ".windsurf/rules/$name.md"
done
```

---

### GitHub Copilot

Copilot supports custom instructions via `.github/copilot-instructions.md` and individual prompt files in `.github/prompts/`.

**Option A: Prompt files (recommended — one per agent)**

```bash
mkdir -p .github/prompts/

for agent in bmad-sdlc-agents/bmad-*/; do
  name=$(basename "$agent")
  cp "$agent/SKILL.md" ".github/prompts/$name.prompt.md"
done

# Shared context as a separate prompt
cp bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md .github/prompts/bmad-shared-context.prompt.md
```

Reference agents in Copilot Chat with `#bmad-backend-engineer` or by mentioning the prompt file.

**Option B: Single instructions file**

```bash
cat bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md > .github/copilot-instructions.md
for agent in bmad-sdlc-agents/bmad-*/; do
  echo -e "\n---\n" >> .github/copilot-instructions.md
  cat "$agent/SKILL.md" >> .github/copilot-instructions.md
done
```

---

### OpenAI Codex CLI

Codex reads project instructions from `AGENTS.md` or `codex.md` at the project root, and supports per-directory `AGENTS.md` files.

```bash
# Copy shared context as the root instructions
cp bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md AGENTS.md

# Append agent reference index
cat >> AGENTS.md << 'EOF'

## Agent Skills

Each BMAD agent skill is in `.bmad-agents/<agent-name>/SKILL.md`.
Read the relevant SKILL.md before performing that agent's role.
EOF

# Copy agents for reference
cp -r bmad-sdlc-agents/ .bmad-agents/
```

Alternatively, create an `agents/` directory with one file per agent:

```bash
mkdir -p agents/
for agent in bmad-sdlc-agents/bmad-*/; do
  name=$(basename "$agent")
  cp "$agent/SKILL.md" "agents/$name.md"
done
```

---

### Google Gemini CLI

Gemini CLI reads instructions from `GEMINI.md` at the project root and supports `.gemini/` configuration.

```bash
# Create GEMINI.md with BMAD context
cp bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md GEMINI.md

cat >> GEMINI.md << 'EOF'

## BMAD Agent Skills

Agent skill definitions are in `.bmad-agents/`. Read the relevant SKILL.md
before performing that role. Available agents:

- Business Analyst: `.bmad-agents/bmad-business-analyst/SKILL.md`
- Product Owner: `.bmad-agents/bmad-product-owner/SKILL.md`
- Solution Architect: `.bmad-agents/bmad-solution-architect/SKILL.md`
- Enterprise Architect: `.bmad-agents/bmad-enterprise-architect/SKILL.md`
- UX/UI Designer: `.bmad-agents/bmad-ux-ui-designer/SKILL.md`
- Tech Lead: `.bmad-agents/bmad-tech-lead/SKILL.md`
- Tester & QE: `.bmad-agents/bmad-tester-qe/SKILL.md`
- Backend Engineer: `.bmad-agents/bmad-backend-engineer/SKILL.md`
- Frontend Engineer: `.bmad-agents/bmad-frontend-engineer/SKILL.md`
- Mobile Engineer: `.bmad-agents/bmad-mobile-engineer/SKILL.md`
EOF

cp -r bmad-sdlc-agents/ .bmad-agents/
```

---

### OpenCode

OpenCode reads instructions from `OPENCODE.md` at the project root.

```bash
cp bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md OPENCODE.md

cat >> OPENCODE.md << 'EOF'

## BMAD Agent Skills

Agent definitions are in `.bmad-agents/`. Read the relevant SKILL.md for each role.
See the agent roster table above for the full list.
EOF

cp -r bmad-sdlc-agents/ .bmad-agents/
```

---

### Aider

Aider uses `--read` flags or `.aider.conf.yml` to load reference files.

```bash
# In .aider.conf.yml
cat > .aider.conf.yml << 'EOF'
read:
  - .bmad-agents/BMAD-SHARED-CONTEXT.md
  # Add specific agents as needed:
  # - .bmad-agents/bmad-backend-engineer/SKILL.md
  # - .bmad-agents/bmad-tech-lead/SKILL.md
EOF

cp -r bmad-sdlc-agents/ .bmad-agents/
```

Or load agents dynamically per session:

```bash
aider --read .bmad-agents/bmad-backend-engineer/SKILL.md src/
```

---

### Cline (VS Code Extension)

Cline reads `.clinerules` at the project root or custom instructions in its settings.

```bash
# As .clinerules
echo "# BMAD SDLC Agent Framework" > .clinerules
cat bmad-sdlc-agents/BMAD-SHARED-CONTEXT.md >> .clinerules
for agent in bmad-sdlc-agents/bmad-*/; do
  echo -e "\n---\n" >> .clinerules
  cat "$agent/SKILL.md" >> .clinerules
done

# Or copy agents and reference from .clinerules
cp -r bmad-sdlc-agents/ .bmad-agents/
```

---

### Any Other AI Coding Tool

The pattern works universally. Every AI coder has some way to inject custom instructions:

1. **Copy** the `bmad-sdlc-agents/` directory into your project (as `.bmad-agents/` or similar)
2. **Reference** the SKILL.md files from your tool's instructions/rules file
3. **Instruct** the AI to read the relevant SKILL.md before performing a role

The key instruction to include in whatever config file your tool uses:

```
This project uses BMAD method for structured SDLC. Agent skill definitions
are in `.bmad-agents/`. Before performing any SDLC role, read the matching
SKILL.md file and follow its persona, workflow, and artifact conventions.
Always read BMAD-SHARED-CONTEXT.md first for the overall framework.
```

---

## How to Use the Agents in Practice

### Agent Handoff Flow

Each agent reads artifacts from the previous phase and produces artifacts for the next:

```
BA creates:     docs/project-brief.md
                    ↓
PO creates:     docs/prd.md
                    ↓
SA creates:     docs/architecture/solution-architecture.md
EA creates:     docs/architecture/enterprise-architecture.md
UX creates:     docs/ux/design-system.md, wireframes, ui-spec
                    ↓
Tech Lead:      docs/stories/epic-N/story-N.N.md (refined)
                    ↓
Engineers:      src/ (implementation)
QE:             docs/test-plans/ + tests/
```

### Collaborative Iteration

Agents can loop back at any point. For example:

- Solution Architect realizes a PRD requirement is ambiguous → asks Product Owner to clarify
- Tech Lead spots hidden complexity in a story → sends back to Solution Architect
- QE finds a gap in test coverage → requests UX/UI Designer to clarify interaction spec
- Backend Engineer discovers an API contract issue → loops back to Solution Architect

All handoffs are logged in `docs/.bmad/handoff-log.md`.

---

## Sample Prompts — Full SDLC Walkthrough

Below is a complete sequence of prompts that walks a project through all four BMAD phases, invokes every agent, and includes iterative review loops. Copy these into your AI coding tool one at a time. Each prompt builds on the artifacts produced by the previous one.

The example project is an **Enterprise Order Management System** — a microservices-based platform handling order creation, payment processing, inventory management, and fulfillment tracking.

### Phase 1: Analysis

#### Prompt 1 — Business Analyst: Create the Project Brief

```
Read `.bmad-agents/BMAD-SHARED-CONTEXT.md` and `.bmad-agents/bmad-business-analyst/SKILL.md`.

Act as the BMAD Business Analyst.

We need to build an Enterprise Order Management System (OMS) for a mid-size
e-commerce company processing ~50,000 orders/day. The current monolithic system
is hitting scaling limits, has no real-time inventory visibility, and the
checkout failure rate is 12%.

Key stakeholders: VP of Engineering, Head of Product, Operations Manager,
Customer Support Lead.

Create a comprehensive project brief at `docs/project-brief.md` using the
template from `templates/project-brief-template.md`. Include:
- Problem statement with quantified pain points
- Stakeholder analysis with interest/influence mapping
- In-scope: order lifecycle, payment processing, inventory sync, fulfillment tracking
- Out-of-scope: warehouse robotics, supplier portal (phase 2)
- High-level functional and non-functional requirements
- Risk assessment
- Success criteria with measurable KPIs

Also initialize `docs/.bmad/handoff-log.md` with the first entry.
```

### Phase 2: Planning

#### Prompt 2 — Product Owner: Create the PRD

```
Read `.bmad-agents/bmad-product-owner/SKILL.md`.

Act as the BMAD Product Owner. Read `docs/project-brief.md` produced by the BA.

Create `docs/prd.md` using `templates/prd-template.md`. Organize requirements into
epics and user stories:

Epic 1 — Order Lifecycle Management (Must Have)
Epic 2 — Payment Processing & Reconciliation (Must Have)
Epic 3 — Real-Time Inventory Sync (Must Have)
Epic 4 — Fulfillment Tracking & Notifications (Should Have)
Epic 5 — Analytics Dashboard (Could Have)

For each epic, write 3-5 user stories with Gherkin acceptance criteria.
Prioritize using RICE framework. Define NFRs for:
- Performance: <200ms p95 API response, >1000 RPS
- Availability: 99.95% uptime SLA
- Security: PCI-DSS for payments, GDPR for customer data
- Scalability: handle 5x traffic spikes during flash sales

Include a traceability matrix mapping every requirement back to the project brief.
Log the handoff in `docs/.bmad/handoff-log.md`.
```

#### Prompt 2a — Product Owner: Alignment Check (iterative loop)

```
Act as the BMAD Product Owner.

Review `docs/project-brief.md` and `docs/prd.md` side by side.
Run the alignment checklist:
- Does every project brief requirement appear in the PRD?
- Are there PRD items that aren't traceable to the brief?
- Are priorities consistent between documents?
- Are NFR targets realistic given the brief's constraints?

If you find misalignment, fix it in the PRD and note what changed in the
changelog section. Update the handoff log.
```

### Phase 3: Solutioning

#### Prompt 3 — Solution Architect: Design the System

```
Read `.bmad-agents/bmad-solution-architect/SKILL.md`.

Act as the BMAD Solution Architect. Read `docs/prd.md`.

Design the solution architecture for the Order Management System. Create
`docs/architecture/solution-architecture.md` including:

1. Service decomposition — identify microservices (Order Service, Payment Service,
   Inventory Service, Fulfillment Service, Notification Service, API Gateway)
2. API contracts — define key endpoints for each service (REST for sync,
   AsyncAPI for events)
3. Data model — entity diagrams for each service's bounded context
4. Integration patterns — event-driven with Kafka for inter-service communication,
   saga pattern for distributed transactions (order→payment→inventory)
5. Technology stack selection with justification
6. Mermaid diagrams: component diagram, sequence diagram for order placement flow,
   data flow diagram

Create ADRs for at least 3 key decisions:
- `docs/architecture/adr/ADR-001-event-driven-architecture.md`
- `docs/architecture/adr/ADR-002-database-per-service.md`
- `docs/architecture/adr/ADR-003-saga-pattern-for-distributed-transactions.md`

Use the ADR template from `templates/adr-template.md`.
Log the handoff in `docs/.bmad/handoff-log.md`.
```

#### Prompt 4 — Enterprise Architect: Cloud & Infrastructure

```
Read `.bmad-agents/bmad-enterprise-architect/SKILL.md`.

Act as the BMAD Enterprise Architect. Read `docs/prd.md` and
`docs/architecture/solution-architecture.md`.

Create `docs/architecture/enterprise-architecture.md` covering:

1. Cloud infrastructure on AWS — EKS for container orchestration, RDS/Aurora
   for databases, MSK for Kafka, ElastiCache for Redis
2. Multi-environment strategy: dev, staging, prod with IaC (Terraform)
3. CI/CD pipeline: GitHub Actions → build → test → staging → canary deploy → prod
4. Observability stack: Prometheus + Grafana for metrics, ELK for logs,
   Jaeger for distributed tracing, PagerDuty for alerting
5. Security architecture: VPC layout, IAM roles, secrets management (AWS Secrets
   Manager), WAF, network policies
6. Compliance: PCI-DSS scope isolation for payment service, GDPR data handling
7. Disaster recovery: multi-AZ deployment, RTO < 15min, RPO < 1min
8. Cost estimation and FinOps tagging strategy

Create `docs/architecture/adr/ADR-004-aws-eks-container-orchestration.md`.
Log the handoff.
```

#### Prompt 5 — UX/UI Designer: Design the Experience

```
Read `.bmad-agents/bmad-ux-ui-designer/SKILL.md`.

Act as the BMAD UX/UI Designer. Read `docs/prd.md` and
`docs/architecture/solution-architecture.md`.

Create the following UX artifacts:

1. `docs/ux/personas.md` — at least 3 personas: Operations Manager (power user),
   Customer Support Agent (daily user), System Administrator (config user)
2. `docs/ux/user-journeys.md` — map the critical flows:
   - Order placement (happy path + payment failure + inventory conflict)
   - Order tracking and status updates
   - Bulk order management for operations
   Include Mermaid task flow diagrams for each journey.
3. `docs/ux/information-architecture.md` — navigation structure for the admin dashboard
4. `docs/ux/design-system.md` — design tokens (colors, typography, spacing, elevation)
   and component library (buttons, forms, tables, feedback components)
5. `docs/ux/ui-spec.md` — detailed spec for the Order Dashboard screen including:
   - All screen states (loading, empty, populated, error, partial)
   - Interaction specs with animations and feedback
   - Responsive breakpoints
   - Keyboard shortcuts
   - Error state mapping for all HTTP status codes
6. `docs/ux/accessibility-audit.md` — WCAG 2.2 AA checklist for the dashboard

Log the handoff.
```

#### Prompt 5a — Solution Architect ↔ UX/UI Designer: Cross-Review (iterative loop)

```
Act as the BMAD Solution Architect.

Review `docs/ux/ui-spec.md` and `docs/ux/design-system.md` against the API
contracts in `docs/architecture/solution-architecture.md`.

Check for:
- Does the UI spec reference API endpoints that actually exist?
- Are the data fields in the UI spec consistent with the data model?
- Are real-time features (order status updates) supported by the event architecture?
- Are there UI interactions that require APIs not yet designed?

If you find gaps, update the solution architecture to add missing endpoints or
event streams, and note what the UX/UI Designer should update. Log the feedback
loop in the handoff log.
```

#### Prompt 6 — Tech Lead: Refine Stories

```
Read `.bmad-agents/bmad-tech-lead/SKILL.md`.

Act as the BMAD Tech Lead. Read all artifacts:
- `docs/prd.md`
- `docs/architecture/solution-architecture.md`
- `docs/architecture/enterprise-architecture.md`
- `docs/ux/ui-spec.md`

Create refined implementation stories for Epic 1 (Order Lifecycle Management).
For each story, use `templates/story-template.md` and save to
`docs/stories/epic-1/`:

- `story-1.1.md` — Order Service: Create Order API (Backend)
- `story-1.2.md` — Order Service: Order State Machine (Backend)
- `story-1.3.md` — Order Dashboard: Order List View (Frontend)
- `story-1.4.md` — Order Dashboard: Order Detail View (Frontend)
- `story-1.5.md` — Order Events: Publish order lifecycle events to Kafka (Backend)
- `story-1.6.md` — Order Notifications: Mobile push for order status (Mobile)

Each story must include:
- Gherkin acceptance criteria
- Technical implementation notes referencing the architecture
- API changes and data changes
- Security considerations
- Dependencies on other stories
- Definition of Done checklist
- Test case stubs

Also create `docs/reviews/code-review-checklist.md` with standards for this project.
Log the handoff.
```

#### Prompt 6a — Tech Lead: Risk & Complexity Review (iterative loop)

```
Act as the BMAD Tech Lead.

Review all stories in `docs/stories/epic-1/`. For each story:
1. Assess technical complexity (S/M/L/XL) — adjust if your estimate differs
   from the original
2. Identify hidden dependencies not captured in the dependency table
3. Flag stories that need a technical spike before implementation
4. Check that acceptance criteria are testable and unambiguous
5. Verify that the Definition of Done is achievable

If you find issues, update the story files directly and add a note in the
changelog section explaining what changed and why. Create spike stories if needed.
Update the handoff log.
```

### Phase 4: Implementation

#### Prompt 7 — Backend Engineer: Implement Order Service

```
Read `.bmad-agents/bmad-backend-engineer/SKILL.md`.

Act as the BMAD Backend Engineer. Read:
- `docs/stories/epic-1/story-1.1.md` (Create Order API)
- `docs/stories/epic-1/story-1.2.md` (Order State Machine)
- `docs/architecture/solution-architecture.md`

Implement the Order Service:
1. Project scaffolding with the tech stack from the architecture doc
2. Create Order API — POST /api/v1/orders with request validation,
   idempotency key support, and proper error responses
3. Order state machine — states: CREATED → PAYMENT_PENDING → PAID →
   FULFILLMENT_PENDING → SHIPPED → DELIVERED / CANCELLED
4. Database schema and migrations
5. Unit tests for the state machine and API validation
6. Integration tests for the API endpoints
7. Structured logging and health check endpoint

Follow the coding standards from `docs/reviews/code-review-checklist.md`.
When done, mark the Definition of Done items as complete in the story files.
```

#### Prompt 8 — Frontend Engineer: Implement Order Dashboard

```
Read `.bmad-agents/bmad-frontend-engineer/SKILL.md`.

Act as the BMAD Frontend Engineer. Read:
- `docs/stories/epic-1/story-1.3.md` (Order List View)
- `docs/ux/ui-spec.md`
- `docs/ux/design-system.md`

Implement the Order Dashboard:
1. Project setup with React + TypeScript following the architecture doc
2. Design system tokens as CSS custom properties / Tailwind config
3. Order List View component:
   - Sortable, filterable, paginated table
   - All 5 screen states: loading (skeleton), empty, populated, error, partial
   - Real-time status badge updates
   - Bulk selection and actions
4. Responsive layout for desktop, tablet, mobile breakpoints per UI spec
5. Keyboard shortcuts from the UI spec (/ for search, Esc to close)
6. Unit tests for components and state management
7. Accessibility: ARIA labels, focus management, screen reader testing notes

Apply the design tokens and component specs from `docs/ux/design-system.md`.
Follow coding standards from `docs/reviews/code-review-checklist.md`.
Update Definition of Done in the story file.
```

#### Prompt 9 — Mobile Engineer: Implement Order Notifications

```
Read `.bmad-agents/bmad-mobile-engineer/SKILL.md`.

Act as the BMAD Mobile Engineer. Read:
- `docs/stories/epic-1/story-1.6.md` (Mobile push for order status)
- `docs/ux/ui-spec.md`
- `docs/architecture/solution-architecture.md`

Implement mobile order status notifications:
1. Push notification service integration (FCM for Android, APNs for iOS)
2. Notification payload handling with deep links to order detail
3. In-app notification center with read/unread state
4. Offline queue — store notifications locally when offline, sync on reconnect
5. Platform-specific UI following the design system tokens
6. Unit tests for notification parsing and offline queue
7. Integration test for push notification receipt

Handle edge cases: notification permissions denied, background/foreground state,
notification grouping for multiple order updates.
Update Definition of Done in the story file.
```

#### Prompt 10 — Tester & QE: Test Strategy and Execution

```
Read `.bmad-agents/bmad-tester-qe/SKILL.md`.

Act as the BMAD Tester & QE. Read all artifacts:
- `docs/prd.md`
- `docs/architecture/solution-architecture.md`
- `docs/ux/ui-spec.md`
- `docs/stories/epic-1/` (all stories)

Create the test strategy and execute validation:

1. Create `docs/test-plans/test-strategy.md` using `templates/test-strategy-template.md`
2. Create test cases in `docs/test-plans/test-cases/`:
   - `tc-order-api.md` — API contract tests for Order Service
   - `tc-order-state-machine.md` — State transition validation (all valid/invalid transitions)
   - `tc-order-dashboard.md` — UI test cases matching every UI spec screen state
   - `tc-order-notifications.md` — Push notification delivery and deep link tests
   - `tc-integration.md` — End-to-end order flow (create → pay → fulfill → notify)
   - `tc-performance.md` — Load test scenarios (1000 RPS, flash sale 5x spike)
   - `tc-security.md` — OWASP Top 10 checks, PCI-DSS scope validation
3. Create the traceability matrix: PRD requirement → Story → Test Case
4. Run available unit and integration tests, report results
5. Flag any gaps: stories without test coverage, UI states without test cases,
   API endpoints without contract tests

Update the handoff log.
```

### Review Loops — Iterate Until Green

#### Prompt 11 — Tech Lead: Code Review

```
Act as the BMAD Tech Lead.

Review all implemented code against:
- `docs/reviews/code-review-checklist.md`
- `docs/architecture/solution-architecture.md` (architecture alignment)
- `docs/stories/epic-1/` (acceptance criteria met?)

For each component (Order Service, Order Dashboard, Mobile Notifications):
1. Check architecture alignment — does the implementation match the ADRs?
2. Check coding standards — naming, error handling, logging, test coverage
3. Check security — input validation, auth, no secrets in code
4. Check the Definition of Done in each story file — are all items checked?
5. Note any technical debt introduced

If you find issues:
- Create specific, actionable feedback with file paths and line references
- Categorize as: MUST FIX (blocks merge), SHOULD FIX (improves quality),
  CONSIDER (nice to have)
- Send feedback back to the relevant engineer agent

Log the review in `docs/.bmad/handoff-log.md`.
```

#### Prompt 12 — Fix Issues and Re-Test (iterative loop)

```
Read the Tech Lead's review feedback from the handoff log.

Act as the relevant engineer (Backend/Frontend/Mobile — based on the feedback).

Fix all MUST FIX and SHOULD FIX issues identified in the code review.
For each fix:
1. Implement the change
2. Add or update tests to cover the fix
3. Run tests and confirm they pass
4. Update the story's Definition of Done

When all fixes are done, hand back to the Tech Lead for re-review.
```

#### Prompt 13 — QE: Final Validation Pass

```
Act as the BMAD Tester & QE.

Run the complete test suite and validate:
1. All unit tests pass
2. All integration tests pass
3. API contract tests match the spec in `docs/architecture/solution-architecture.md`
4. UI tests cover every screen state from `docs/ux/ui-spec.md`
5. Performance targets from the PRD are met (run load test if possible)
6. Security checklist from `docs/test-plans/test-strategy.md` is green
7. Accessibility audit from `docs/ux/accessibility-audit.md` is addressed

If ANY tests fail:
- Create a defect report with: severity, steps to reproduce, expected vs actual,
  which story/requirement is affected
- Route back to the relevant engineer with specific fix instructions
- This prompt should be RE-RUN after fixes until all tests pass

If ALL tests pass:
- Update test results in the test plan
- Mark the epic as QE-approved in the handoff log
- Summarize coverage: X tests, Y% pass rate, Z requirements fully covered
```

#### Prompt 14 — Tech Lead: Release Readiness

```
Act as the BMAD Tech Lead.

Perform the final release readiness check:
1. All stories in Epic 1 have Definition of Done fully checked
2. All code reviews are approved (no outstanding MUST FIX items)
3. All QE tests pass
4. Architecture alignment confirmed
5. No critical or high-severity defects open
6. Documentation is complete:
   - API docs match implementation
   - Architecture docs reflect actual decisions
   - Deployment runbook exists

Produce a release summary in `docs/reviews/release-readiness-epic-1.md`:
- Features included
- Test results summary
- Known issues and workarounds
- Deployment steps
- Rollback plan
- Monitoring checklist for post-deploy

If NOT ready: list specific blockers and which agent needs to act.
If READY: approve the release and log it in the handoff log.
```

### Iteration Pattern Summary

The prompts above are designed to loop. The general pattern is:

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│   Phase 1-3: Create artifacts                           │
│       ↓                                                 │
│   Cross-review: Agents check each other's work          │
│       ↓                                                 │
│   Phase 4: Implement from stories                       │
│       ↓                                                 │
│   Tech Lead: Code review                                │
│       ↓                                                 │
│   ┌─ Issues found? ──→ Engineer fixes ──→ Re-review ─┐  │
│   │                                                   │  │
│   └─ No issues ──→ QE: Run all tests                  │  │
│                        ↓                              │  │
│                   ┌─ Tests fail? ──→ Engineer fixes ──┘  │
│                   │                                      │
│                   └─ All pass ──→ Tech Lead: Release ✓   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

Re-run Prompt 12 (fix issues) and Prompt 13 (QE validation) as many times as
needed until all tests pass and the Tech Lead approves the release.

---

## Squad Prompt — Full Agent Team on One Epic

The prompt below orchestrates **all 10 agents** to execute a complete epic from
analysis through passing tests in a single conversation. It is designed to loop
autonomously: agents hand off artifacts, cross-review, implement, test, fix, and
re-test until the Tech Lead signs off on release readiness.

Copy this prompt into your AI coding tool. Replace the `[PLACEHOLDERS]` with your
project details.

### The Squad Prompt

````
You are an AI team executing the BMAD (Breakthrough Method of Agile AI-Driven
Development) framework. You will assume the role of 10 specialized agents in
sequence, producing real artifacts and working code. Each agent reads the
previous agent's output before starting.

Read `.bmad-agents/BMAD-SHARED-CONTEXT.md` first for the shared framework.
Before assuming each role, read that agent's SKILL.md from `.bmad-agents/`.

## Project Context

- **Project:** [PROJECT NAME — e.g., Enterprise Order Management System]
- **Description:** [1-2 sentences — e.g., Microservices platform handling order
  creation, payment processing, inventory management, and fulfillment tracking
  for an e-commerce company processing ~50,000 orders/day]
- **Epic to implement:** [EPIC NAME — e.g., Epic 1: Order Lifecycle Management]
- **Tech stack preferences (optional):** [e.g., "Team knows Java/Kotlin and React"
  or "Must run on AWS" or "Self-hosted auth required" or "Let architects decide
  using references/technology-radar.md"]
- **Key constraints:** [e.g., PCI-DSS for payments, GDPR data sovereignty,
  99.95% uptime SLA, <200ms p95 API latency, must handle 5x traffic spikes,
  multi-cloud portability required]

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
Read `.bmad-agents/bmad-solution-architect/SKILL.md` AND `references/technology-radar.md`.
Act as the Solution Architect.
- Read `docs/prd.md`
- Create `docs/architecture/solution-architecture.md`
- Include: service decomposition, API contracts, data models, integration patterns,
  Mermaid diagrams (component, sequence, data flow)
- **Select technology stack using the Technology Radar decision frameworks** —
  evaluate backend languages, databases, messaging, API gateway, auth, design
  patterns, and workflow engines against project constraints. Use the weighted
  decision matrix template. Document what you chose AND what you rejected.
- Create at least 2 ADRs in `docs/architecture/adr/` using `templates/adr-template.md`
  (include one for the primary technology selection decisions)
- Log handoff

**Step 3b: Enterprise Architect**
Read `.bmad-agents/bmad-enterprise-architect/SKILL.md` AND `references/technology-radar.md`.
Act as the Enterprise Architect.
- Read `docs/prd.md` and `docs/architecture/solution-architecture.md`
- **Validate the Solution Architect's technology choices** against enterprise
  context (compliance, ops maturity, cost, multi-cloud strategy)
- Create `docs/architecture/enterprise-architecture.md`
- Include: cloud infrastructure, CI/CD pipeline, observability stack, security
  architecture, compliance mapping, DR/BCP, cost estimation
- **Select infrastructure-level technologies** using the Technology Radar:
  API gateway, auth provider, data lake/BI, monitoring stack. Document rationale.
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
````

### Customizing the Squad Prompt

Replace the placeholders to use this for any project:

| Placeholder | Example |
|-------------|---------|
| `[PROJECT NAME]` | Enterprise Order Management System |
| `[Description]` | Microservices platform for e-commerce order processing |
| `[EPIC NAME]` | Epic 1: Order Lifecycle Management |
| `[Tech stack preferences]` | "Team knows Kotlin and React, must run on AWS" or "Let architects decide" |
| `[Key constraints]` | PCI-DSS, GDPR data sovereignty, 99.95% SLA, self-hosted auth, multi-cloud |

### Running Multiple Epics

After the first epic is approved, run the squad prompt again with the next epic.
The agents will read the existing artifacts (architecture, design system, test
strategy) and build on them rather than recreating from scratch:

```
[Same squad prompt as above, but change:]
- Epic to implement: Epic 2: Payment Processing & Reconciliation
- Add to instructions: "Read all existing docs/ artifacts from Epic 1.
  Extend the architecture, design system, and test strategy rather than
  replacing them. Add new stories, components, and tests alongside existing ones."
```

---

## Enterprise Focus

All agents are tailored for enterprise systems: microservices, cloud infrastructure (AWS/Azure/GCP), complex integrations, compliance (SOC2/GDPR/HIPAA), observability, and multi-environment deployment.

---

## License

These skills are provided for reuse in your projects. Customize the agent personas, templates, and workflows to fit your team's specific needs.
