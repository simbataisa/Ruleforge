-- V033: Tags system overhaul + enhanced search indexes
-- Sprint 02: EPIC 1 - Enhanced Rule Search and Filtering

-- 1. Drop old tag system tables if they exist (old UUID-referenced approach)
-- The new approach uses string-based tags directly on rule_tags

-- 2. Recreate rule_tags as normalized string-based tag storage
DROP TABLE IF EXISTS rule_tags CASCADE;
CREATE TABLE rule_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(rule_id, tag)
);

CREATE INDEX idx_rule_tags_tag ON rule_tags(tag);
CREATE INDEX idx_rule_tags_rule_id ON rule_tags(rule_id);

-- 3. Add denormalized tags TEXT[] column on rules for fast list queries
ALTER TABLE rules ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT '{}';
CREATE INDEX IF NOT EXISTS rules_tags_gin ON rules USING gin(tags);

-- 4. Sync trigger: keeps rules.tags in sync with rule_tags
CREATE OR REPLACE FUNCTION sync_rule_tags() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE rules SET tags = (
            SELECT COALESCE(array_agg(rt.tag ORDER BY rt.tag), '{}')
            FROM rule_tags rt WHERE rt.rule_id = NEW.rule_id
        ) WHERE id = NEW.rule_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE rules SET tags = (
            SELECT COALESCE(array_agg(rt.tag ORDER BY rt.tag), '{}')
            FROM rule_tags rt WHERE rt.rule_id = OLD.rule_id
        ) WHERE id = OLD.rule_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_rule_tags
    AFTER INSERT OR DELETE ON rule_tags
    FOR EACH ROW EXECUTE FUNCTION sync_rule_tags();

-- 5. New indexes for enhanced search filters
CREATE INDEX IF NOT EXISTS idx_rules_created_by ON rules(created_by);
CREATE INDEX IF NOT EXISTS idx_rules_updated_at ON rules(updated_at);

-- 6. RLS policies for rule_tags
ALTER TABLE rule_tags ENABLE ROW LEVEL SECURITY;

CREATE POLICY rule_tags_org_isolation ON rule_tags
    USING (
        EXISTS (
            SELECT 1 FROM rules r
            JOIN spaces s ON s.id = r.space_id
            WHERE r.id = rule_tags.rule_id
              AND s.org_id = current_setting('app.current_org_id')::uuid
        )
    );
