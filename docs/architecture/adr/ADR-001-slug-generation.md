# ADR-001: Slug Generation Strategy for Spaces

## Status
Accepted

## Context

The frontend requires human-readable URL slugs for spaces (e.g., `/spaces/production-rules` instead of `/spaces/3fa85f64-...`). The `OrgSpace` TypeScript interface in `org.api.ts` already declares a `slug` field, but the backend's `SpaceDto` does not return one and the `spaces` table has no `slug` column.

Two strategies were considered:

1. **Compute on-the-fly** — Derive slug from `name` at read time, never store it
2. **Store in DB** — Generate slug at write time, persist in `spaces.slug` with unique constraint

## Decision

**Store slug in DB.** Generate at write time (create and update), persist in a `slug VARCHAR(255) NOT NULL` column, enforce uniqueness with `UNIQUE(org_id, slug)` constraint.

## Rationale

### Why not compute on-the-fly?

- **URL instability**: If a space is renamed, the slug changes, breaking bookmarks and external references
- **No uniqueness enforcement**: Two spaces "Sales & Marketing" and "Sales Marketing" both compute to `sales-marketing` — the DB cannot detect this collision at runtime
- **Complex deduplication**: Without stored slugs, de-duplication logic must query all space names on every request to check for conflicts, which is expensive and race-condition-prone

### Why store in DB?

- **Stable URLs**: Slug is set at creation; renaming the space name does NOT change the URL slug (just like GitHub repos — renaming keeps the old slug or allows explicit redirect)
- **Uniqueness via constraint**: `UNIQUE(org_id, slug)` is enforced by the DB. Collision is deterministic — `DataIntegrityViolationException` maps to 409 Conflict
- **Simple implementation**: `SlugUtils.toSlug(name)` is a 5-line utility. Persisting once at write time is straightforward

## Implementation

```java
// SlugUtils.toSlug("My First Space!") → "my-first-space"
public static String toSlug(String name) {
    return name.toLowerCase()
               .replaceAll("[^a-z0-9]+", "-")
               .replaceAll("^-|-$", "");
}
```

Migration `V047` adds `slug VARCHAR(255) NOT NULL` with `UNIQUE(org_id, slug)` and backfills existing spaces.

On `createSpace()` and `updateSpace()`, slug is regenerated from the new name. If a rename causes a collision, the API returns 409.

## Consequences

**Positive:**
- Stable, bookmarkable URLs
- Database-enforced uniqueness (no race conditions)
- Simple implementation

**Negative:**
- DB migration required (V047)
- Rename triggers slug change, which could break existing bookmarks — acceptable trade-off since spaces are organizational units, not frequently renamed
- Slug computed only from name, not custom-settable by user (acceptable for v1)

## Alternatives Considered

1. **UUID in URL** — Keep `/spaces/:uuid` URLs. Rejected: ugly, non-shareable, doesn't match existing `OrgSpace.slug` type in frontend.
2. **Custom user-settable slug** — Allow admins to set their own slug. Rejected: out of scope for v1; adds form complexity.
3. **Compute on-the-fly** — Rejected: unstable URLs, no uniqueness enforcement.

## Review Date
2026-Q3 (when space rename frequency is known from usage data)
