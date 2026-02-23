-- ============================================================================
-- V024: Comprehensive Sample Rules
--
-- Adds rich, realistic sample rules for each of the 8 rule types to showcase
-- the full capabilities of RuleForge. These complement the simpler V022
-- samples and are organized into a "Samples" folder.
--
-- All rules belong to Acme Corp (org b0...01), Default Space (d0...01),
-- created by admin user (c0...01).
--
-- UUID scheme: e1000000-0000-0000-0000-00000000000X (rules)
--              f1000000-0000-0000-0000-00000000000X (versions)
--              aa000000-0000-0000-0000-000000000001 (folder)
-- ============================================================================

-- Create a "Showcase Rules" folder to organize the new sample rules
INSERT INTO folders (id, space_id, name, updated_at) VALUES
    ('aa000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'Showcase Rules',
     now())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 1. TABLE — Employee Benefits Eligibility
-- Use case: HR determines benefits tier based on department, tenure, and performance
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Employee Benefits Eligibility', 'employee-benefits', 'TABLE',
     'Determines employee benefits tier, bonus percentage, vacation days, and health plan based on department, tenure, performance, and employment type',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000001',
     'e1000000-0000-0000-0000-000000000001', 1,
     '{
  "hitPolicy": "FIRST",
  "columns": [
    {"name":"department","columnType":"CONDITION","operator":"EQ","dataType":"ANY","index":0},
    {"name":"yearsOfService","columnType":"CONDITION","operator":"GTE","dataType":"ANY","index":1},
    {"name":"performanceRating","columnType":"CONDITION","operator":"EQ","dataType":"ANY","index":2},
    {"name":"employeeType","columnType":"CONDITION","operator":"EQ","dataType":"ANY","index":3},
    {"name":"benefitsTier","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":4},
    {"name":"bonusPercent","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":5},
    {"name":"vacationDays","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":6},
    {"name":"healthPlan","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":7}
  ],
  "rows": [
    {"rowNumber":1,"priority":1,"conditions":[{"value":"any"},{"value":0},{"value":"any"},{"value":"contractor"}],"actions":[{"value":"none"},{"value":0},{"value":0},{"value":"none"}]},
    {"rowNumber":2,"priority":0,"conditions":[{"value":"Engineering"},{"value":5},{"value":"Exceeds"},{"value":"full-time"}],"actions":[{"value":"platinum"},{"value":20},{"value":30},{"value":"premium"}]},
    {"rowNumber":3,"priority":0,"conditions":[{"value":"Engineering"},{"value":3},{"value":"Meets"},{"value":"full-time"}],"actions":[{"value":"gold"},{"value":12},{"value":25},{"value":"standard"}]},
    {"rowNumber":4,"priority":0,"conditions":[{"value":"Engineering"},{"value":0},{"value":"Meets"},{"value":"full-time"}],"actions":[{"value":"silver"},{"value":8},{"value":20},{"value":"standard"}]},
    {"rowNumber":5,"priority":0,"conditions":[{"value":"Sales"},{"value":5},{"value":"Exceeds"},{"value":"full-time"}],"actions":[{"value":"platinum"},{"value":25},{"value":25},{"value":"premium"}]},
    {"rowNumber":6,"priority":0,"conditions":[{"value":"Sales"},{"value":2},{"value":"Meets"},{"value":"full-time"}],"actions":[{"value":"gold"},{"value":15},{"value":22},{"value":"standard"}]},
    {"rowNumber":7,"priority":0,"conditions":[{"value":"Sales"},{"value":0},{"value":"Below"},{"value":"full-time"}],"actions":[{"value":"bronze"},{"value":5},{"value":18},{"value":"basic"}]},
    {"rowNumber":8,"priority":0,"conditions":[{"value":"Marketing"},{"value":5},{"value":"Exceeds"},{"value":"full-time"}],"actions":[{"value":"gold"},{"value":15},{"value":25},{"value":"premium"}]},
    {"rowNumber":9,"priority":0,"conditions":[{"value":"Marketing"},{"value":2},{"value":"Meets"},{"value":"full-time"}],"actions":[{"value":"silver"},{"value":10},{"value":22},{"value":"standard"}]},
    {"rowNumber":10,"priority":0,"conditions":[{"value":"Support"},{"value":3},{"value":"Exceeds"},{"value":"full-time"}],"actions":[{"value":"gold"},{"value":12},{"value":24},{"value":"standard"}]},
    {"rowNumber":11,"priority":0,"conditions":[{"value":"Support"},{"value":0},{"value":"Meets"},{"value":"full-time"}],"actions":[{"value":"silver"},{"value":8},{"value":20},{"value":"basic"}]},
    {"rowNumber":12,"priority":0,"conditions":[{"value":"any"},{"value":0},{"value":"any"},{"value":"full-time"}],"actions":[{"value":"bronze"},{"value":5},{"value":18},{"value":"basic"}]}
  ]
}',
     '{"department":"STRING","yearsOfService":"INTEGER","performanceRating":"STRING","employeeType":"STRING"}',
     '{"benefitsTier":"STRING","bonusPercent":"DECIMAL","vacationDays":"INTEGER","healthPlan":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version with 12 rules covering Engineering, Sales, Marketing, Support, and contractors')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 2. TREE — Customer Support Ticket Routing
-- Use case: Route tickets to correct team with SLA based on priority and tier
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000002',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Support Ticket Routing', 'ticket-routing', 'TREE',
     'Routes customer support tickets to the appropriate team with SLA targets based on priority, customer tier, and issue category',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000002',
     'e1000000-0000-0000-0000-000000000002', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "priority",
    "field": "priority",
    "operator": "EQ",
    "branches": [
      {
        "value": "P1",
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "p1_tier",
          "field": "customerTier",
          "operator": "EQ",
          "branches": [
            {
              "value": "Enterprise",
              "child": {"nodeType":"ACTION","nodeId":"p1_ent","outputs":{"team":"Senior Engineering","slaHours":"1","escalationLevel":"VP","autoResponse":"false"}}
            },
            {
              "value": "Business",
              "child": {"nodeType":"ACTION","nodeId":"p1_biz","outputs":{"team":"Engineering","slaHours":"2","escalationLevel":"Manager","autoResponse":"false"}}
            }
          ],
          "defaultBranch": {"nodeType":"ACTION","nodeId":"p1_free","outputs":{"team":"Engineering","slaHours":"4","escalationLevel":"Lead","autoResponse":"true"}}
        }
      },
      {
        "value": "P2",
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "p2_cat",
          "field": "issueCategory",
          "operator": "EQ",
          "branches": [
            {
              "value": "Billing",
              "child": {"nodeType":"ACTION","nodeId":"p2_bill","outputs":{"team":"Finance Support","slaHours":"8","escalationLevel":"Manager","autoResponse":"false"}}
            },
            {
              "value": "Technical",
              "child": {"nodeType":"ACTION","nodeId":"p2_tech","outputs":{"team":"Technical Support","slaHours":"8","escalationLevel":"Lead","autoResponse":"false"}}
            }
          ],
          "defaultBranch": {"nodeType":"ACTION","nodeId":"p2_other","outputs":{"team":"General Support","slaHours":"12","escalationLevel":"Lead","autoResponse":"true"}}
        }
      },
      {
        "value": "P3",
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "p3_tier",
          "field": "customerTier",
          "operator": "EQ",
          "branches": [
            {
              "value": "Enterprise",
              "child": {"nodeType":"ACTION","nodeId":"p3_ent","outputs":{"team":"Account Support","slaHours":"24","escalationLevel":"Lead","autoResponse":"false"}}
            }
          ],
          "defaultBranch": {"nodeType":"ACTION","nodeId":"p3_def","outputs":{"team":"General Support","slaHours":"48","escalationLevel":"None","autoResponse":"true"}}
        }
      }
    ],
    "defaultBranch": {"nodeType":"ACTION","nodeId":"p4_def","outputs":{"team":"Self-Service","slaHours":"72","escalationLevel":"None","autoResponse":"true"}}
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"priority":"STRING","customerTier":"STRING","issueCategory":"STRING"}',
     '{"team":"STRING","slaHours":"INTEGER","escalationLevel":"STRING","autoResponse":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 3-level routing tree with priority, tier, and category')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 3. SCORECARD — Vendor Risk Assessment
-- Use case: Procurement scores vendor risk across 5 dimensions
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000003',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Vendor Risk Assessment', 'vendor-risk', 'SCORECARD',
     'Scores vendor risk across financial stability, compliance, delivery, security, and business maturity for supplier selection decisions',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000003',
     'e1000000-0000-0000-0000-000000000003', 1,
     '{
  "baseScore": 0,
  "strategy": "WEIGHTED_AVERAGE",
  "minimumScore": 0,
  "maximumScore": 100,
  "categories": [
    {
      "name": "Financial Stability",
      "weight": 0.25,
      "attributes": [{
        "name": "financialScore",
        "factPath": "financialScore",
        "defaultPoints": 0,
        "ranges": [
          {"min":90,"max":100,"points":100,"label":"Excellent"},
          {"min":70,"max":89,"points":75,"label":"Good"},
          {"min":50,"max":69,"points":40,"label":"Adequate"},
          {"min":0,"max":49,"points":10,"label":"Poor"}
        ]
      }]
    },
    {
      "name": "Compliance",
      "weight": 0.20,
      "attributes": [{
        "name": "complianceCerts",
        "factPath": "complianceCerts",
        "defaultPoints": 0,
        "ranges": [
          {"min":4,"max":10,"points":100,"label":"Fully Certified"},
          {"min":2,"max":3,"points":70,"label":"Mostly Certified"},
          {"min":1,"max":1,"points":40,"label":"Minimally Certified","minInclusive":true,"maxInclusive":true},
          {"min":0,"max":0,"points":5,"label":"No Certifications","minInclusive":true,"maxInclusive":true}
        ]
      }]
    },
    {
      "name": "Delivery Performance",
      "weight": 0.25,
      "attributes": [{
        "name": "deliveryOnTime",
        "factPath": "deliveryOnTime",
        "defaultPoints": 0,
        "ranges": [
          {"min":95,"max":100,"points":100,"label":"Excellent"},
          {"min":85,"max":94,"points":70,"label":"Good"},
          {"min":70,"max":84,"points":40,"label":"Fair"},
          {"min":0,"max":69,"points":10,"label":"Poor"}
        ]
      }]
    },
    {
      "name": "Security",
      "weight": 0.20,
      "attributes": [{
        "name": "securityAuditScore",
        "factPath": "securityAuditScore",
        "defaultPoints": 0,
        "ranges": [
          {"min":90,"max":100,"points":100,"label":"Excellent"},
          {"min":75,"max":89,"points":70,"label":"Good"},
          {"min":60,"max":74,"points":40,"label":"Acceptable"},
          {"min":0,"max":59,"points":10,"label":"Risky"}
        ]
      }]
    },
    {
      "name": "Business Maturity",
      "weight": 0.10,
      "attributes": [{
        "name": "yearsInBusiness",
        "factPath": "yearsInBusiness",
        "defaultPoints": 0,
        "ranges": [
          {"min":10,"max":100,"points":100,"label":"Established"},
          {"min":5,"max":9,"points":70,"label":"Mature"},
          {"min":2,"max":4,"points":40,"label":"Growing"},
          {"min":0,"max":1,"points":15,"label":"Startup"}
        ]
      }]
    }
  ]
}',
     '{"financialScore":"DECIMAL","complianceCerts":"INTEGER","deliveryOnTime":"DECIMAL","securityAuditScore":"DECIMAL","yearsInBusiness":"INTEGER"}',
     '{"score":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 5-category weighted vendor risk scorecard')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 4. SCRIPT — Tax Calculation Engine
-- Use case: Progressive income tax with brackets, deductions, and credits
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000004',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Tax Calculation Engine', 'tax-calculator', 'SCRIPT',
     'Calculates progressive federal income tax with standard deductions, dependent credits, and state tax estimates',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000004',
     'e1000000-0000-0000-0000-000000000004', 1,
     '{"language":"javascript","scriptBody":"function execute(input) {\n  var gross = input.grossIncome || 0;\n  var filing = input.filingStatus || ''single'';\n  var deps = input.dependents || 0;\n  var itemized = input.deductions || 0;\n  var state = input.stateCode || ''CA'';\n\n  // Standard deductions by filing status\n  var stdDeduction = { single: 14600, married: 29200, head_of_household: 21900 };\n  var deduction = Math.max(itemized, stdDeduction[filing] || 14600);\n  var depCredit = deps * 2000;\n\n  // Taxable income\n  var taxable = Math.max(0, gross - deduction);\n\n  // Federal progressive brackets (2024)\n  var brackets = [\n    { limit: 11600, rate: 0.10 },\n    { limit: 47150, rate: 0.12 },\n    { limit: 100525, rate: 0.22 },\n    { limit: 191950, rate: 0.24 },\n    { limit: 243725, rate: 0.32 },\n    { limit: 609350, rate: 0.35 },\n    { limit: Infinity, rate: 0.37 }\n  ];\n\n  var federal = 0;\n  var prev = 0;\n  var bracketName = ''10%'';\n  for (var i = 0; i < brackets.length; i++) {\n    var b = brackets[i];\n    if (taxable <= prev) break;\n    var amt = Math.min(taxable, b.limit) - prev;\n    federal += amt * b.rate;\n    if (taxable <= b.limit) { bracketName = (b.rate * 100) + ''%''; break; }\n    prev = b.limit;\n  }\n\n  federal = Math.max(0, federal - depCredit);\n\n  // State tax (simplified flat rates)\n  var stateRates = { CA: 0.093, NY: 0.0685, TX: 0, FL: 0, WA: 0, IL: 0.0495 };\n  var stateRate = stateRates[state] !== undefined ? stateRates[state] : 0.05;\n  var stateTax = Math.round(taxable * stateRate * 100) / 100;\n\n  var totalTax = federal + stateTax;\n  var effectiveRate = gross > 0 ? Math.round((totalTax / gross) * 10000) / 100 : 0;\n\n  return {\n    federalTax: Math.round(federal * 100) / 100,\n    stateTax: stateTax,\n    effectiveRate: effectiveRate,\n    taxBracket: bracketName,\n    totalDeductions: deduction\n  };\n}"}',
     '{"grossIncome":"DECIMAL","filingStatus":"STRING","dependents":"INTEGER","deductions":"DECIMAL","stateCode":"STRING"}',
     '{"federalTax":"DECIMAL","stateTax":"DECIMAL","effectiveRate":"DECIMAL","taxBracket":"STRING","totalDeductions":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 40-line progressive tax calculator with 7 brackets, deductions, and state tax')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 5. FLOW — Loan Application Pipeline
-- Use case: Multi-step loan review with credit check, debt ratio, and manual review
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000005',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Loan Application Pipeline', 'loan-pipeline', 'FLOW',
     'Multi-step loan application review with credit scoring, debt ratio analysis, and approval/rejection paths',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000005',
     'e1000000-0000-0000-0000-000000000005', 1,
     '{
  "startNodeId": "start",
  "maxIterations": 50,
  "nodes": [
    {"id":"start","type":"ACTION","outputs":{"stage":"validation"}},
    {"id":"checkCredit","type":"CONDITION","expression":"creditScore >= 650"},
    {"id":"checkDebt","type":"CONDITION","expression":"existingDebt / annualIncome < 0.4"},
    {"id":"checkAmount","type":"CONDITION","expression":"requestedAmount <= annualIncome * 5"},
    {"id":"borderline","type":"CONDITION","expression":"creditScore >= 600"},
    {"id":"approve","type":"ACTION","outputs":{"decision":"APPROVED","interestRate":4.5,"conditions":"Standard terms"}},
    {"id":"conditionalApprove","type":"ACTION","outputs":{"decision":"CONDITIONALLY_APPROVED","interestRate":6.9,"conditions":"Requires additional documentation and co-signer"}},
    {"id":"manualReview","type":"ACTION","outputs":{"decision":"MANUAL_REVIEW","interestRate":0,"conditions":"Referred to senior underwriter for borderline credit profile"}},
    {"id":"rejectDebt","type":"ACTION","outputs":{"decision":"REJECTED","interestRate":0,"conditions":"Debt-to-income ratio exceeds 40% threshold"}},
    {"id":"rejectCredit","type":"ACTION","outputs":{"decision":"REJECTED","interestRate":0,"conditions":"Credit score below minimum requirement"}}
  ],
  "edges": [
    {"from":"start","to":"checkCredit","condition":"true"},
    {"from":"checkCredit","to":"checkDebt","condition":"true"},
    {"from":"checkCredit","to":"borderline","condition":"false"},
    {"from":"checkDebt","to":"checkAmount","condition":"true"},
    {"from":"checkDebt","to":"rejectDebt","condition":"false"},
    {"from":"checkAmount","to":"approve","condition":"true"},
    {"from":"checkAmount","to":"conditionalApprove","condition":"false"},
    {"from":"borderline","to":"manualReview","condition":"true"},
    {"from":"borderline","to":"rejectCredit","condition":"false"}
  ]
}',
     '{"creditScore":"INTEGER","annualIncome":"DECIMAL","requestedAmount":"DECIMAL","existingDebt":"DECIMAL"}',
     '{"decision":"STRING","interestRate":"DECIMAL","conditions":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 10-node pipeline with credit, debt, and amount checks plus manual review path')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 6. DMN — Shipping Rate Calculator
-- Use case: E-commerce shipping cost based on weight, zone, and priority
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000006',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Shipping Rate Calculator', 'shipping-rates', 'DMN',
     'Calculates shipping rates, carrier selection, and delivery estimates based on package weight, destination zone, and priority level',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000006',
     'e1000000-0000-0000-0000-000000000006', 1,
     '{
  "decisions": [{
    "id": "shipping-calc",
    "name": "Shipping Rate Calculator",
    "decisionTable": {
      "hitPolicy": "FIRST",
      "inputClauses": [
        {"name":"weightKg","expression":"weightKg"},
        {"name":"zone","expression":"zone"},
        {"name":"priority","expression":"priority"}
      ],
      "outputClauses": [
        {"name":"shippingRate"},
        {"name":"carrier"},
        {"name":"estimatedDays"}
      ],
      "rules": [
        {"ruleNumber":1,"inputEntries":["<= 2","domestic","overnight"],"outputEntries":["29.99","Express Air","1"]},
        {"ruleNumber":2,"inputEntries":["<= 2","domestic","express"],"outputEntries":["14.99","Priority Mail","2"]},
        {"ruleNumber":3,"inputEntries":["<= 2","domestic","standard"],"outputEntries":["5.99","Ground","5"]},
        {"ruleNumber":4,"inputEntries":["<= 10","domestic","overnight"],"outputEntries":["49.99","Express Air","1"]},
        {"ruleNumber":5,"inputEntries":["<= 10","domestic","express"],"outputEntries":["24.99","Priority Mail","3"]},
        {"ruleNumber":6,"inputEntries":["<= 10","domestic","standard"],"outputEntries":["12.99","Ground","7"]},
        {"ruleNumber":7,"inputEntries":["> 10","domestic","overnight"],"outputEntries":["89.99","Freight Express","2"]},
        {"ruleNumber":8,"inputEntries":["> 10","domestic","standard"],"outputEntries":["29.99","Freight Ground","10"]},
        {"ruleNumber":9,"inputEntries":["<= 5","international","express"],"outputEntries":["59.99","International Air","5"]},
        {"ruleNumber":10,"inputEntries":["<= 5","international","standard"],"outputEntries":["29.99","International Post","14"]},
        {"ruleNumber":11,"inputEntries":["> 5","international","express"],"outputEntries":["99.99","International Air","7"]},
        {"ruleNumber":12,"inputEntries":["> 5","international","standard"],"outputEntries":["54.99","Ocean Freight","21"]}
      ]
    }
  }]
}',
     '{"weightKg":"DECIMAL","zone":"STRING","priority":"STRING"}',
     '{"shippingRate":"DECIMAL","carrier":"STRING","estimatedDays":"INTEGER"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 12 rules covering domestic/international × weight × priority')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 7. ENSEMBLE — Customer Churn Prediction
-- Use case: Combines credit scoring and risk assessment for churn signal
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000007',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Customer Churn Prediction', 'churn-prediction', 'ENSEMBLE',
     'Predicts customer churn risk by combining weighted signals from credit scoring, insurance risk, and loan eligibility models',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000007',
     'e1000000-0000-0000-0000-000000000007', 1,
     '{"strategy":"WEIGHTED_AVERAGE","outputField":"score","members":[{"ruleId":"e0000000-0000-0000-0000-000000000003","alias":"credit-signal","weight":0.4,"version":0},{"ruleId":"e0000000-0000-0000-0000-000000000001","alias":"eligibility-signal","weight":0.35,"version":0},{"ruleId":"e0000000-0000-0000-0000-000000000002","alias":"risk-signal","weight":0.25,"version":0}]}',
     '{"paymentHistory":"DECIMAL","debtToIncome":"DECIMAL","accountAge":"INTEGER","creditMix":"INTEGER","creditScore":"INTEGER","income":"INTEGER","age":"INTEGER","accidents":"INTEGER"}',
     '{"churnRisk":"DECIMAL"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Initial version — 3-member ensemble combining credit, eligibility, and risk signals')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- 8. CEP — Login Anomaly Detector
-- Use case: Detect brute force and geo-hopping login patterns
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000008',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Login Anomaly Detector', 'login-anomaly', 'CEP',
     'Detects suspicious login patterns including brute force attempts and rapid geographic location changes',
     'DRAFT', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f1000000-0000-0000-0000-000000000008',
     'e1000000-0000-0000-0000-000000000008', 1,
     '{
  "patternType": "SEQUENCE",
  "eventSchemas": [
    {
      "eventType": "loginAttempt",
      "fields": {
        "userId": "string",
        "ip": "string",
        "country": "string",
        "success": "boolean"
      },
      "filterConditions": [
        {"field":"success","operator":"EQ","value":"false"}
      ]
    },
    {
      "eventType": "locationChange",
      "fields": {
        "userId": "string",
        "fromCountry": "string",
        "toCountry": "string",
        "timeDeltaSeconds": "number"
      },
      "filterConditions": [
        {"field":"timeDeltaSeconds","operator":"LT","value":"3600"}
      ]
    }
  ],
  "timeWindow": {
    "type": "SLIDING",
    "duration": 600,
    "graceTime": 30
  },
  "actions": [
    {"actionType":"EMIT_EVENT","parameters":{"topic":"security-alerts","severity":"HIGH","action":"lockAccount"}}
  ],
  "inputTopic": "login-events",
  "outputTopic": "security-alerts"
}',
     '{"userId":"STRING","ip":"STRING","country":"STRING","success":"BOOLEAN"}',
     '{"alertType":"STRING","severity":"STRING"}',
     'DRAFT', 'c0000000-0000-0000-0000-000000000001', 'Initial draft — SEQUENCE pattern for brute force + geo-hopping detection')
ON CONFLICT (rule_id, version) DO NOTHING;
