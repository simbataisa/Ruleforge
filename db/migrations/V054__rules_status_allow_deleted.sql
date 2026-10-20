-- Deleting a rule is a soft delete (RuleService sets status = 'DELETED'), but the
-- original check constraint only allowed DRAFT / PUBLISHED / ARCHIVED, so every
-- delete failed with a constraint violation. Allow DELETED.
ALTER TABLE rules DROP CONSTRAINT IF EXISTS rules_status_check;
ALTER TABLE rules ADD CONSTRAINT rules_status_check
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED'));
