# ADR-009: Webhook Delivery Metrics — On-Demand SQL Aggregate

**Date:** 2026-03-18
**Status:** Accepted
**Author:** Solution Architect (BMAD Epic 5)

---

## Context

The PRD requires a `GET /v1/webhooks/{id}/metrics` endpoint that returns 7-day delivery statistics: success rate, failure count, DLQ depth, average latency, and a per-day timeline chart.

The `webhook_deliveries` table already records every delivery attempt with `status`, `attempted_at`, `delivered_at`, and `webhook_id`. The question is whether to:
- **(A)** Query this table on-demand with SQL aggregates each time metrics are requested
- **(B)** Pre-aggregate metrics into a separate `webhook_metrics` table via background job

---

## Decision

**Option A: On-demand SQL aggregate** from `webhook_deliveries` with a covering index.

Add a compound index: `(webhook_id, org_id, attempted_at, status)` to support efficient 7-day window aggregation.

---

## Rationale

1. **Simplicity**: No new table, no background job, no cache invalidation. One SQL query returns fresh metrics.

2. **Data volume is manageable**: A typical enterprise webhook fires at most hundreds of times per day. 7 days × 500 deliveries = 3,500 rows — well within PostgreSQL's inline aggregate performance (sub-10ms with the covering index).

3. **Always fresh**: On-demand query returns metrics as of the current moment. Pre-aggregated metrics would require careful incremental update logic to avoid stale DLQ depth counts.

4. **The covering index handles scale**: For high-volume orgs (10,000+ deliveries/day), the compound index `(webhook_id, org_id, attempted_at, status)` allows PostgreSQL to resolve the 7-day aggregate from an index-only scan.

5. **Avoid premature optimization**: Pre-aggregation introduces a background job, scheduling logic, failure handling, and a second source of truth for delivery data. This overhead is not justified at current scale.

---

## Consequences

**Positive:**
- Zero additional tables or jobs
- Fresh data on every request
- Simple query, easy to test

**Negative:**
- **At extreme scale** (10M+ deliveries per webhook): query may degrade even with index. **Mitigation**: partition `webhook_deliveries` by `(org_id, date)` in v2 if needed.
- **Latency under concurrent metrics requests**: Multiple concurrent calls to the same webhook's metrics are all hitting the same index range. **Mitigation**: cache `GET /v1/webhooks/{id}/metrics` response in Redis with 60-second TTL (light caching, not pre-aggregation).

---

## Alternatives Considered

### Option B: Pre-aggregated `webhook_metrics` table
- **Pros**: O(1) read; no query-time computation
- **Rejected**: Requires background job + upsert logic; metrics can become stale; DLQ depth in particular is real-time (changes on retry/resolve); adds schema complexity. Disproportionate engineering cost for the problem size.

### Option C: Elasticsearch aggregation
- **Pros**: Already used for analytics; good at time-series aggregation
- **Rejected**: Webhook delivery data lives in PostgreSQL with RLS enforcement. Moving it to Elasticsearch for metrics would require dual-write or a Kafka pipeline. Adds significant complexity. Not warranted for per-webhook 7d stats.
