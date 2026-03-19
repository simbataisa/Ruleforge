# Story 014 — Webhook Auth DB Schema + Entity Migration

**Story ID:** story-014
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Backend Engineer
**Priority:** P0 — Sprint 5.1 (must land first; all other stories depend on this)
**Estimate:** 2 story points
**Depends on:** None (foundation story)

---

## User Story

**As a** backend engineer,
**I want** the `webhooks` table to have `auth_type`, `auth_config`, and `retry_*` columns with safe defaults,
**So that** all subsequent auth + retry stories have a schema to build on, and existing webhooks continue to work without any re-configuration.

---

## Background

The existing `webhooks` table has a `secret` VARCHAR column for HMAC signing. To support multiple auth types, we need:
1. `auth_type` — enum-constrained VARCHAR discriminator (not encrypted; needed by OutboxPoller before decrypt)
2. `auth_config` — TEXT to hold AES-256-GCM encrypted JSON blob of credentials
3. `retry_initial_delay_seconds`, `retry_backoff_multiplier`, `retry_max_attempts`, `retry_on_status_codes` — per-webhook retry tuning

All new columns have safe defaults so existing rows are untouched and still deliver correctly.

V051 sets `auth_type = 'SIGNING_SECRET'` for rows that have a `secret` value. Actual credential encryption happens in Story 015 (application-layer migration service), not in SQL.

---

## Acceptance Criteria

1. Flyway migration `V050__webhook_auth_config.sql` applies cleanly to an existing database with webhooks rows.
2. All new columns have the correct defaults:
   - `auth_type` defaults to `'SIGNING_SECRET'` (preserves existing behaviour for rows with a secret)
   - `auth_config` nullable (no credentials for NONE type)
   - `retry_initial_delay_seconds` = 30, `retry_backoff_multiplier` = 2.0, `retry_max_attempts` = 5, `retry_on_status_codes` = '429,408,500,502,503,504'
3. `chk_auth_type` constraint rejects unknown auth type strings.
4. `idx_webhooks_auth_type` and `idx_webhook_deliveries_metrics` indexes exist after migration.
5. Flyway migration `V051__migrate_existing_webhook_secrets.sql` sets `auth_type = 'SIGNING_SECRET'` for rows with non-null `secret`, and `auth_type = 'NONE'` for rows with null `secret`. Migration is idempotent.
6. `WebhookEntity.java` updated with new fields: `authType` (enum), `authConfig` (String), `retryInitialDelaySeconds`, `retryBackoffMultiplier`, `retryMaxAttempts`, `retryOnStatusCodes`.
7. `WebhookAuthType` Java enum created with values: `NONE`, `SIGNING_SECRET`, `API_KEY`, `BASIC_AUTH`, `BEARER_TOKEN`, `CUSTOM_HEADER`, `OAUTH2_CLIENT_CREDENTIALS`.
8. `outbox_events` table gains `next_retry_at TIMESTAMPTZ` column (V050).
9. `./gradlew build` passes with 0 errors after entity changes.
10. Existing webhook delivery smoke tests still pass (no regression).

---

## Files to Create

### `db/migrations/V050__webhook_auth_config.sql`

```sql
-- Webhook auth type and encrypted credentials
ALTER TABLE webhooks
    ADD COLUMN IF NOT EXISTS auth_type VARCHAR(32) NOT NULL DEFAULT 'SIGNING_SECRET',
    ADD COLUMN IF NOT EXISTS auth_config TEXT;

ALTER TABLE webhooks
    ADD CONSTRAINT chk_auth_type CHECK (
        auth_type IN ('NONE', 'SIGNING_SECRET', 'API_KEY', 'BASIC_AUTH',
                      'BEARER_TOKEN', 'CUSTOM_HEADER', 'OAUTH2_CLIENT_CREDENTIALS')
    );

-- Retry config with safe defaults
ALTER TABLE webhooks
    ADD COLUMN IF NOT EXISTS retry_initial_delay_seconds INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS retry_backoff_multiplier NUMERIC(4,2) NOT NULL DEFAULT 2.0,
    ADD COLUMN IF NOT EXISTS retry_max_attempts INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS retry_on_status_codes VARCHAR(100) NOT NULL DEFAULT '429,408,500,502,503,504';

-- Index for OutboxPoller auth_type routing
CREATE INDEX IF NOT EXISTS idx_webhooks_auth_type ON webhooks(auth_type);

-- next_retry_at for per-webhook backoff scheduling
ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

-- Composite index for metrics aggregation (Story 018)
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_metrics
    ON webhook_deliveries(webhook_id, org_id, attempted_at, status);
```

### `db/migrations/V051__migrate_existing_webhook_secrets.sql`

```sql
-- Set auth_type based on presence of existing secret
UPDATE webhooks
SET auth_type = 'SIGNING_SECRET'
WHERE secret IS NOT NULL
  AND auth_type = 'SIGNING_SECRET';  -- idempotent

UPDATE webhooks
SET auth_type = 'NONE'
WHERE secret IS NULL
  AND auth_type = 'SIGNING_SECRET';

-- auth_config will be populated by WebhookCredentialMigrationService at app startup
-- This migration only sets the discriminator column
```

---

## Files to Modify

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/entity/WebhookEntity.java`

Add new fields:
```java
import io.ruleforge.event.model.WebhookAuthType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.math.BigDecimal;

// Add to existing entity fields:

@Column(name = "auth_type", nullable = false)
@Enumerated(EnumType.STRING)
private WebhookAuthType authType = WebhookAuthType.SIGNING_SECRET;

@Column(name = "auth_config", columnDefinition = "TEXT")
private String authConfig;  // encrypted JSON blob; null for NONE type

@Column(name = "retry_initial_delay_seconds", nullable = false)
private int retryInitialDelaySeconds = 30;

@Column(name = "retry_backoff_multiplier", nullable = false, precision = 4, scale = 2)
private BigDecimal retryBackoffMultiplier = new BigDecimal("2.0");

@Column(name = "retry_max_attempts", nullable = false)
private int retryMaxAttempts = 5;

@Column(name = "retry_on_status_codes", nullable = false)
private String retryOnStatusCodes = "429,408,500,502,503,504";
```

**Also:** Add `@ToString.Exclude` (Lombok) or override `toString()` to exclude `authConfig` — **this field must never appear in logs**.

---

## Files to Create (Java)

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/model/WebhookAuthType.java`

```java
package io.ruleforge.event.model;

public enum WebhookAuthType {
    NONE,
    SIGNING_SECRET,
    API_KEY,
    BASIC_AUTH,
    BEARER_TOKEN,
    CUSTOM_HEADER,
    OAUTH2_CLIENT_CREDENTIALS
}
```

---

## Definition of Done

- [ ] `V050__webhook_auth_config.sql` and `V051__migrate_existing_webhook_secrets.sql` in `db/migrations/`
- [ ] Both migrations apply cleanly: `make migrate` succeeds
- [ ] `WebhookAuthType` enum created in `io.ruleforge.event.model`
- [ ] `WebhookEntity` updated with all new fields and correct defaults
- [ ] `authConfig` excluded from `toString()` / Lombok `@ToString.Exclude`
- [ ] `./gradlew :ruleforge-event-service:build` passes
- [ ] Existing webhook delivery smoke tests pass

---

## Notes for Implementer

- Do NOT drop the `secret` column in this story. It remains for the credential migration in Story 015.
- `auth_type` default is `'SIGNING_SECRET'` (not `'NONE'`) so that existing rows with secrets continue to use HMAC signing after V050 runs. V051 only resets to `'NONE'` for rows where `secret IS NULL`.
- The `@Enumerated(EnumType.STRING)` annotation is critical — do not use `EnumType.ORDINAL` (breaks on enum reordering).
- `retry_on_status_codes` stored as a comma-separated VARCHAR (e.g., `"429,408,500"`) — parse at runtime in `OutboxPoller`. Avoids ARRAY type for simpler portability.
