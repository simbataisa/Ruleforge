-- V014__create_webhook_outbox.sql
-- IMPORTANT: This migration requires V048 (webhooks scope extension) to have been
-- applied first via `make migrate`. Start the event-service only AFTER running
-- `make migrate` to ensure webhooks.scope_id and webhooks.org_id columns exist.

CREATE TABLE IF NOT EXISTS webhook_outbox (
  id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  webhook_id       UUID         NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
  org_id           UUID         NOT NULL,
  event_type       VARCHAR(255) NOT NULL,
  payload          JSONB        NOT NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  attempt_count    INT          NOT NULL DEFAULT 0,
  attempt_nonce    UUID         NOT NULL DEFAULT gen_random_uuid(),
  next_attempt_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  last_attempt_at  TIMESTAMPTZ,
  last_status_code INT,
  last_error       TEXT,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partial index for pending/failed delivery polling
CREATE INDEX IF NOT EXISTS idx_outbox_pending
  ON webhook_outbox(next_attempt_at)
  WHERE (status = 'PENDING' OR status = 'FAILED');

CREATE INDEX IF NOT EXISTS idx_outbox_webhook_id ON webhook_outbox(webhook_id);
CREATE INDEX IF NOT EXISTS idx_outbox_org_id     ON webhook_outbox(org_id);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON webhook_outbox(created_at);
