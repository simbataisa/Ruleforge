-- Enable Row Level Security on all tenant-scoped tables
ALTER TABLE spaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE rule_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE rule_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;

-- RLS policies: users can only access data in their organization
-- The application sets app.current_org_id via SET before each query

CREATE POLICY spaces_org_isolation ON spaces
    USING (org_id = current_setting('app.current_org_id', true)::uuid);

CREATE POLICY folders_org_isolation ON folders
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY rules_org_isolation ON rules
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY rule_versions_org_isolation ON rule_versions
    USING (rule_id IN (
        SELECT r.id FROM rules r
        JOIN spaces s ON s.id = r.space_id
        WHERE s.org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY api_keys_org_isolation ON api_keys
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY tags_org_isolation ON tags
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY rule_tags_org_isolation ON rule_tags
    USING (rule_id IN (
        SELECT r.id FROM rules r
        JOIN spaces s ON s.id = r.space_id
        WHERE s.org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY webhooks_org_isolation ON webhooks
    USING (space_id IN (
        SELECT id FROM spaces WHERE org_id = current_setting('app.current_org_id', true)::uuid
    ));

CREATE POLICY audit_events_org_isolation ON audit_events
    USING (org_id = current_setting('app.current_org_id', true)::uuid);
