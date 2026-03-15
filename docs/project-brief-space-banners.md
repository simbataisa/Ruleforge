# Project Brief — Space Context Banners & Rules 500 Fix

**Date:** 2026-03-15
**Analyst:** Business Analyst (BMAD)
**Epic:** Space Context Awareness & Stability

---

## Executive Summary

The SpaceDetailPage lacks contextual messaging to orient users about the nature of the space they are working in. Users cannot distinguish Sandbox (experimental) from production spaces at a glance, leading to accidental rule creation in the wrong context. Additionally, the `/v1/spaces/{spaceId}/rules` endpoint returns HTTP 500 for all spaces due to a misconfigured Vite dev proxy, making the rules tab completely non-functional.

## Problem Statement

1. **No contextual banner**: SpaceDetailPage displays rules without any indication of whether the space is a Sandbox or production environment, creating risk of misrouted rule creation.
2. **Rules endpoint 500 error**: All `GET /v1/spaces/{spaceId}/rules` requests return 500 because Vite proxies `/v1/spaces` to the auth-service (port 8081), which has no rules endpoint — the rules endpoint lives in management-service (port 8082).

## Stakeholder Analysis

| Stakeholder | Need | Concern |
|---|---|---|
| Rule Authors | Know which space they're editing | Accidentally creating prod rules in Sandbox |
| Space Admins | Clear environment labeling | No visual differentiation |
| Org Owners | Rule governance | Rules created in wrong space |
| Dev Team | Functioning rules tab | 500 blocks all space usage |

## Scope

### In-Scope
- FR-001: Sandbox space banner ("This is the Sandbox space. Any rules created here will be visible to all spaces.")
- FR-002: Production space banner ("This is a production space. Any rules created here will be visible to all spaces.")
- FR-003: Fix 500 Internal Server Error on `/v1/spaces/{spaceId}/rules` endpoint

### Out-of-Scope
- Backend space type field (detection by name "Sandbox")
- Space type management UI
- Persistent banner dismiss

## Root Cause Analysis — 500 Error

**Cause:** `vite.config.ts` routes `/v1/spaces/*` → auth-service (8081). The rules subroute `/v1/spaces/{id}/rules` lives in management-service (8082). Vite's proxy matches `/v1/spaces` first (prefix match), so the rules request never reaches management-service.

**Fix:** Add a regex-based Vite proxy rule that matches `/v1/spaces/{uuid}/rules` before the generic `/v1/spaces` rule, targeting management-service (8082).

## Requirements

### Functional
- FR-001: When space.name === "Sandbox", show: "This is the Sandbox space. Any rules created here will be visible to all spaces."
- FR-002: When space.name !== "Sandbox", show: "This is a production space. Any rules created here will be visible to all spaces."
- FR-003: `/v1/spaces/{id}/rules` must proxy to management-service (8082)

### Non-Functional
- NFR-001: Banner renders within existing page load time (<200ms marginal cost)
- NFR-002: Proxy fix has zero backend code changes
- NFR-003: Banner is accessible (role="alert" or role="status", WCAG 2.1 AA)

## Feasibility

- **Technical:** GREEN — Pure frontend changes only (1 file for banner, 1 file for proxy fix)
- **Timeline:** GREEN — <1 sprint (2 files, ~50 lines)
- **Risk:** LOW — No backend changes, no DB migration, no service restart needed

## KPIs

- 500 error rate on `/v1/spaces/{id}/rules` → 0%
- Space context banner visible on 100% of SpaceDetailPage loads
- TypeScript build: 0 errors
- Smoke test: 0 FAIL

