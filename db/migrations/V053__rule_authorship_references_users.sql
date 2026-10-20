-- Rule authorship columns hold user IDs, not member IDs.
--
-- management-service writes tenant.userId() into rules.created_by, rules.locked_by,
-- rule_versions.created_by and rule_sample_inputs.created_by, and compares them against
-- the current user ID (lock ownership, self-approval checks). The original foreign keys
-- pointed at members(id), so every rule created or copied through the API failed with a
-- foreign key violation. Repoint them at users(id) and convert existing member IDs
-- (seed data) to the matching user IDs.

-- 1. Drop the member-based foreign keys
ALTER TABLE rules              DROP CONSTRAINT IF EXISTS rules_created_by_fkey;
ALTER TABLE rules              DROP CONSTRAINT IF EXISTS rules_locked_by_fkey;
ALTER TABLE rule_versions      DROP CONSTRAINT IF EXISTS rule_versions_created_by_fkey;
ALTER TABLE rule_sample_inputs DROP CONSTRAINT IF EXISTS fk_sample_inputs_created_by;

-- 2. Convert member IDs to user IDs
UPDATE rules r              SET created_by = m.user_id FROM members m WHERE r.created_by = m.id;
UPDATE rules r              SET locked_by  = m.user_id FROM members m WHERE r.locked_by  = m.id;
UPDATE rule_versions v      SET created_by = m.user_id FROM members m WHERE v.created_by = m.id;
UPDATE rule_sample_inputs s SET created_by = m.user_id FROM members m WHERE s.created_by = m.id;

-- 3. Reference users instead
ALTER TABLE rules
    ADD CONSTRAINT rules_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT rules_locked_by_fkey  FOREIGN KEY (locked_by)  REFERENCES users(id);
ALTER TABLE rule_versions
    ADD CONSTRAINT rule_versions_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE rule_sample_inputs
    ADD CONSTRAINT fk_sample_inputs_created_by FOREIGN KEY (created_by) REFERENCES users(id);
