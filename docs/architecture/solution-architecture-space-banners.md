# Solution Architecture — Space Context Banners & Rules Fix

**Date:** 2026-03-15 | **SA:** Solution Architect (BMAD)

## Overview

Two isolated frontend changes with no backend modifications:
1. **Banner UI** — conditional render in `SpaceDetailPage.tsx` based on `space.name`
2. **Proxy fix** — regex rule in `vite.config.ts` routes `/v1/spaces/{id}/rules*` → management-service

## Root Cause (US-3)

```
Browser → Vite dev proxy
  /v1/spaces/* → 8081 (auth-service)  ← current config
  /v1/spaces/{id}/rules → 8082 (management-service)  ← MISSING
```

Vite proxy uses longest-prefix matching. Since `/v1/spaces` is defined before any more-specific path, ALL `/v1/spaces` traffic goes to auth-service. The auth-service has no `/v1/spaces/{id}/rules` controller → 404/500.

## Fix Architecture

```
Browser → Vite dev proxy
  '^/v1/spaces/[^/]+/rules' → 8082 (management-service)   [NEW — more specific, evaluated FIRST]
  '/v1/spaces' → 8081 (auth-service)                       [EXISTING — catches space CRUD]
```

Vite supports regex keys in proxy config. Placing the regex rule before the string rule ensures specificity.

## Banner Architecture

```typescript
// Decision tree in SpaceDetailPage.tsx
if (space.name === 'Sandbox') {
  render <SandboxBanner />
} else {
  render <ProductionBanner />
}
```

**Design decisions:**
- ADR-005: Detect Sandbox by name (not DB flag) — no migration needed
- Banners are informational, non-dismissible, always visible
- Inline Tailwind styling, no new component file (complexity doesn't warrant extraction)

## API Contract (No Change)

`GET /v1/spaces/{spaceId}/rules` — already defined in management-service `RuleController`. No changes needed.

## Files Changed

| File | Change Type | Reason |
|---|---|---|
| `frontend/vite.config.ts` | Modify | Add regex proxy rule before `/v1/spaces` |
| `frontend/src/features/spaces/pages/SpaceDetailPage.tsx` | Modify | Add space type banners |

