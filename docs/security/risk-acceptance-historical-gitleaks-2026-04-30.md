# Risk Acceptance - Historical Gitleaks Findings

**Date:** 2026-04-30  
**Accepted by:** Tech Lead on behalf of release review  
**Scope:** Epic 6 Phase 5 release review  
**Related report:** `docs/security/sast-dast-report.md`  
**Status:** Accepted for this release only

## Decision

The 9 redacted historical gitleaks findings in service `application.yml` git history are accepted for this release.

## Rationale

- Current working tree gitleaks scan passes with no leaks.
- Source configuration now uses environment-backed local-development marker defaults instead of committed base64-like secrets.
- The historical values are treated as local-development defaults and are not approved for production use.
- Rewriting git history this late in the release would create integration risk across the active BMAD worktree and handoff flow.

## Conditions

This acceptance does not waive future secret hygiene work. Before production deployment or external distribution:

1. Confirm no historically committed value is active in any deployed environment.
2. Rotate any value that may have been copied into non-local environments.
3. Ensure production uses secret-manager or environment-provided values only.
4. Revisit git history rewrite if this repository is published outside the trusted project boundary.

## Evidence

- Current-tree gitleaks: PASS, no leaks.
- Historical gitleaks: 9 redacted findings remain.
- Current-source remediation tracked in `docs/security/sast-dast-report.md`.
