# ADR-002: ruleCount Computation Strategy

## Status
Accepted

## Context

The `OrgSpace` frontend type declares `ruleCount: number`. The backend must provide this value. Three strategies were considered:

1. **Denormalized counter** — Store `rule_count` in `spaces` table, increment/decrement on rule create/delete
2. **Cross-service REST call** — auth-service calls management-service's `/rules/count?spaceId=:id`
3. **Real-time SQL COUNT** — auth-service queries `rules` table directly via COUNT SQL

## Decision

**Real-time SQL COUNT in auth-service.** For the list endpoint, a single bulk GROUP BY query fetches counts for all spaces in one round trip. For single-space endpoints, a direct COUNT query is used.

## Rationale

### Why not denormalize?

- **Synchronization complexity**: Every rule create/delete in management-service must update the counter in auth-service's `spaces` table — either via synchronous HTTP (tight coupling) or Kafka events (eventual consistency, lag)
- **Counter drift**: Any bug in the update path silently produces stale counts with no automatic correction
- **Transaction boundaries**: Rule creation in management-service and counter increment in `spaces` are in different services, making atomic updates impossible without distributed transactions

### Why not cross-service REST call?

- **Latency**: The list endpoint would make N+1 HTTP calls (one per space) to management-service
- **Coupling**: auth-service becomes runtime-dependent on management-service being healthy
- **Operational complexity**: Circuit breaker, timeout, fallback logic needed

### Why real-time SQL COUNT?

- **Same database**: auth-service and management-service share the same PostgreSQL instance in RuleForge's architecture (confirmed: `platform/settings.gradle.kts` uses a single shared DB)
- **Single query**: The list endpoint uses one bulk GROUP BY query across all spaceIds
- **Always accurate**: No stale data, no synchronization bugs
- **Simple**: Five lines of `@Query` in `SpaceRepository`

## Implementation

```java
// For list endpoint — single query for all spaces
@Query(value = """
    SELECT r.space_id as spaceId, COUNT(*) as ruleCount
    FROM rules r
    WHERE r.space_id IN :spaceIds
    GROUP BY r.space_id
    """, nativeQuery = true)
List<Object[]> countRulesBySpaceIds(@Param("spaceIds") List<UUID> spaceIds);

// For single space
@Query(value = "SELECT COUNT(*) FROM rules WHERE space_id = :spaceId", nativeQuery = true)
long countRulesBySpaceId(@Param("spaceId") UUID spaceId);
```

In `SpaceService.listSpaces()`:
1. Fetch all `SpaceEntity` for the org
2. Extract spaceIds
3. Run bulk count query
4. Build `Map<UUID, Long>` for O(1) lookup
5. Map each entity to `SpaceDto` with the count

## Consequences

**Positive:**
- Always accurate
- No cross-service coupling
- Simple implementation (two SQL queries in SpaceRepository)
- No counter drift

**Negative:**
- COUNT query adds ~5ms per list call; acceptable for current scale
- Ties auth-service to the `rules` table schema — if rules table moves to a separate DB, this breaks
- For orgs with thousands of spaces and millions of rules, GROUP BY may become slow — revisit with index on `rules(space_id)` (index already exists per management-service schema)

## Assumptions

- auth-service and management-service share the same PostgreSQL database instance (confirmed from `platform/` setup)
- `rules` table has an index on `space_id` (standard for FK columns in Hibernate auto-DDL)
- Current org sizes: <100 spaces per org, <100K rules — within safe range for real-time COUNT

## Review Date
2026-Q3 (revisit if org sizes exceed 500 spaces or 1M rules per org)
