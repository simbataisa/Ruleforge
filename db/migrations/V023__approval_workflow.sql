-- Add approval workflow columns to rule_versions
ALTER TABLE rule_versions ADD COLUMN IF NOT EXISTS approved_by UUID;
ALTER TABLE rule_versions ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE rule_versions ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Update status check constraint to include PENDING_APPROVAL
ALTER TABLE rule_versions DROP CONSTRAINT IF EXISTS rule_versions_status_check;
ALTER TABLE rule_versions ADD CONSTRAINT rule_versions_status_check
    CHECK (status::text = ANY (ARRAY['DRAFT', 'PENDING', 'PUBLISHED', 'DEPRECATED', 'PENDING_APPROVAL']));
