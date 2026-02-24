-- ============================================================================
-- V028: Enable RLS on rule_sample_inputs table
--
-- Ensures tenant isolation: users can only access sample inputs for rules
-- belonging to their organization.
-- ============================================================================

ALTER TABLE rule_sample_inputs ENABLE ROW LEVEL SECURITY;

CREATE POLICY rule_sample_inputs_org_isolation ON rule_sample_inputs
    USING (rule_id IN (
        SELECT r.id FROM rules r
        JOIN spaces s ON s.id = r.space_id
        WHERE s.org_id = current_setting('app.current_org_id', true)::uuid
    ));

-- Add missing FK on created_by
ALTER TABLE rule_sample_inputs
    ADD CONSTRAINT fk_sample_inputs_created_by FOREIGN KEY (created_by) REFERENCES members(id);

-- Add updated_at for audit trail on mutable records
ALTER TABLE rule_sample_inputs
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
