# Story 030 - Moderate Dependency Advisory Backlog

**Story ID:** story-030  
**Epic:** Security Hardening Backlog  
**Prepared by:** Tech Lead  
**Date:** 2026-04-30  
**Assigned to:** Frontend Engineer + SDK Owner  
**Priority:** P1  
**Estimate:** 5 story points  
**Security:** Yes
**Status:** Remediated

---

## User Story

**As a** release owner,  
**I want** remaining moderate dependency advisories remediated or risk-accepted with evidence,  
**So that** dependency risk is tracked beyond the high-severity audit gate.

## Context

Current scanner evidence shows:

- `frontend/`: 7 moderate npm audit findings, plus 8 Trivy DOMPurify medium vulnerabilities in `frontend/package-lock.json`
- `sdks/sdk-node/`: 4 moderate npm audit findings
- `sdks/sdk-java/pom.xml`: Trivy reports `com.fasterxml.jackson.core:jackson-core` 2.17.0 affected by `GHSA-72hv-8253-57qq`

## Acceptance Criteria

1. Frontend Vite/Vitest/Monaco/DOMPurify upgrade path is assessed.
2. SDK Node Vite/Vitest upgrade path is assessed.
3. SDK Java Jackson dependency is upgraded to a fixed compatible version or formally risk-accepted.
4. `npm audit --audit-level=moderate` is rerun in affected Node workspaces, or residual moderate findings are documented.
5. Trivy fs is rerun and dependency evidence is updated.

## Definition of Done

- [x] Dependency updates or risk acceptances completed
- [x] Frontend tests/build pass if dependencies change
- [x] SDK tests/build pass if dependencies change
- [x] Trivy fs evidence refreshed
- [x] Security report updated
