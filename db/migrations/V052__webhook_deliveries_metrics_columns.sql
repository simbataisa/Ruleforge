-- Add columns needed for metrics aggregation
ALTER TABLE webhook_deliveries
    ADD COLUMN IF NOT EXISTS org_id UUID,
    ADD COLUMN IF NOT EXISTS attempted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_metrics
    ON webhook_deliveries(webhook_id, org_id, attempted_at, status);
