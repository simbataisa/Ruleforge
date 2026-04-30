# Tech Lead Review - DevSecOps Security Scan Remediation

**Date:** 2026-04-30  
**Reviewer:** Tech Lead  
**Scope:** Handoff 018 DevSecOps remediation: current-source gitleaks cleanup, scanner evidence, security gate docs  
**Verdict:** FAIL - rework required

## Findings

### TL-SEC-001 - JWT dev default breaks every service without `RULEFORGE_JWT_SECRET`

**Severity:** High  
**Status:** Rework required  
**Files:**

- `platform/ruleforge-auth-service/src/main/resources/application.yml`
- `platform/ruleforge-event-service/src/main/resources/application.yml`
- `platform/ruleforge-management-service/src/main/resources/application.yml`
- `platform/ruleforge-solver-service/src/main/resources/application.yml`
- `platform/ruleforge-connector-service/src/main/resources/application.yml`
- `platform/ruleforge-analytics-service/src/main/resources/application.yml`
- `platform/ruleforge-scheduler-service/src/main/resources/application.yml`
- `platform/ruleforge-common/src/main/java/io/ruleforge/common/security/JwtTokenProvider.java`

The replacement JWT default is a plain `local-dev-*` string, but `JwtTokenProvider` unconditionally calls `Base64.getDecoder().decode(base64Secret)`. Any local service that starts without `RULEFORGE_JWT_SECRET` will fail during bean construction.

**Required fix:** Either keep JWT defaults base64-encoded but allowlist known local placeholders in gitleaks, or add an explicit local-dev decode path to `JwtTokenProvider` with tests across auth plus at least one resource service.

### TL-SEC-002 - Security reports incorrectly mark Trivy fs as passing

**Severity:** High  
**Status:** Rework required  
**Files:**

- `docs/security/sast-dast-report.md`
- `docs/security/security-gate-results.md`
- `docs/security/.scan/trivy-fs.json`

The report states Trivy fs passed with no reported findings, but `docs/security/.scan/trivy-fs.json` contains findings:

- 8 medium vulnerabilities in `frontend/package-lock.json`
- 1 medium vulnerability in `sdks/sdk-java/pom.xml`
- 9 Dockerfile targets with 2 misconfigurations each

The earlier summary appears to have queried lowercase JSON fields instead of Trivy's uppercase `Results`, `Vulnerabilities`, and `Misconfigurations` fields.

**Required fix:** Update both security docs to report Trivy fs as FAIL or PARTIAL with the actual vulnerability and misconfiguration counts, and include the SDK Java Jackson finding in the remediation plan.

## Non-Blocking Notes

- Current-source `gitleaks --no-git` passes after remediation.
- Historical gitleaks remains correctly reported as failing with 9 redacted findings.
- `bandit -r ai-service/app` and `pip-audit -r ai-service/requirements.txt` are valid PASS evidence.
- Event-service credential key local-dev derivation is acceptable for local development, but the error message still says all keys must be base64-encoded. Consider clarifying the message.

## Verification Reviewed

- `gitleaks --no-git`: PASS, no current-tree leaks.
- `gitleaks` history: FAIL, 9 redacted historical findings.
- `semgrep`: completed, 17 findings.
- `checkov`: completed, 18 Dockerfile findings.
- `trivy fs`: completed, contains vulnerability and misconfiguration findings.
- `./gradlew :ruleforge-event-service:test :ruleforge-connector-service:test`: previously PASS.

## Decision

Security remediation is not approved for release. Rework the JWT default handling and correct Trivy reporting before another TL review.
