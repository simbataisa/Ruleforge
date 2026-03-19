-- Add columns needed for metrics aggregation
ALTER TABLE webhook_deliveries
    ADD COLUMN IF NOT EXISTS org_id UUID,
    ADD COLUMN IF NOT EXISTS attempted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';

-- Backfill org_id from parent webhooks table for all existing rows
UPDATE webhook_deliveries
    SET org_id = w.org_id
    FROM webhooks w
    WHERE webhook_deliveries.webhook_id = w.id
      AND webhook_deliveries.org_id IS NULL;

-- Enforce NOT NULL now that existing rows are backfilled
ALTER TABLE webhook_deliveries ALTER COLUMN org_id SET NOT NULL;

-- status CHECK constraint
ALTER TABLE webhook_deliveries
    ADD CONSTRAINT IF NOT EXISTS chk_webhook_delivery_status
    CHECK (status IN ('SUCCESS', 'FAILED', 'IN_DLQ'));

-- After ADD COLUMN sets default 'SUCCESS' for all rows, this corrects rows where success=false (old failures).
-- Rows where success=true already have the correct default 'SUCCESS' and are excluded by the WHERE clause.
UPDATE webhook_deliveries SET status = CASE WHEN success THEN 'SUCCESS' ELSE 'FAILED' END
    WHERE status = 'SUCCESS' AND NOT success;

-- Backfill attempted_at from delivered_at for existing rows
UPDATE webhook_deliveries SET attempted_at = delivered_at WHERE attempted_at IS NULL;

-- Drop the broken V050 index (it referenced nonexistent columns) and recreate properly
DROP INDEX IF EXISTS idx_webhook_deliveries_metrics;
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_metrics
    ON webhook_deliveries(webhook_id, attempted_at, status)
    WHERE attempted_at IS NOT NULL;
