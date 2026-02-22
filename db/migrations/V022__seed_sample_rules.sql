-- Sample rules for each rule type for local development
-- All created in Acme Corp's Default Space by admin user

-- Decision Table: Loan Eligibility
-- Engine expects: columns[{name, columnType, operator, dataType}], rows[{rowNumber, priority, conditions[{value}], actions[{value}]}]
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'Loan Eligibility', 'loan-eligibility', 'TABLE',
     'Determines loan eligibility based on credit score and income',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000001', 1,
     '{"hitPolicy":"FIRST","columns":[{"name":"creditScore","columnType":"CONDITION","operator":"GTE","dataType":"ANY","index":0},{"name":"income","columnType":"CONDITION","operator":"GTE","dataType":"ANY","index":1},{"name":"eligible","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":2},{"name":"maxLoan","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":3}],"rows":[{"rowNumber":1,"priority":0,"conditions":[{"value":750},{"value":50000}],"actions":[{"value":"true"},{"value":500000}]},{"rowNumber":2,"priority":0,"conditions":[{"value":650},{"value":30000}],"actions":[{"value":"true"},{"value":200000}]},{"rowNumber":3,"priority":0,"conditions":[{"value":0},{"value":0}],"actions":[{"value":"false"},{"value":0}]}]}',
     '{"creditScore":"INTEGER","income":"INTEGER"}',
     '{"eligible":"STRING","maxLoan":"INTEGER"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- Decision Tree: Insurance Risk Assessment
-- Engine expects: root{nodeType, nodeId, field, operator, branches[{value, child}]}
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000002',
     'd0000000-0000-0000-0000-000000000001',
     'Insurance Risk Assessment', 'insurance-risk', 'TREE',
     'Assesses insurance risk category based on age and driving history',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000002',
     'e0000000-0000-0000-0000-000000000002', 1,
     '{"root":{"nodeType":"CONDITION","nodeId":"n1","field":"age","operator":"LT","branches":[{"value":"25","child":{"nodeType":"CONDITION","nodeId":"n2","field":"accidents","operator":"GT","branches":[{"value":"0","child":{"nodeType":"ACTION","nodeId":"n3","outputs":{"riskCategory":"HIGH","premium":"1.5"}}},{"value":"-1","child":{"nodeType":"ACTION","nodeId":"n4","outputs":{"riskCategory":"MEDIUM","premium":"1.2"}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n4b","outputs":{"riskCategory":"MEDIUM","premium":"1.2"}}}},{"value":"999","child":{"nodeType":"CONDITION","nodeId":"n5","field":"accidents","operator":"GT","branches":[{"value":"2","child":{"nodeType":"ACTION","nodeId":"n6","outputs":{"riskCategory":"HIGH","premium":"1.4"}}},{"value":"-1","child":{"nodeType":"ACTION","nodeId":"n7","outputs":{"riskCategory":"LOW","premium":"1.0"}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n7b","outputs":{"riskCategory":"LOW","premium":"1.0"}}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n8","outputs":{"riskCategory":"LOW","premium":"1.0"}}},"scoringMode":false,"earlyTermination":true}',
     '{"age":"INTEGER","accidents":"INTEGER"}',
     '{"riskCategory":"STRING","premium":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- Scorecard: Customer Credit Score
-- Engine expects: baseScore, strategy, categories[{name, weight, attributes[{name, factPath, ranges[{min, max, points, label}]}]}]
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000003',
     'd0000000-0000-0000-0000-000000000001',
     'Customer Credit Scoring', 'credit-scoring', 'SCORECARD',
     'Computes a weighted credit score from multiple financial factors',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000003',
     'e0000000-0000-0000-0000-000000000003', 1,
     '{"baseScore":300,"strategy":"ADDITIVE","categories":[{"name":"Payment History","weight":0.35,"attributes":[{"name":"paymentHistory","factPath":"paymentHistory","defaultPoints":0,"ranges":[{"min":95,"max":100,"points":100,"label":"Excellent"},{"min":80,"max":94,"points":70,"label":"Good"},{"min":60,"max":79,"points":40,"label":"Fair"},{"min":0,"max":59,"points":10,"label":"Poor"}]}]},{"name":"Debt Ratio","weight":0.30,"attributes":[{"name":"debtToIncome","factPath":"debtToIncome","defaultPoints":0,"ranges":[{"min":0,"max":20,"points":100,"label":"Low"},{"min":21,"max":40,"points":60,"label":"Medium"},{"min":41,"max":100,"points":20,"label":"High"}]}]},{"name":"Account Age","weight":0.15,"attributes":[{"name":"accountAge","factPath":"accountAge","defaultPoints":0,"ranges":[{"min":10,"max":50,"points":100,"label":"Long"},{"min":3,"max":9,"points":60,"label":"Medium"},{"min":0,"max":2,"points":20,"label":"Short"}]}]},{"name":"Credit Mix","weight":0.20,"attributes":[{"name":"creditMix","factPath":"creditMix","defaultPoints":0,"ranges":[{"min":3,"max":10,"points":100,"label":"Diverse"},{"min":1,"max":2,"points":50,"label":"Limited"},{"min":0,"max":0,"points":10,"label":"Single","minInclusive":true,"maxInclusive":true}]}]}]}',
     '{"paymentHistory":"DECIMAL","debtToIncome":"DECIMAL","accountAge":"INTEGER","creditMix":"INTEGER"}',
     '{"score":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- Script: Dynamic Pricing
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000004',
     'd0000000-0000-0000-0000-000000000001',
     'Dynamic Pricing Engine', 'dynamic-pricing', 'SCRIPT',
     'Calculates dynamic product pricing based on demand and inventory',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000004',
     'e0000000-0000-0000-0000-000000000004', 1,
     '{"language":"javascript","scriptBody":"function execute(input) { var basePrice = input.basePrice; var factor = Math.min(2.0, Math.max(0.5, input.demandIndex / 100)); var finalPrice = basePrice * factor; return { finalPrice: Math.round(finalPrice * 100) / 100 }; }"}',
     '{"basePrice":"DECIMAL","demandIndex":"INTEGER"}',
     '{"finalPrice":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- Flow: Order Processing Pipeline
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000005',
     'd0000000-0000-0000-0000-000000000001',
     'Order Processing Pipeline', 'order-pipeline', 'FLOW',
     'Multi-step order validation flow',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000005',
     'e0000000-0000-0000-0000-000000000005', 1,
     '{"nodes":[{"id":"start","type":"START","next":"check"},{"id":"check","type":"CONDITION","field":"orderTotal","operator":"GT","value":0,"trueNext":"approve","falseNext":"reject"},{"id":"approve","type":"END","output":{"status":"APPROVED"}},{"id":"reject","type":"END","output":{"status":"REJECTED"}}],"edges":[]}',
     '{"orderTotal":"DECIMAL"}',
     '{"status":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- DMN: Vacation Days Calculation
-- Engine expects: decisions[{id, name, decisionTable{hitPolicy, inputClauses, outputClauses, rules}}]
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000006',
     'd0000000-0000-0000-0000-000000000001',
     'Vacation Days Calculator', 'vacation-days', 'DMN',
     'DMN decision table for calculating employee vacation days',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000006',
     'e0000000-0000-0000-0000-000000000006', 1,
     '{"decisions":[{"id":"vacation-days","name":"Vacation Days","decisionTable":{"hitPolicy":"FIRST","inputClauses":[{"name":"yearsOfService","expression":"yearsOfService"},{"name":"role","expression":"role"}],"outputClauses":[{"name":"vacationDays"}],"rules":[{"ruleNumber":1,"inputEntries":[">=0","Junior"],"outputEntries":["15"]},{"ruleNumber":2,"inputEntries":[">=0","Senior"],"outputEntries":["20"]},{"ruleNumber":3,"inputEntries":[">=5","Junior"],"outputEntries":["25"]},{"ruleNumber":4,"inputEntries":[">=5","Senior"],"outputEntries":["28"]},{"ruleNumber":5,"inputEntries":[">=5","Manager"],"outputEntries":["30"]}]}}]}',
     '{"yearsOfService":"INTEGER","role":"STRING"}',
     '{"vacationDays":"INTEGER"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- Ensemble: Fraud Detection (combines multiple models)
-- Engine expects: strategy, outputField, members[{ruleId, alias, weight}]
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000007',
     'd0000000-0000-0000-0000-000000000001',
     'Fraud Detection Ensemble', 'fraud-detection', 'ENSEMBLE',
     'Combines multiple fraud detection rules with weighted voting',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000007',
     'e0000000-0000-0000-0000-000000000007', 1,
     '{"strategy":"WEIGHTED_AVERAGE","outputField":"fraudScore","members":[{"ruleId":"e0000000-0000-0000-0000-000000000001","alias":"loan-check","weight":0.5},{"ruleId":"e0000000-0000-0000-0000-000000000002","alias":"risk-check","weight":0.5}]}',
     '{"amount":"DECIMAL","country":"STRING"}',
     '{"fraudScore":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version')
ON CONFLICT (rule_id, version) DO NOTHING;

-- CEP: Real-time Transaction Monitoring
-- Engine expects: patternType, eventSchemas, timeWindow, actions
INSERT INTO rules (id, space_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000008',
     'd0000000-0000-0000-0000-000000000001',
     'Transaction Velocity Monitor', 'txn-velocity', 'CEP',
     'Detects suspicious transaction velocity patterns',
     'DRAFT', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f0000000-0000-0000-0000-000000000008',
     'e0000000-0000-0000-0000-000000000008', 1,
     '{"patternType":"FREQUENCY","eventSchemas":[{"eventType":"transaction","fields":{"amount":"number","country":"string"},"filterConditions":[{"field":"amount","operator":"GT","value":"100"}]}],"timeWindow":{"type":"TUMBLING","duration":300,"graceTime":0},"actions":[{"actionType":"EMIT_EVENT","parameters":{}}],"inputTopic":"transactions","outputTopic":"alerts"}',
     '{"transactionId":"STRING","amount":"DECIMAL","country":"STRING"}',
     '{"alertType":"STRING"}',
     'DRAFT', 'c0000000-0000-0000-0000-000000000001', 'Initial draft')
ON CONFLICT (rule_id, version) DO NOTHING;
