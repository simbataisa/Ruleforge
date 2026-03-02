-- Workflows table
CREATE TABLE IF NOT EXISTS workflows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID NOT NULL REFERENCES spaces(id),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    definition      JSONB NOT NULL DEFAULT '{"nodes":[],"edges":[]}',
    enabled         BOOLEAN NOT NULL DEFAULT false,
    cron_expression VARCHAR(100),
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Workflow runs table
CREATE TABLE IF NOT EXISTS workflow_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    step_results    JSONB DEFAULT '[]',
    error           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed workflow 1: Loan Processing Pipeline
INSERT INTO workflows (id, space_id, name, description, definition, enabled, cron_expression, created_by) VALUES
('f1000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
 'Loan Processing Pipeline',
 'End-to-end loan processing: eligibility check followed by insurance risk assessment',
 '{
   "nodes": [
     {"id": "trigger-1", "type": "trigger", "position": {"x": 250, "y": 0}, "data": {"label": "Cron Trigger", "config": {"cronExpression": "0 0 8 * * ?"}}},
     {"id": "rule-1", "type": "rule", "position": {"x": 250, "y": 150}, "data": {"label": "Loan Eligibility", "ruleId": "e0000000-0000-0000-0000-000000000001", "ruleName": "Loan Eligibility", "ruleType": "TABLE"}},
     {"id": "rule-2", "type": "rule", "position": {"x": 250, "y": 300}, "data": {"label": "Insurance Risk", "ruleId": "e0000000-0000-0000-0000-000000000002", "ruleName": "Insurance Risk Assessment", "ruleType": "TREE"}},
     {"id": "output-1", "type": "output", "position": {"x": 250, "y": 450}, "data": {"label": "Decision Output", "config": {"outputType": "webhook"}}}
   ],
   "edges": [
     {"id": "e1", "source": "trigger-1", "target": "rule-1"},
     {"id": "e2", "source": "rule-1", "target": "rule-2"},
     {"id": "e3", "source": "rule-2", "target": "output-1"}
   ]
 }',
 true, '0 0 8 * * ?', 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO UPDATE SET definition = EXCLUDED.definition, updated_at = NOW();

-- Seed workflow 2: Dynamic Pricing Chain
INSERT INTO workflows (id, space_id, name, description, definition, enabled, cron_expression, created_by) VALUES
('f1000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001',
 'Dynamic Pricing Chain',
 'Compute dynamic pricing then apply vacation day adjustments',
 '{
   "nodes": [
     {"id": "trigger-1", "type": "trigger", "position": {"x": 250, "y": 0}, "data": {"label": "Manual Trigger", "config": {}}},
     {"id": "rule-1", "type": "rule", "position": {"x": 250, "y": 150}, "data": {"label": "Dynamic Pricing", "ruleId": "e0000000-0000-0000-0000-000000000004", "ruleName": "Dynamic Pricing Engine", "ruleType": "SCRIPT"}},
     {"id": "rule-2", "type": "rule", "position": {"x": 250, "y": 300}, "data": {"label": "Vacation Days", "ruleId": "e0000000-0000-0000-0000-000000000006", "ruleName": "Vacation Days Policy", "ruleType": "DMN"}},
     {"id": "output-1", "type": "output", "position": {"x": 250, "y": 450}, "data": {"label": "Pricing Output", "config": {"outputType": "log"}}}
   ],
   "edges": [
     {"id": "e1", "source": "trigger-1", "target": "rule-1"},
     {"id": "e2", "source": "rule-1", "target": "rule-2"},
     {"id": "e3", "source": "rule-2", "target": "output-1"}
   ]
 }',
 false, NULL, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO UPDATE SET definition = EXCLUDED.definition, updated_at = NOW();

-- Seed workflow runs for workflow 1
INSERT INTO workflow_runs (id, workflow_id, status, started_at, completed_at, step_results, error) VALUES
('f2000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
 NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 58 minutes',
 '[{"nodeId":"rule-1","status":"SUCCESS","duration":120},{"nodeId":"rule-2","status":"SUCCESS","duration":85}]', NULL),
('f2000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
 NOW() - INTERVAL '26 hours', NOW() - INTERVAL '25 hours 57 minutes',
 '[{"nodeId":"rule-1","status":"SUCCESS","duration":115},{"nodeId":"rule-2","status":"SUCCESS","duration":90}]', NULL),
('f2000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001', 'FAILED',
 NOW() - INTERVAL '50 hours', NOW() - INTERVAL '49 hours 59 minutes',
 '[{"nodeId":"rule-1","status":"SUCCESS","duration":130}]', 'Insurance Risk rule evaluation timed out'),
('f2000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001', 'RUNNING',
 NOW() - INTERVAL '5 minutes', NULL, '[{"nodeId":"rule-1","status":"SUCCESS","duration":110}]', NULL)
ON CONFLICT (id) DO NOTHING;

-- Seed workflow runs for workflow 2
INSERT INTO workflow_runs (id, workflow_id, status, started_at, completed_at, step_results, error) VALUES
('f2000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
 NOW() - INTERVAL '5 hours', NOW() - INTERVAL '4 hours 58 minutes',
 '[{"nodeId":"rule-1","status":"SUCCESS","duration":200},{"nodeId":"rule-2","status":"SUCCESS","duration":95}]', NULL),
('f2000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
 NOW() - INTERVAL '29 hours', NOW() - INTERVAL '28 hours 57 minutes',
 '[{"nodeId":"rule-1","status":"SUCCESS","duration":210},{"nodeId":"rule-2","status":"SUCCESS","duration":100}]', NULL),
('f2000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000002', 'CANCELLED',
 NOW() - INTERVAL '53 hours', NOW() - INTERVAL '52 hours 59 minutes',
 '[{"nodeId":"rule-1","status":"RUNNING"}]', 'Cancelled by user')
ON CONFLICT (id) DO NOTHING;
