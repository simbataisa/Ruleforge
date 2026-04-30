# Story 028 - Container Runtime Hardening Backlog

**Story ID:** story-028  
**Epic:** Security Hardening Backlog  
**Prepared by:** Tech Lead  
**Date:** 2026-04-30  
**Assigned to:** DevSecOps Engineer + Backend Engineer + Frontend Engineer  
**Priority:** P0  
**Estimate:** 5 story points  
**Security:** Yes
**Status:** Remediated - deployment build validation pending

---

## User Story

**As a** platform operator,  
**I want** all RuleForge runtime Docker images to run as non-root users and expose health checks,  
**So that** production containers meet baseline hardening policy and scanner gates pass.

## Context

Checkov and Trivy both report Dockerfile hardening findings across 9 runtime images:

- `ai-service/Dockerfile`
- `frontend/Dockerfile`
- `platform/ruleforge-analytics-service/Dockerfile`
- `platform/ruleforge-auth-service/Dockerfile`
- `platform/ruleforge-connector-service/Dockerfile`
- `platform/ruleforge-event-service/Dockerfile`
- `platform/ruleforge-management-service/Dockerfile`
- `platform/ruleforge-scheduler-service/Dockerfile`
- `platform/ruleforge-solver-service/Dockerfile`

Findings:

- Missing non-root runtime `USER`
- Missing `HEALTHCHECK`

## Acceptance Criteria

1. Every runtime Dockerfile creates or uses a non-root user.
2. Every runtime Dockerfile sets `USER` to the non-root runtime user.
3. Every runtime Dockerfile defines a health check appropriate for its service.
4. Container builds still succeed for frontend, AI service, and all platform services.
5. Checkov no longer reports `CKV_DOCKER_2` or `CKV_DOCKER_3` for the listed files.
6. Trivy fs no longer reports `DS-0002` or `DS-0026` for the listed files.

## Definition of Done

- [x] Dockerfiles updated
- [ ] Container builds verified in a Docker-enabled environment
- [x] Checkov rerun and evidence saved
- [x] Trivy fs rerun and evidence saved
- [x] Security report updated
