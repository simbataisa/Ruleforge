# Team Conventions: RuleForge

## Changelog

- 2026-10-25: Replace scaffold defaults with observed conventions and explicitly identify unconfirmed policies (--force).

> Repository observations are evidence of implementation, not approval of a new team policy.

## Version Control

- Default branch: origin/main (git symbolic-ref).
- Conventional Commits: 49 of the last 50 subjects (98%) match the conventional prefix pattern.
- Branch naming and branching strategy: under review; no supported prefix found in the last 50 commit decorations.
- PR approvals and review requirements: under review; no PR template or CONTRIBUTING.md found.
- Worktree location for BMAD role work: .bmad/worktrees/<role>, per the supplied AGENTS.md instructions.
- No merge, commit, or deployment is authorized by this onboarding snapshot.

## Code Style

| Area | Observed convention | Source |
|---|---|---|
| Java | Java 25, package root io.ruleforge; Gradle Kotlin DSL | engine/gradle.properties; platform/gradle.properties; source tree |
| React | PascalCase.tsx component files; *.api.ts API modules | frontend/src |
| TypeScript | Strict checking, unused local and parameter checks | frontend/tsconfig.json |
| Python | Python >=3.11; FastAPI app | ai-service/pyproject.toml |
| Lint | ESLint dependency and npm run lint; no matching config found | frontend/package.json; config inventory |
| Format | No formatter configuration detected | Repository inventory |

File/function length limits and automatic formatting policy remain under review.

## API Conventions

- REST endpoints use /v1/... (README and frontend API modules).
- Java ErrorResponse fields: status, error, message, path, timestamp (platform/ruleforge-common/src/main/java/io/ruleforge/common/dto/ErrorResponse.java).
- Java PageResponse fields: content, page, size, totalElements, totalPages (adjacent PageResponse.java).
- JWT and API-key authentication are documented in README and docs/auth-guide.md.
- TenantContext uses ScopedValue; work on other threads must explicitly re-bind tenant information using runAs/callAs (TenantContext.java).

## Testing

| Component | Declared framework / command | Source |
|---|---|---|
| Engine | JUnit 5, Mockito, AssertJ; Gradle test | engine/build.gradle.kts |
| Platform | JUnit Platform; module Gradle tests | platform/build.gradle.kts |
| Frontend | Vitest, Testing Library; npm test | frontend/package.json |
| AI | pytest | ai-service/pyproject.toml |
| Live stack | scripts/smoke-test.sh | README.md |

Commands were inspected, not executed. Coverage target and enforcement remain under review.

## Database

- Versioned migrations use V{N}__{description}.sql under db/migrations/.
- README documents make migrate for migrations and PostgreSQL RLS for tenant isolation.
- Operational database state and RLS coverage were not assessed.

## Documentation and Process

- Existing brief, PRD, solution/enterprise architecture, ADR index, and UX design are indexed in PROJECT-CONTEXT.md.
- Artifact changes include a changelog and a numbered handoff per supplied AGENTS.md instructions.
- Tech Lead independent review and merge-token requirements follow the supplied BMAD instructions.
- Sprint length, story sizing, reviewer counts, and production Definition of Done: under review; onboarding does not infer them.
- No CI workflow, PR template, CONTRIBUTING.md, or CODE_OF_CONDUCT.md found in the scanned repository.

## Local Development

- scripts/start-local.sh orchestrates local startup; scripts/stop-local.sh stops local services.
- Docker Compose config is under infra/docker/.
- Vite proxies local /v1 traffic directly to services; Kong is configured for Docker.
- README lists known issues affecting Kafka publication, seed data, and MinIO image pulls.

## Gate Configuration

Gate detection failed: the installed bmad-gate.py rejects coverage.report and coverage.threshold in the scaffold gates.json. The detector also reports no root candidates for tests-green, build-green, and typecheck-clean in this multi-component repository. Tech Lead must resolve the schema mismatch and declare reviewed component commands before autonomous execution.
