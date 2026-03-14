# Story 001 — Space DTO Enrichment + DB Migration

**Epic:** Spaces Page Features
**Phase:** Backend
**Assignee:** Backend Engineer
**Priority:** P0 — Must complete before all other stories

---

## User Story

As a frontend client consuming the Spaces API,
I want SpaceDto to include `slug`, `description`, `ruleCount`, and `updatedAt`,
So that the UI can display enriched space cards without additional API calls.

---

## Background

`SpaceDto` currently returns only `id`, `name`, `orgId`, `createdAt`. The `OrgSpace` TypeScript type in `org.api.ts` declares `slug` and `ruleCount` as required fields — the frontend is already referencing these but they return `undefined` from the API. The DB `spaces` table has a `description` column (from V003) that is not mapped. There is no `slug` column.

---

## Acceptance Criteria

1. **DB migration V047** — `spaces.slug VARCHAR(255) NOT NULL` column added; backfilled from name; `UNIQUE(org_id, slug)` constraint added
2. **`SpaceEntity`** — Maps `slug`, `description`, `updatedAt` fields
3. **`SpaceDto`** — Record includes `slug: String`, `description: String` (nullable), `ruleCount: long`, `updatedAt: Instant`
4. **`SpaceService.toDto()`** — Enriches all `SpaceDto` instances with `ruleCount` via bulk query (see ADR-002)
5. **`SpaceService.createSpace()`** — Generates and saves slug using `SlugUtils.toSlug(name)`
6. All existing `GET /v1/organizations/current/spaces` responses include the new fields
7. Slug uniqueness collision throws 409 Conflict (see story-002 for DELETE/PUT endpoints)

---

## Technical Implementation

### New File: `util/SlugUtils.java`
```java
package io.ruleforge.auth.util;

public final class SlugUtils {
    private SlugUtils() {}

    public static String toSlug(String name) {
        if (name == null || name.isBlank()) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9]+", "-")
                   .replaceAll("^-|-$", "");
    }
}
```

### Modified: `entity/SpaceEntity.java`
Add fields:
```java
@Column(nullable = false)
private String slug;

@Column(columnDefinition = "text")
private String description;
```
(Note: `description` column already exists in DB per V003, just add mapping.)

### Modified: `dto/SpaceDto.java`
```java
public record SpaceDto(
    UUID id,
    String name,
    String slug,
    String description,
    long ruleCount,
    UUID orgId,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Modified: `repository/SpaceRepository.java`
Add bulk count query:
```java
@Query(value = """
    SELECT r.space_id as spaceId, COUNT(*) as ruleCount
    FROM rules r
    WHERE r.space_id IN :spaceIds
    GROUP BY r.space_id
    """, nativeQuery = true)
List<Object[]> countRulesBySpaceIds(@Param("spaceIds") List<UUID> spaceIds);

@Query(value = "SELECT COUNT(*) FROM rules WHERE space_id = :spaceId", nativeQuery = true)
long countRulesBySpaceId(@Param("spaceId") UUID spaceId);
```

### Modified: `service/SpaceService.java`
- `createSpace()`: set `space.setSlug(SlugUtils.toSlug(request.name()))`
- Update `toDto(entity, ruleCount)` signature to accept count
- `listSpaces()`: Fetch all spaces, call bulk count query, map to DTO
- Catch `DataIntegrityViolationException` with "spaces_slug_org_unique" message → throw `ConflictException`

### New File: `db/migrations/V047__spaces_slug_and_enhancements.sql`
```sql
ALTER TABLE spaces ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

UPDATE spaces
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL;

ALTER TABLE spaces ALTER COLUMN slug SET NOT NULL;

ALTER TABLE spaces
  ADD CONSTRAINT spaces_slug_org_unique UNIQUE (org_id, slug);
```

---

## Definition of Done

- [ ] V047 migration file created and applies cleanly on existing DB
- [ ] `SpaceEntity` maps `slug`, `description`
- [ ] `SpaceDto` record has all 8 fields
- [ ] `SlugUtils.toSlug()` utility class created with unit tests
- [ ] `GET /v1/organizations/current/spaces` returns enriched DTOs (verified with curl)
- [ ] Creating two spaces with same name → 409 response
- [ ] No N+1 queries: list endpoint uses bulk count query (verify with EXPLAIN or logs)
- [ ] Existing smoke tests still pass

---

## Dependencies

None — this is the foundational story. All other stories depend on this.

**Estimated Complexity:** Medium (2 days)
