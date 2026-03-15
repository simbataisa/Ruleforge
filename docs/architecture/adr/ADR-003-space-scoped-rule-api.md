# ADR-003: Space-Scoped Rule API Design

**Date:** 2026-03-14
**Status:** ACCEPTED
**Deciders:** Solution Architect, Backend Engineer
**Context:** Space-Scoped Rule Management epic

---

## Context

The management-service exposes two URL shapes for rule operations:
1. `GET /v1/rules` — org-wide flat list (convenience)
2. `GET /v1/spaces/{spaceId}/rules` — space-scoped list

As we add space filtering to the frontend, we need to decide whether to add a `spaceId` query parameter to `GET /v1/rules` or use the space-scoped URL.

## Decision

**Use the existing `GET /v1/spaces/{spaceId}/rules` endpoint for space-scoped rule lists.** Do NOT add a `spaceId` query param to `GET /v1/rules`.

## Rationale

1. **REST resource hierarchy**: `GET /spaces/{id}/rules` is semantically correct — rules are a sub-resource of spaces in the domain model.
2. **Existing implementation**: The endpoint already exists, is tested, and works.
3. **Single responsibility**: `GET /rules` remains the org-wide convenience endpoint; `GET /spaces/{id}/rules` is the space-scoped endpoint. Each has a clear, single purpose.
4. **Cache keys**: React Query can cleanly use `['rules', spaceId]` vs `['rules']` as separate cache keys without ambiguity.
5. **Avoid over-parameterization**: Adding `spaceId` to `GET /rules` would make it do double duty (org-wide OR space-scoped), complicating the contract.

## Consequences

- Frontend must use two different API paths depending on context (space vs. org-wide)
- The `useRules` hook behavior branches on whether a `spaceId` is provided
- No backend changes required for list/create — only move endpoint and copy DTO extension needed
