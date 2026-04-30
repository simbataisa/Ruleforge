# Story 031 - Historical Secret Exposure Follow-up Backlog

**Story ID:** story-031  
**Epic:** Security Hardening Backlog  
**Prepared by:** Tech Lead  
**Date:** 2026-04-30  
**Assigned to:** DevSecOps Engineer  
**Priority:** P2  
**Estimate:** 3 story points  
**Security:** Yes

---

## User Story

**As a** security owner,  
**I want** the accepted historical gitleaks exposure revisited after release,  
**So that** accepted release risk does not become unmanaged long-term security debt.

## Context

Historical gitleaks reports 9 redacted findings in service `application.yml` history. The current tree is clean and the historical exposure is accepted for Epic 6 Phase 5 release review in `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md`.

## Acceptance Criteria

1. Confirm whether any historical value was ever used outside local development.
2. Rotate any value that may have been used outside local development.
3. Decide whether git history rewrite is necessary before external repository distribution.
4. Document the final decision and evidence in `docs/security/`.
5. Keep current-tree gitleaks at 0 findings.

## Definition of Done

- [ ] Environment usage reviewed
- [ ] Rotation completed if needed
- [ ] History rewrite decision documented
- [ ] Current-tree gitleaks remains clean
