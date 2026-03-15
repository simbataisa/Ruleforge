# ADR-005: Detect Sandbox Space by Name

**Status:** Accepted | **Date:** 2026-03-15

## Context
Need to distinguish Sandbox from production spaces to show different banners.

## Decision
Detect Sandbox by `space.name === "Sandbox"` (case-sensitive string comparison).

## Rationale
- No DB migration required (no `space_type` column needed)
- The Sandbox space is seeded with name "Sandbox" (V021 migration)
- Simplest implementation, no API change
- Org naming conventions enforce a single "Sandbox" space

## Consequences
- Positive: Zero backend changes, instant ship
- Negative: Name-based detection is fragile if admins rename the space
- Accepted: Org convention is "Sandbox" = sandbox; renaming is an edge case

## Alternatives Rejected
- **DB space_type column**: Requires migration V048, service restart, DTO change — over-engineered for this feature
- **Slug-based detection** (`space.slug === "sandbox"`): Equivalent fragility, same decision

