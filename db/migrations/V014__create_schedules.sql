CREATE TABLE schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    rule_id         UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    input_template  JSONB NOT NULL DEFAULT '{}',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    description     TEXT,
    created_by      UUID NOT NULL REFERENCES members(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedules_space_id ON schedules(space_id);
CREATE INDEX idx_schedules_rule_id ON schedules(rule_id);
CREATE INDEX idx_schedules_enabled ON schedules(enabled);
