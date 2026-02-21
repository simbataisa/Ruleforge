CREATE TABLE ab_tests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id    UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    rule_id     UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    version_a   INTEGER NOT NULL,
    version_b   INTEGER NOT NULL,
    traffic_split INTEGER NOT NULL DEFAULT 50 CHECK (traffic_split BETWEEN 0 AND 100),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'STOPPED', 'COMPLETED')),
    start_date  TIMESTAMPTZ,
    end_date    TIMESTAMPTZ,
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ab_tests_rule_id ON ab_tests(rule_id);
CREATE INDEX idx_ab_tests_space_id ON ab_tests(space_id);
CREATE INDEX idx_ab_tests_status ON ab_tests(status) WHERE status = 'ACTIVE';

ALTER TABLE ab_tests ENABLE ROW LEVEL SECURITY;
CREATE POLICY ab_tests_org_isolation ON ab_tests
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', TRUE)::uuid
    ));
