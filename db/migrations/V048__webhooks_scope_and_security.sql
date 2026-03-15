-- V048__webhooks_scope_and_security.sql

-- Step 1: Drop RLS policy FIRST (it references space_id via the spaces join)
DROP POLICY IF EXISTS webhooks_org_isolation ON webhooks;

-- Step 2: Add new columns as nullable (scope_id MUST be nullable to backfill from space_id before drop)
-- NOTE: created_by already exists from V012 — do NOT re-add it
ALTER TABLE webhooks
  ADD COLUMN IF NOT EXISTS scope_type           VARCHAR(10) NOT NULL DEFAULT 'SPACE',
  ADD COLUMN IF NOT EXISTS scope_id             UUID,       -- nullable until backfilled
  ADD COLUMN IF NOT EXISTS org_id               UUID,       -- nullable until backfilled
  ADD COLUMN IF NOT EXISTS name                 VARCHAR(255),
  ADD COLUMN IF NOT EXISTS consecutive_failures INT  NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS circuit_opened_at    TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS last_delivered_at    TIMESTAMPTZ;

-- Step 3: Backfill scope_id from space_id BEFORE dropping space_id
UPDATE webhooks SET scope_id = space_id;

-- Step 4: Backfill org_id via spaces join (scope_id now holds the old space_id value)
UPDATE webhooks w
SET org_id = s.org_id
FROM spaces s
WHERE w.scope_id = s.id;

-- Delete webhooks whose space no longer exists (orphaned — cannot determine org)
DELETE FROM webhooks WHERE org_id IS NULL AND scope_id IS NOT NULL;

-- Validate no NULL org_id remain before enforcing NOT NULL
DO $$
BEGIN
  IF EXISTS(SELECT 1 FROM webhooks WHERE org_id IS NULL) THEN
    RAISE EXCEPTION 'V048 backfill incomplete: % webhooks have NULL org_id',
      (SELECT COUNT(*) FROM webhooks WHERE org_id IS NULL);
  END IF;
END $$;

-- Step 5: Drop space_id AFTER backfill is complete
ALTER TABLE webhooks DROP COLUMN IF EXISTS space_id;

-- Step 6: Enforce NOT NULL now that all rows are populated
ALTER TABLE webhooks ALTER COLUMN scope_id SET NOT NULL;
ALTER TABLE webhooks ALTER COLUMN org_id   SET NOT NULL;

-- Step 7: Fix events column type mismatch (V012 declared TEXT[], entity uses JSONB)
ALTER TABLE webhooks ALTER COLUMN events TYPE JSONB USING events::text::jsonb;

CREATE INDEX IF NOT EXISTS idx_webhooks_scope  ON webhooks(scope_type, scope_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_org_id ON webhooks(org_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_active ON webhooks(org_id, active) WHERE active = true;

-- Step 8: Recreate RLS using org_id directly (no join needed)
CREATE POLICY webhooks_org_isolation ON webhooks
  USING (org_id = current_setting('app.current_org_id')::uuid);
