# Epic 6 Phase 5 Release Remediation Kickoff

**Date:** 2026-04-30  
**Owner:** Tech Lead  
**Status:** Ready for remediation execution  
**Source gates:** `docs/reviews/release-check-epic-6-phase-5.md`, `docs/testing/epic-6-phase-5-results.md`

---

## Objective

Move Epic 6 Phase 5 from **BLOCKED** to release-ready by closing the formal release blockers. Implementation-level feature tests already pass; this kickoff is limited to release remediation and evidence production.

## Current Release Verdict

**Release status:** BLOCKED

Blocking gates:

1. Full smoke cannot run because the local stack is down.
2. Frontend dependency audit reports 4 high vulnerabilities.
3. DevSecOps security gate artifact is missing.
4. Canonical QA gate and coverage evidence are missing.
5. Release notes are missing.
6. BMAD sentinel signals are missing.

## Scope

### In Scope

| Track | Owner | Required Output |
|-------|-------|-----------------|
| Smoke environment and full smoke | Tech Lead / DevOps | Full `./scripts/smoke-test.sh` evidence with Section 3.22 result |
| Frontend audit remediation | Frontend Engineer + DevSecOps | `npm audit --audit-level=high` passes or formal risk acceptance is documented |
| Security gate | DevSecOps / InfoSec | `docs/security/security-gate-results.md` and, if required, `docs/security/threat-model.md` |
| Canonical QA gate | Tester QE | `docs/testing/qa-gate-results-epic-6-phase-5.md` with coverage and bug backlog status |
| Release notes | Product Owner / Tech Lead | `docs/release-notes-epic-6-phase-5.md` |
| BMAD signals | Tech Lead after verifying artifacts | `.bmad/signals/*-done` files that correspond to actually completed gates |

### Out of Scope

- New webhook product scope.
- DB schema changes.
- Refactoring unrelated frontend dependency chains unless required to clear high vulnerabilities.
- Rewriting already-passing implementation tests.

## Work Items

### R1 - Restore Smoke Environment

**Owner:** Tech Lead / DevOps  
**Priority:** P0

Tasks:

- Start infra and platform services using the project-standard local flow.
- Verify health for PostgreSQL, Redis, Kafka, Elasticsearch, auth-service, management-service, solver-service, event-service, analytics-service, scheduler-service, and connector-service.
- Run `./scripts/smoke-test.sh --quick`.
- Run full `./scripts/smoke-test.sh`.
- Preserve the full smoke result in the QA gate artifact.

Done when:

- Full smoke exits 0, or any failure has a specific defect report/fix owner.
- Section 3.22 result is explicitly recorded.

### R2 - Clear Frontend High Vulnerabilities

**Owner:** Frontend Engineer + DevSecOps  
**Priority:** P0  
**Security-sensitive:** yes

Current audit findings include high vulnerabilities in `flatted`, `minimatch`, `picomatch`, and `rollup`.

Tasks:

- Run `npm audit --audit-level=high` from `frontend/`.
- Apply the smallest safe dependency updates that clear high findings.
- Avoid breaking Vite/Vitest unless a major upgrade is intentionally approved.
- Run `npm test`.
- Run `npm run build`.
- Re-run `npm audit --audit-level=high`.

Done when:

- Audit exits 0 for high severity, or DevSecOps records a formal risk acceptance.
- Frontend tests and build still pass.

### R3 - Produce Security Gate

**Owner:** DevSecOps / InfoSec  
**Priority:** P0

Tasks:

- Review Epic 6 auth-sensitive changes: OAuth2 token URL validation, SSRF guard behavior, credential masking, secret persistence, token cache behavior, VIEWER restrictions, and AUTH_FAILURE handling.
- Include dependency audit status from R2.
- Save security decision to `docs/security/security-gate-results.md`.
- Save threat model or scoped threat-model addendum to `docs/security/threat-model.md` if no canonical threat model exists.

Done when:

- Security gate is PASS or explicit risk-accepted FAIL with named approver.
- `devsecops-done` is eligible to be written by Tech Lead after review.

### R4 - Produce Canonical QA Gate

**Owner:** Tester QE  
**Priority:** P0

Tasks:

- Convert current feature results into canonical `docs/testing/qa-gate-results-epic-6-phase-5.md`.
- Include full smoke evidence from R1.
- Include coverage evidence or explicitly document unavailable coverage and TL risk decision.
- Confirm no critical/high bugs remain open.

Done when:

- QA gate artifact exists and gives a clear PASS/FAIL.
- If FAIL, each failing item has a defect owner and next action.

### R5 - Draft Release Notes

**Owner:** Product Owner / Tech Lead  
**Priority:** P1

Tasks:

- Draft `docs/release-notes-epic-6-phase-5.md`.
- Include user-visible changes: Custom Header auth, OAuth2 client credentials, AUTH_FAILURE status, settings UI credential rotation, and smoke coverage.
- Include operator notes: OAuth2 token URL must use HTTPS; auth failures are terminal and require credential repair.

Done when:

- Release notes are ready for stakeholder review.

## Sequencing

1. R2 and R3 can start immediately.
2. R1 can start immediately if local services can be launched.
3. R4 waits for R1 and R2 evidence.
4. R5 can run in parallel.
5. Tech Lead reruns `tech-lead-release-check` after R1-R5 are complete.

## Required Final Verification

Before release approval:

- `./gradlew :ruleforge-event-service:test`
- `npm test`
- `npm run build`
- `npm audit --audit-level=high`
- `./scripts/smoke-test.sh`

## Signal Policy

Do not create completion signals until the corresponding evidence exists.

Eligible signals after remediation:

- `E2-be-done` only after backend implementation and smoke-relevant backend behavior are TL-reviewed.
- `E2-fe-done` only after dependency remediation, tests, build, and UI scope are TL-reviewed.
- `devsecops-done` only after security gate artifact is present and reviewed.
- `tl-plan-done` may be written for this remediation kickoff.

## Rollback / Stop Criteria

Stop and escalate to Tech Lead if:

- Dependency remediation requires a major Vite/Vitest migration.
- Full smoke reveals a product behavior regression in Stories 022-027.
- Security gate identifies unmitigated secret leakage, SSRF bypass, auth bypass, or privilege escalation.
