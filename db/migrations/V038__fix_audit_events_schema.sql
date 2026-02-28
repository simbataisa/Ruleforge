-- Add new columns for enriched audit data
ALTER TABLE audit_events
  ADD COLUMN IF NOT EXISTS actor_name    VARCHAR(200),
  ADD COLUMN IF NOT EXISTS resource_name VARCHAR(300),
  ADD COLUMN IF NOT EXISTS actor_type    VARCHAR(50),
  ADD COLUMN IF NOT EXISTS space_id      UUID,
  ADD COLUMN IF NOT EXISTS metadata      JSONB;

-- Give the legacy timestamp column a default so new rows don't require it
ALTER TABLE audit_events ALTER COLUMN timestamp SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_audit_events_resource_type ON audit_events(resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor_id      ON audit_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_org_id_ts     ON audit_events(org_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource_name ON audit_events(resource_name);
