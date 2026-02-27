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

### Claude Code

Claude Code uses a `.claude/` directory at the project root for custom instructions and a skills system.

**Option A: As project instructions (simplest)**

Copy the agent skill files into your project and reference them from `CLAUDE.md`:

```bash
# From your project root
cp -r bmad-sdlc-agents/ .bmad-agents/

# Create or append to CLAUDE.md
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

Always read `BMAD-SHARED-CONTEXT.md` first for the overall workflow and artifact structure.
All artifacts go in `docs/` per the BMAD directory convention.
EOF
```

**Option B: As Claude Code skills (auto-triggering)**

If using Cowork or Claude Code's skill system, copy each agent folder into `.skills/skills/`:

```bash
cp -r bmad-sdlc-agents/bmad-business-analyst/ .skills/skills/bmad-business-analyst/
cp -r bmad-sdlc-agents/bmad-product-owner/ .skills/skills/bmad-product-owner/
# ... repeat for each agent
```

Skills auto-trigger based on the `description` field in each SKILL.md frontmatter.

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

### Starting a New Project

Tell your AI coder which agent to invoke. Example prompts:

```
"Act as the BMAD Business Analyst. Read the BA skill and create a
project brief for an order management system."

"Now switch to Product Owner. Read the PO skill, take the project
brief, and create a PRD with prioritized epics and stories."

"Act as the Solution Architect. Design the microservices architecture
based on the PRD."
```

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

Agents can loop back. For example:

- Solution Architect realizes a PRD requirement is ambiguous → asks Product Owner to clarify
- Tech Lead spots hidden complexity in a story → sends back to Solution Architect
- QE finds a gap in test coverage → requests UX/UI Designer to clarify interaction spec

All handoffs are logged in `docs/.bmad/handoff-log.md`.

---

## Enterprise Focus

All agents are tailored for enterprise systems: microservices, cloud infrastructure (AWS/Azure/GCP), complex integrations, compliance (SOC2/GDPR/HIPAA), observability, and multi-environment deployment.

---

## License

These skills are provided for reuse in your projects. Customize the agent personas, templates, and workflows to fit your team's specific needs.
