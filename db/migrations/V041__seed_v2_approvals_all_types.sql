-- ============================================================================
-- V041: Seed v2 rule versions with PENDING_APPROVAL for all remaining rule types
--
-- Creates version 2 for the 6 rules seeded in V022 (rules 003-008) and places
-- each under a PENDING approval request so the Approvals UI has realistic data
-- across SCORECARD, SCRIPT, FLOW, DMN, ENSEMBLE, and CEP rule types.
--
-- Prerequisites:
--   V022 — rules e0000000-...-03 through -08 exist (v1)
--   V040 — rules 003-008 set to PUBLISHED
--
-- Known IDs (from V021 seed):
--   Org:           a0000000-0000-0000-0000-000000000001  (Acme Corp)
--   Space:         d0000000-0000-0000-0000-000000000001
--   Admin user:    b0000000-0000-0000-0000-000000000001  (OWNER)
--   Editor user:   b0000000-0000-0000-0000-000000000002  (EDITOR)
--   Admin member:  c0000000-0000-0000-0000-000000000001
--   Editor member: c0000000-0000-0000-0000-000000000002
--
-- New record IDs:
--   rule_version  SCORECARD v2:  f0000000-0000-0000-0000-000000000020
--   rule_version  SCRIPT v2:     f0000000-0000-0000-0000-000000000021
--   rule_version  FLOW v2:       f0000000-0000-0000-0000-000000000022
--   rule_version  DMN v2:        f0000000-0000-0000-0000-000000000023
--   rule_version  ENSEMBLE v2:   f0000000-0000-0000-0000-000000000024
--   rule_version  CEP v2:        f0000000-0000-0000-0000-000000000025
--   approval_req  SCORECARD:     f0000000-0000-0000-0000-000000000040
--   approval_req  SCRIPT:        f0000000-0000-0000-0000-000000000041
--   approval_req  FLOW:          f0000000-0000-0000-0000-000000000042
--   approval_req  DMN:           f0000000-0000-0000-0000-000000000043
--   approval_req  ENSEMBLE:      f0000000-0000-0000-0000-000000000044
--   approval_req  CEP:           f0000000-0000-0000-0000-000000000045
--   assignee      SCORECARD:     f1000000-0000-0000-0000-000000000040
--   assignee      SCRIPT:        f1000000-0000-0000-0000-000000000041
--   assignee      FLOW:          f1000000-0000-0000-0000-000000000042
--   assignee      DMN:           f1000000-0000-0000-0000-000000000043
--   assignee      ENSEMBLE:      f1000000-0000-0000-0000-000000000044
--   assignee      CEP:           f1000000-0000-0000-0000-000000000045
-- ============================================================================


-- ============================================================================
-- 1. SCORECARD v2 — Customer Credit Scoring (rule 003)
--
-- Changes from v1:
--   - Added "Credit Score" category with creditScore factPath and bins:
--     EXCELLENT (750-850, 50pts), GOOD (650-749, 30pts), FAIR (500-649, 10pts)
--   - Adjusted existing "Payment History" bin thresholds down by 10 points:
--     Excellent 95→85, Good 80→70, Fair 60→50
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000020',
    'e0000000-0000-0000-0000-000000000003',
    2,
    '{"baseScore":300,"strategy":"ADDITIVE","categories":[{"name":"Payment History","weight":0.30,"attributes":[{"name":"paymentHistory","factPath":"paymentHistory","defaultPoints":0,"ranges":[{"min":85,"max":100,"points":100,"label":"Excellent"},{"min":70,"max":84,"points":70,"label":"Good"},{"min":50,"max":69,"points":40,"label":"Fair"},{"min":0,"max":49,"points":10,"label":"Poor"}]}]},{"name":"Credit Score","weight":0.25,"attributes":[{"name":"creditScore","factPath":"creditScore","defaultPoints":0,"ranges":[{"min":750,"max":850,"points":50,"label":"Excellent"},{"min":650,"max":749,"points":30,"label":"Good"},{"min":500,"max":649,"points":10,"label":"Fair"}]}]},{"name":"Debt Ratio","weight":0.20,"attributes":[{"name":"debtToIncome","factPath":"debtToIncome","defaultPoints":0,"ranges":[{"min":0,"max":20,"points":100,"label":"Low"},{"min":21,"max":40,"points":60,"label":"Medium"},{"min":41,"max":100,"points":20,"label":"High"}]}]},{"name":"Account Age","weight":0.10,"attributes":[{"name":"accountAge","factPath":"accountAge","defaultPoints":0,"ranges":[{"min":10,"max":50,"points":100,"label":"Long"},{"min":3,"max":9,"points":60,"label":"Medium"},{"min":0,"max":2,"points":20,"label":"Short"}]}]},{"name":"Credit Mix","weight":0.15,"attributes":[{"name":"creditMix","factPath":"creditMix","defaultPoints":0,"ranges":[{"min":3,"max":10,"points":100,"label":"Diverse"},{"min":1,"max":2,"points":50,"label":"Limited"},{"min":0,"max":0,"points":10,"label":"Single","minInclusive":true,"maxInclusive":true}]}]}]}',
    '{"paymentHistory":"DECIMAL","creditScore":"INTEGER","debtToIncome":"DECIMAL","accountAge":"INTEGER","creditMix":"INTEGER"}',
    '{"score":"DECIMAL"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Added payment history scoring factor per credit risk committee (CRC-2026-01)'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 2. SCRIPT v2 — Dynamic Pricing Engine (rule 004)
--
-- Changes from v1:
--   - Added seasonal multiplier: Dec/Jan = 1.15, else 1.0
--   - Added loyalty discount tier: loyaltyYears >= 5 gets extra 5% off
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000021',
    'e0000000-0000-0000-0000-000000000004',
    2,
    '{"language":"js","scriptBody":"function evaluate(input) { var basePrice = input.basePrice; var factor = Math.min(2.0, Math.max(0.5, input.demandIndex / 100)); var month = input.month || 1; var seasonalMultiplier = (month === 12 || month === 1) ? 1.15 : 1.0; var loyaltyYears = input.loyaltyYears || 0; var loyaltyDiscount = loyaltyYears >= 5 ? 0.95 : 1.0; var finalPrice = basePrice * factor * seasonalMultiplier * loyaltyDiscount; return { finalPrice: Math.round(finalPrice * 100) / 100, seasonalMultiplier: seasonalMultiplier, loyaltyDiscount: loyaltyDiscount }; }"}',
    '{"basePrice":"DECIMAL","demandIndex":"INTEGER","month":"INTEGER","loyaltyYears":"INTEGER"}',
    '{"finalPrice":"DECIMAL","seasonalMultiplier":"DECIMAL","loyaltyDiscount":"DECIMAL"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Q1 2026 pricing update: seasonal multiplier + loyalty tiers (PM-2026-08)'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 3. FLOW v2 — Order Processing Pipeline (rule 005)
--
-- Changes from v1:
--   - Added "fraudCheck" condition node between "check" and "approve"
--   - If fraudScore > 0.8 → MANUAL_REVIEW, else continue to APPROVED
--   - New edges for the fraud check path
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000022',
    'e0000000-0000-0000-0000-000000000005',
    2,
    '{"nodes":[{"id":"start","type":"START","next":"check"},{"id":"check","type":"CONDITION","field":"orderTotal","operator":"GT","value":0,"trueNext":"fraudCheck","falseNext":"reject"},{"id":"fraudCheck","type":"CONDITION","field":"fraudScore","operator":"GT","value":0.8,"trueNext":"manualReview","falseNext":"approve"},{"id":"approve","type":"END","output":{"status":"APPROVED"}},{"id":"reject","type":"END","output":{"status":"REJECTED"}},{"id":"manualReview","type":"END","output":{"status":"MANUAL_REVIEW"}}],"edges":[]}',
    '{"orderTotal":"DECIMAL","fraudScore":"DECIMAL"}',
    '{"status":"STRING"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Added fraud pre-check gate per compliance requirement COMP-2026-12'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 4. DMN v2 — Vacation Days Calculator (rule 006)
--
-- Changes from v1:
--   - Added "remote" input clause (boolean)
--   - Remote workers get +3 vacation days across all seniority levels
--   - Updated existing rules with remote=false, added remote=true variants
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000023',
    'e0000000-0000-0000-0000-000000000006',
    2,
    '{"decisions":[{"id":"vacation-days","name":"Vacation Days","decisionTable":{"hitPolicy":"FIRST","inputClauses":[{"name":"yearsOfService","expression":"yearsOfService"},{"name":"role","expression":"role"},{"name":"remote","expression":"remote"}],"outputClauses":[{"name":"vacationDays"}],"rules":[{"ruleNumber":1,"inputEntries":[">=0","Junior","true"],"outputEntries":["18"]},{"ruleNumber":2,"inputEntries":[">=0","Junior","false"],"outputEntries":["15"]},{"ruleNumber":3,"inputEntries":[">=0","Senior","true"],"outputEntries":["23"]},{"ruleNumber":4,"inputEntries":[">=0","Senior","false"],"outputEntries":["20"]},{"ruleNumber":5,"inputEntries":[">=5","Junior","true"],"outputEntries":["28"]},{"ruleNumber":6,"inputEntries":[">=5","Junior","false"],"outputEntries":["25"]},{"ruleNumber":7,"inputEntries":[">=5","Senior","true"],"outputEntries":["31"]},{"ruleNumber":8,"inputEntries":[">=5","Senior","false"],"outputEntries":["28"]},{"ruleNumber":9,"inputEntries":[">=5","Manager","true"],"outputEntries":["33"]},{"ruleNumber":10,"inputEntries":[">=5","Manager","false"],"outputEntries":["30"]}]}}]}',
    '{"yearsOfService":"INTEGER","role":"STRING","remote":"BOOLEAN"}',
    '{"vacationDays":"INTEGER"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Remote work policy update: +3 days for remote employees (HR-2026-03)'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 5. ENSEMBLE v2 — Fraud Detection Ensemble (rule 007)
--
-- Changes from v1:
--   - Changed strategy from WEIGHTED_AVERAGE to MAJORITY_VOTE
--   - Added third member "rules-engine" (rule e0000000-...-001)
--   - Adjusted weights to 0.4 / 0.4 / 0.2
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000024',
    'e0000000-0000-0000-0000-000000000007',
    2,
    '{"strategy":"MAJORITY_VOTE","outputField":"fraudScore","members":[{"ruleId":"e0000000-0000-0000-0000-000000000001","alias":"loan-check","weight":0.4},{"ruleId":"e0000000-0000-0000-0000-000000000002","alias":"risk-check","weight":0.4},{"ruleId":"e0000000-0000-0000-0000-000000000001","alias":"rules-engine","weight":0.2}]}',
    '{"amount":"DECIMAL","country":"STRING"}',
    '{"fraudScore":"DECIMAL"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Switched to majority vote for better consensus; added rules-engine member'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 6. CEP v2 — Transaction Velocity Monitor (rule 008)
--
-- Changes from v1:
--   - Changed timeWindow duration from 300s to 600s
--   - Added second filter condition: country NOT_EQUALS "US"
--   - Changed threshold to 3 (via additional filterCondition)
--   - Changed outputTopic from "alerts" to "high-risk-alerts"
-- ============================================================================
INSERT INTO rule_versions (
    id, rule_id, version, definition, input_model, output_model,
    status, created_by, change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000025',
    'e0000000-0000-0000-0000-000000000008',
    2,
    '{"patternType":"FREQUENCY","eventSchemas":[{"eventType":"transaction","fields":{"amount":"number","country":"string"},"filterConditions":[{"field":"amount","operator":"GT","value":"100"},{"field":"country","operator":"NOT_EQUALS","value":"US"}]}],"timeWindow":{"type":"TUMBLING","duration":600,"graceTime":0},"threshold":3,"actions":[{"actionType":"EMIT_EVENT","parameters":{}}],"inputTopic":"transactions","outputTopic":"high-risk-alerts"}',
    '{"transactionId":"STRING","amount":"DECIMAL","country":"STRING"}',
    '{"alertType":"STRING"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Extended monitoring window to 10 min; tightened geo filter per security review'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ============================================================================
-- 7. Advance rules.latest_version to 2 for all 6 rules
--    Guard with WHERE latest_version = 1 so a re-run is a no-op.
-- ============================================================================
UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000003' AND latest_version = 1;

UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000004' AND latest_version = 1;

UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000005' AND latest_version = 1;

UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000006' AND latest_version = 1;

UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000007' AND latest_version = 1;

UPDATE rules SET latest_version = 2, updated_at = NOW()
WHERE id = 'e0000000-0000-0000-0000-000000000008' AND latest_version = 1;


-- ============================================================================
-- 8. Create PENDING approval requests
--    Editor (b0000000-...-0002) submits; Admin (b0000000-...-0001) reviews.
-- ============================================================================

-- SCORECARD approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000040',
    'e0000000-0000-0000-0000-000000000003',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Added payment history scoring factor per credit risk committee recommendation. Please review new category weights and bin thresholds.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000040',
    'f0000000-0000-0000-0000-000000000040',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- SCRIPT approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000041',
    'e0000000-0000-0000-0000-000000000004',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Q1 2026 pricing update: added seasonal multiplier (Dec/Jan +15%) and loyalty discount tiers (5+ years = 5% off). Verify pricing logic before peak season.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000041',
    'f0000000-0000-0000-0000-000000000041',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- FLOW approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000042',
    'e0000000-0000-0000-0000-000000000005',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Added fraud pre-check gate per compliance requirement COMP-2026-12. Orders with fraudScore > 0.8 are routed to MANUAL_REVIEW instead of auto-approval.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000042',
    'f0000000-0000-0000-0000-000000000042',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- DMN approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000043',
    'e0000000-0000-0000-0000-000000000006',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Remote work policy update per HR-2026-03: remote employees receive +3 vacation days across all seniority levels. Please verify the new decision table rows.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000043',
    'f0000000-0000-0000-0000-000000000043',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- ENSEMBLE approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000044',
    'e0000000-0000-0000-0000-000000000007',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Switched fraud detection from weighted average to majority vote for better consensus. Added rules-engine as third member. Review weight distribution (0.4/0.4/0.2).',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000044',
    'f0000000-0000-0000-0000-000000000044',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- CEP approval
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000045',
    'e0000000-0000-0000-0000-000000000008',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Extended monitoring window from 5 to 10 minutes. Added geo filter (exclude US transactions). Tightened threshold to 3. Output redirected to high-risk-alerts topic.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000045',
    'f0000000-0000-0000-0000-000000000045',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;
