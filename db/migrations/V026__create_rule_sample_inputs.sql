-- ============================================================================
-- V026: Create rule_sample_inputs table
--
-- Stores predefined sample inputs for each rule, enabling one-click testing
-- from the TestBench UI.
-- ============================================================================

CREATE TABLE rule_sample_inputs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    input       JSONB NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_by  UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(rule_id, name)
);

CREATE INDEX idx_rule_sample_inputs_rule_id ON rule_sample_inputs(rule_id);
