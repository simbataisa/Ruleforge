# Story 011 — Fix /v1/spaces/{id}/rules 500 Error

**Epic:** Space Context Awareness | **Priority:** P0 | **Points:** 1

## Description
Add a more-specific Vite proxy rule so `GET /v1/spaces/{id}/rules` routes to management-service (8082) instead of auth-service (8081).

## Root Cause
Vite proxy: `/v1/spaces` → 8081 (auth-service) catches ALL `/v1/spaces/*` requests including `/rules`.
Management-service (8082) hosts the actual rules endpoint.

## Fix
Add to `vite.config.ts` before the `/v1/spaces` rule:
```typescript
'^/v1/spaces/[^/]+/rules': { target: 'http://localhost:8082', changeOrigin: true },
```

## Acceptance Criteria
- `GET /v1/spaces/{any-id}/rules` returns 200 (not 500)
- `GET /v1/organizations/current/spaces` still returns 200 (not broken by fix)
- SpaceDetailPage shows rules (or empty state) without error message

## Files
- `frontend/vite.config.ts`

