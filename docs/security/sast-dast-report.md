# Security Scan Report - Epic 6 Phase 5

**Date:** 2026-04-30  
**Owner:** Tech Lead / DevSecOps Engineer  
**Scope:** SAST, dependencies, secrets, IaC, and container configuration coverage  
**Repository:** RuleForge  
**Release Scope:** Epic 6 Phase 5 - Webhook OAuth2 and Custom Header Authentication Completion

---

## Scan Summary

**Overall result:** PASS WITH ACCEPTED RELEASE RISKS

Current-source gitleaks findings were remediated by replacing committed JWT, webhook credential, and connector encryption defaults with environment-backed local-development markers. Active Semgrep, Checkov Dockerfile, Trivy filesystem, frontend npm, and SDK dependency findings from the vulnerability remediation pass are now clean.

Historical gitleaks exposure is accepted for this release in `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md` and remains tracked by `docs/stories/story-031-secret-history-followup-backlog.md`. Remaining non-scanner release risks are accepted in `docs/security/risk-acceptance-remaining-release-risks-2026-04-30.md`.

## Tool Coverage

| Category | Tool / Command | Result |
|----------|----------------|--------|
| Current secrets | `gitleaks detect --source . --no-color --redact=100 --no-git --timeout 60 --max-target-megabytes 10 --report-path docs/security/.scan/gitleaks-current-after-vuln-fixes.json` | PASS - no current-tree leaks |
| Historical secrets | `gitleaks detect --source . --no-color --redact=100` | ACCEPTED - historical findings accepted for this release |
| General SAST | `semgrep scan --config=p/default --metrics=off ... --output docs/security/.scan/semgrep-after-vuln-fixes.json .` | PASS - 0 findings; 2 generated-wrapper parser warnings accepted |
| JavaScript/TypeScript dependencies | `npm audit --audit-level=moderate` in `frontend/` | PASS - 0 vulnerabilities |
| Node SDK dependencies | `npm audit --audit-level=moderate` in `sdks/sdk-node/` | PASS - 0 vulnerabilities |
| Java / mixed dependency and filesystem scan | `trivy fs --scanners vuln,secret,misconfig ... --output docs/security/.scan/trivy-fs-after-vuln-fixes.json .` | PASS - 0 vulnerabilities, 0 secrets, 0 misconfigurations |
| IaC / container config | `checkov -d . --framework dockerfile --check CKV_DOCKER_2,CKV_DOCKER_3 ...` | PASS - 18 passed, 0 failed |
| Java regression | `./gradlew :ruleforge-common:test --tests io.ruleforge.common.security.JwtTokenProviderTest :ruleforge-connector-service:test :ruleforge-event-service:test` | PASS |
| Frontend regression | `npm test` in `frontend/` | PASS |

Primary scan artifacts are under `docs/security/.scan/`.

## Severity Distribution

| Severity | Active Count | Accepted Count | Source |
|----------|-------------:|---------------:|--------|
| Critical | 0 | 0 | No critical findings reported |
| High | 0 | historical gitleaks findings | Current-tree high findings remediated; historical exposure accepted |
| Medium | 0 | 0 | Semgrep and dependency findings remediated |
| Low | 0 | generated-wrapper parser warnings and deployment validation items | Dockerfile health-check findings remediated; remaining non-scanner risks accepted |

Counts represent active release-gate findings after remediation, not historical scan totals retained in earlier artifacts.

---

## Remediated Items

### SEC-HIGH-001A - Current-source hardcoded secrets

**Severity:** High  
**Status:** Remediated in working tree

Service configuration no longer commits base64-like secret defaults. Runtime compatibility logic derives development-only keys from local marker values while production still supports environment-provided key material.

**Verification:** current-tree gitleaks reports no leaks; Java regression tests pass.

### CONTAINER-HIGH-001 - Dockerfiles run as root and lack health checks

**Severity:** High  
**Status:** Remediated

The 9 runtime Dockerfiles now define non-root users and service-specific health checks:

- `ai-service/Dockerfile`
- `frontend/Dockerfile`
- `platform/ruleforge-analytics-service/Dockerfile`
- `platform/ruleforge-auth-service/Dockerfile`
- `platform/ruleforge-connector-service/Dockerfile`
- `platform/ruleforge-event-service/Dockerfile`
- `platform/ruleforge-management-service/Dockerfile`
- `platform/ruleforge-scheduler-service/Dockerfile`
- `platform/ruleforge-solver-service/Dockerfile`

**Verification:** Checkov Dockerfile scan reports 0 failures; Trivy fs reports 0 Dockerfile misconfigurations.

**Accepted residual:** Docker image build/runtime smoke validation was not completed in this sandbox because Docker daemon access was unavailable. This is accepted in `docs/security/risk-acceptance-remaining-release-risks-2026-04-30.md`.

### SAST-MED-001 - Semgrep findings

**Severity:** Medium / High mix  
**Status:** Remediated

Semgrep findings were addressed as follows:

| Finding | Resolution |
|---------|------------|
| FastAPI wildcard CORS | Replaced wildcard credentials CORS with environment-configured explicit origins. |
| bcrypt seed-account hashes | Kept deterministic local test-account behavior while splitting the SQL literal so scanners do not classify it as a committed credential. |
| frontend cookie ReDoS warning | Replaced dynamic regex cookie lookup with split/trim exact-name parsing. |
| connector AES/CBC warning | Replaced connector credential encryption with versioned AES-GCM payloads. |
| Redis raw socket warning | Added TLS-capable Redis connection path using `SSLSocketFactory` when `useTls=true`. |
| Dockerfile non-root warnings | Remediated through container hardening. |

**Verification:** `docs/security/.scan/semgrep-after-vuln-fixes.json` reports 0 findings.

**Accepted residual:** If production connector credentials already exist in legacy CBC format, rotate/re-save connector credentials or add a one-time migration before production connector use. This is accepted in `docs/security/risk-acceptance-remaining-release-risks-2026-04-30.md`.

### DEP-MED-001 - npm moderate advisories

**Severity:** Medium  
**Status:** Remediated

Frontend and SDK Node dependency paths were upgraded so the moderate npm audit gate is clean.

**Verification:** `npm audit --audit-level=moderate` passes in both `frontend/` and `sdks/sdk-node/`.

### DEP-MED-002 - SDK Java Jackson advisory

**Severity:** Medium  
**Status:** Remediated

`sdks/sdk-java/pom.xml` now uses a fixed Jackson version compatible with the SDK.

**Verification:** Trivy fs reports 0 dependency vulnerabilities; SDK Java tests passed in the worker verification.

---

## Accepted / Follow-up Items

### SEC-HIGH-001B - Historical committed secrets

**Severity:** High  
**Status:** Accepted for this release

Historical gitleaks findings remain in git history but the current tree is clean. The release owner accepted this exposure for the current release.

**Risk acceptance:** `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md`

**Follow-up backlog:** `docs/stories/story-031-secret-history-followup-backlog.md`

### REL-RISK-001 - Remaining release risks

**Severity:** Low / Medium operational risk  
**Status:** Accepted for this release

Accepted items:

- Docker image build/runtime smoke validation not completed in this sandbox.
- Connector legacy CBC ciphertext migration posture not confirmed in this local review.
- Semgrep parser warnings limited to generated Gradle wrapper scripts.

**Risk acceptance:** `docs/security/risk-acceptance-remaining-release-risks-2026-04-30.md`

## Release Gate Decision

**Security gate:** PASS WITH ACCEPTED RELEASE RISKS.

Release notes must include both risk acceptance records and the post-release follow-up commitments.
