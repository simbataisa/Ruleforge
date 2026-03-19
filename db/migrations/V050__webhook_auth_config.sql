-- Webhook auth type and encrypted credentials
ALTER TABLE webhooks
    ADD COLUMN IF NOT EXISTS auth_type VARCHAR(32) NOT NULL DEFAULT 'SIGNING_SECRET',
    ADD COLUMN IF NOT EXISTS auth_config TEXT;

ALTER TABLE webhooks
    ADD CONSTRAINT IF NOT EXISTS chk_auth_type CHECK (
        auth_type IN ('NONE', 'SIGNING_SECRET', 'API_KEY', 'BASIC_AUTH',
                      'BEARER_TOKEN', 'CUSTOM_HEADER', 'OAUTH2_CLIENT_CREDENTIALS')
    );

ALTER TABLE webhooks
    ADD COLUMN IF NOT EXISTS retry_initial_delay_seconds INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS retry_backoff_multiplier NUMERIC(4,2) NOT NULL DEFAULT 2.0,
    ADD COLUMN IF NOT EXISTS retry_max_attempts INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS retry_on_status_codes VARCHAR(100) NOT NULL DEFAULT '429,408,500,502,503,504';

CREATE INDEX IF NOT EXISTS idx_webhooks_auth_type ON webhooks(auth_type);

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_metrics
    ON webhook_deliveries(webhook_id, org_id, attempted_at, status);
