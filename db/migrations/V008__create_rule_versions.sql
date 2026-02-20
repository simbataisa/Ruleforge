CREATE TABLE rule_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    version         INT NOT NULL,
    definition      JSONB NOT NULL,
    input_model     JSONB NOT NULL DEFAULT '{}',
    output_model    JSONB NOT NULL DEFAULT '{}',
    compiled        BYTEA,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PENDING','PUBLISHED','DEPRECATED')),
    created_by      UUID NOT NULL REFERENCES members(id),
    change_note     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    UNIQUE(rule_id, version)
);

CREATE INDEX idx_rule_versions_rule_id ON rule_versions(rule_id);
CREATE INDEX idx_rule_versions_status ON rule_versions(rule_id, status);
