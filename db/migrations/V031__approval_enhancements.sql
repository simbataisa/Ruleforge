-- Add APPROVER to org_members role check
ALTER TABLE members DROP CONSTRAINT IF EXISTS members_role_check;
ALTER TABLE members ADD CONSTRAINT members_role_check
    CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'VIEWER', 'SOLVER', 'APPROVER'));

-- Add approval_deadline and sla_hours to rule_versions
ALTER TABLE rule_versions ADD COLUMN IF NOT EXISTS approval_deadline TIMESTAMPTZ;
ALTER TABLE rule_versions ADD COLUMN IF NOT EXISTS sla_hours INTEGER;

-- Create rule_version_comments table
CREATE TABLE IF NOT EXISTS rule_version_comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    version     INTEGER NOT NULL,
    author_id   UUID NOT NULL REFERENCES users(id),
    body        VARCHAR(2000) NOT NULL,
    parent_id   UUID REFERENCES rule_version_comments(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rule_version_comments_rule_version
    ON rule_version_comments(rule_id, version);
CREATE INDEX IF NOT EXISTS idx_rule_version_comments_parent
    ON rule_version_comments(parent_id) WHERE parent_id IS NOT NULL;

-- RLS for rule_version_comments
ALTER TABLE rule_version_comments ENABLE ROW LEVEL SECURITY;
CREATE POLICY rule_version_comments_org_isolation ON rule_version_comments
    USING (rule_id IN (
        SELECT r.id FROM rules r
        JOIN spaces s ON s.id = r.space_id
        WHERE s.org_id = current_setting('app.current_org_id', true)::uuid
    ));
