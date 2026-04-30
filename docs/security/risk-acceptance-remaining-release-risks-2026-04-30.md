# Risk Acceptance - Remaining Epic 6 Phase 5 Release Risks

**Date:** 2026-04-30  
**Accepted by:** Tech Lead / Release Owner  
**Scope:** Epic 6 Phase 5 security remediation closeout  
**Status:** Accepted for this release  
**Review date:** 2026-05-15

## Decision

The Tech Lead accepts the remaining release risks for Epic 6 Phase 5 after current-tree vulnerability remediation and scanner verification.

Active scanner findings are clean:

- current-tree gitleaks: 0 findings
- Semgrep: 0 findings
- Checkov Dockerfile checks: 0 failures
- Trivy filesystem scan: 0 vulnerabilities, 0 secrets, 0 misconfigurations
- frontend and Node SDK `npm audit --audit-level=moderate`: 0 vulnerabilities

## Accepted Risks

| Risk ID | Risk | Acceptance Rationale | Compensating Controls | Follow-up |
|---------|------|----------------------|-----------------------|-----------|
| R-SEC-009 | Historical local-development secrets remain in git history. | Current tree is clean and no production use is evidenced in the release review. Rewriting history is deferred to avoid release disruption. | Current-source gitleaks gate is required before release; any externally used value must be rotated. | Story 031 |
| R-SEC-010 | Hardened Dockerfiles were not image-build/runtime-smoke verified in this sandbox. | Scanner policy is satisfied and Docker daemon access was unavailable in this execution environment. Runtime validation can be completed in the deployment environment. | Checkov and Trivy container config scans are clean; release notes must call out Docker build/smoke as a pre-deployment checklist item. | DevSecOps deployment checklist |
| R-SEC-011 | Connector AES-GCM remediation may not decrypt any pre-existing legacy CBC connector credentials. | The scanner issue is fixed for new ciphertext. Production connector credential data is not confirmed in this local review, so migration validation is accepted as an operational release risk. | If legacy connector credentials exist, rotate/re-save connector credentials or run a one-time migration before enabling connector delivery in production. | Connector production readiness checklist |
| R-SEC-012 | Semgrep reports parser warnings for generated Gradle wrapper scripts. | Warnings are limited to generated wrapper scripts, not product source, and Semgrep reports 0 findings. Editing generated wrappers solely for parser cosmetics would add avoidable maintenance risk. | Keep generated wrappers under standard Gradle provenance; rerun Semgrep after future Gradle wrapper refreshes. | Next dependency maintenance window |

## Release Conditions

This acceptance does not waive future hardening work. It allows Epic 6 Phase 5 to proceed with these conditions:

1. Attach this acceptance and `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md` to release notes.
2. Run Docker image build and smoke checks before production deployment.
3. Confirm connector credential migration posture before production connector use.
4. Keep Story 031 open until historical secret exposure disposition is completed.

## Expiry

This acceptance expires on **2026-05-15** or when Epic 6 Phase 5 is promoted to production, whichever comes first.
