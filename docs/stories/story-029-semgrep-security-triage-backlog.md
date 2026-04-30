# Story 029 - Semgrep Security Findings Triage Backlog

**Story ID:** story-029  
**Epic:** Security Hardening Backlog  
**Prepared by:** Tech Lead  
**Date:** 2026-04-30  
**Assigned to:** Backend Engineer + Frontend Engineer + DevSecOps Engineer  
**Priority:** P1  
**Estimate:** 5 story points  
**Security:** Yes
**Status:** Remediated

---

## User Story

**As a** security reviewer,  
**I want** each Semgrep finding triaged and either fixed or formally documented,  
**So that** SAST evidence reflects real residual risk instead of unreviewed scanner output.

## Context

Semgrep completed with 17 findings. Dockerfile findings overlap Story 028. Remaining code findings include:

- FastAPI wildcard CORS in `ai-service/app/main.py`
- bcrypt seed-account hash detections in `db/migrations/V021__seed_test_accounts.sql`
- potential ReDoS in `frontend/src/api/client.ts`
- AES/CBC padding-oracle warnings in connector credential encryption
- unencrypted Redis socket warning in `RedisConnector`

## Acceptance Criteria

1. Each Semgrep finding is classified as true positive, false positive, or accepted risk.
2. True positives are fixed where feasible within the story estimate.
3. Accepted risks include rationale and owner.
4. Semgrep is rerun after changes.
5. `docs/security/sast-dast-report.md` is updated with final triage evidence.

## Definition of Done

- [x] Triage table added to security docs
- [x] Code fixes merged for feasible true positives
- [x] Semgrep rerun
- [x] Residual risks documented
