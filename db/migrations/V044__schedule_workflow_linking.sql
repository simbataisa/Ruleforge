-- V044: Link schedules to workflows + add trigger types to workflows

ALTER TABLE schedules ADD COLUMN IF NOT EXISTS workflow_id UUID REFERENCES workflows(id) ON DELETE SET NULL;
ALTER TABLE schedules ALTER COLUMN rule_id DROP NOT NULL;

ALTER TABLE workflows ADD COLUMN IF NOT EXISTS trigger_type VARCHAR(20) NOT NULL DEFAULT 'CRON';
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS trigger_config JSONB DEFAULT '{}';

-- Link existing seed schedule to the loan-processing workflow
UPDATE schedules SET workflow_id = 'f1000000-0000-0000-0000-000000000001' WHERE name = 'Daily Loan Processing' AND workflow_id IS NULL;
UPDATE workflows SET trigger_type = 'CRON', trigger_config = '{"cronExpression":"0 0 8 * * ?"}' WHERE id = 'f1000000-0000-0000-0000-000000000001';
UPDATE workflows SET trigger_type = 'MANUAL', trigger_config = '{}' WHERE id = 'f1000000-0000-0000-0000-000000000002';
