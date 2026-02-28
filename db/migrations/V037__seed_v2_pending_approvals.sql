-- ============================================================================
-- V037: Seed v2 rule versions with PENDING_APPROVAL status
--
-- Creates version 2 for the two primary seeded rules and places each under
-- a new PENDING approval request so the Approvals UI has realistic data to
-- display across both "My Queue" and "My Requests" views.
--
-- Prerequisites:
--   V022 — rules e0000000-...-01 and e0000000-...-02 exist (v1 PUBLISHED)
--   V023 — rule_versions.status accepts PENDING_APPROVAL
--   V035 — approval_requests for v1 already exist
--   V036 — unique constraint replaced by partial index on PENDING status only
--
-- Known IDs (from V021 seed):
--   Org:          a0000000-0000-0000-0000-000000000001  (Acme Corp)
--   Space:        d0000000-0000-0000-0000-000000000001
--   Admin user:   b0000000-0000-0000-0000-000000000001  (OWNER)
--   Editor user:  b0000000-0000-0000-0000-000000000002  (EDITOR)
--   Admin member: c0000000-0000-0000-0000-000000000001
--   Editor member:c0000000-0000-0000-0000-000000000002
--
-- New record IDs introduced here:
--   rule_version  Loan Eligibility v2:     f0000000-0000-0000-0000-000000000011
--   rule_version  Insurance Risk v2:       f0000000-0000-0000-0000-000000000012
--   approval_req  Loan Eligibility v2:     f0000000-0000-0000-0000-000000000031
--   approval_req  Insurance Risk v2:       f0000000-0000-0000-0000-000000000032
--   assignee row  for request 031:         f1000000-0000-0000-0000-000000000031
--   assignee row  for request 032:         f1000000-0000-0000-0000-000000000032
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. Create version 2 of Loan Eligibility (TABLE)
--
--    Changes from v1:
--      - Lowered credit-score thresholds (750→720, 650→620)
--      - Added income as a second condition column
--      - Introduced a new LIMITED tier for borderline applicants (580 / 20k)
--      - Added a "tier" action column (PREMIUM / STANDARD / LIMITED / NONE)
-- ----------------------------------------------------------------------------
INSERT INTO rule_versions (
    id,
    rule_id,
    version,
    definition,
    input_model,
    output_model,
    status,
    created_by,
    change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000011',
    'e0000000-0000-0000-0000-000000000001',
    2,
    '{"hitPolicy":"FIRST","columns":[{"name":"creditScore","columnType":"CONDITION","operator":"GTE","dataType":"ANY","index":0},{"name":"income","columnType":"CONDITION","operator":"GTE","dataType":"ANY","index":1},{"name":"eligible","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":2},{"name":"maxLoan","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":3},{"name":"tier","columnType":"ACTION","operator":"EQ","dataType":"ANY","index":4}],"rows":[{"rowNumber":1,"priority":0,"conditions":[{"value":720},{"value":45000}],"actions":[{"value":"true"},{"value":600000},{"value":"PREMIUM"}]},{"rowNumber":2,"priority":0,"conditions":[{"value":620},{"value":25000}],"actions":[{"value":"true"},{"value":250000},{"value":"STANDARD"}]},{"rowNumber":3,"priority":0,"conditions":[{"value":580},{"value":20000}],"actions":[{"value":"true"},{"value":75000},{"value":"LIMITED"}]},{"rowNumber":4,"priority":0,"conditions":[{"value":0},{"value":0}],"actions":[{"value":"false"},{"value":0},{"value":"NONE"}]}]}',
    '{"creditScore":"INTEGER","income":"INTEGER"}',
    '{"eligible":"STRING","maxLoan":"INTEGER","tier":"STRING"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Lowered credit score thresholds and added new PREMIUM/STANDARD/LIMITED tier classification (Q1 2026 risk model update)'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 2. Create version 2 of Insurance Risk Assessment (TREE)
--
--    Changes from v1:
--      - Added a senior driver branch for age >= 60 with separate risk buckets
--      - Increased young driver HIGH risk premium from 1.5 to 1.8
--      - Adjusted young driver MEDIUM premium from 1.2 to 1.3
-- ----------------------------------------------------------------------------
INSERT INTO rule_versions (
    id,
    rule_id,
    version,
    definition,
    input_model,
    output_model,
    status,
    created_by,
    change_note
)
VALUES (
    'f0000000-0000-0000-0000-000000000012',
    'e0000000-0000-0000-0000-000000000002',
    2,
    '{"root":{"nodeType":"CONDITION","nodeId":"n1","field":"age","operator":"LT","branches":[{"value":"25","child":{"nodeType":"CONDITION","nodeId":"n2","field":"accidents","operator":"GT","branches":[{"value":"0","child":{"nodeType":"ACTION","nodeId":"n3","outputs":{"riskCategory":"HIGH","premium":"1.8"}}},{"value":"-1","child":{"nodeType":"ACTION","nodeId":"n4","outputs":{"riskCategory":"MEDIUM","premium":"1.3"}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n4b","outputs":{"riskCategory":"MEDIUM","premium":"1.3"}}}},{"value":"60","child":{"nodeType":"CONDITION","nodeId":"n5","field":"accidents","operator":"GT","branches":[{"value":"0","child":{"nodeType":"ACTION","nodeId":"n6","outputs":{"riskCategory":"HIGH","premium":"1.6"}}},{"value":"-1","child":{"nodeType":"ACTION","nodeId":"n7","outputs":{"riskCategory":"LOW","premium":"1.1"}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n7b","outputs":{"riskCategory":"LOW","premium":"1.1"}}}},{"value":"999","child":{"nodeType":"CONDITION","nodeId":"n8","field":"accidents","operator":"GT","branches":[{"value":"2","child":{"nodeType":"ACTION","nodeId":"n9","outputs":{"riskCategory":"HIGH","premium":"1.4"}}},{"value":"-1","child":{"nodeType":"ACTION","nodeId":"n10","outputs":{"riskCategory":"LOW","premium":"1.0"}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n10b","outputs":{"riskCategory":"LOW","premium":"1.0"}}}}],"defaultBranch":{"nodeType":"ACTION","nodeId":"n11","outputs":{"riskCategory":"LOW","premium":"1.0"}}},"scoringMode":false,"earlyTermination":true}',
    '{"age":"INTEGER","accidents":"INTEGER"}',
    '{"riskCategory":"STRING","premium":"STRING"}',
    'PENDING_APPROVAL',
    'c0000000-0000-0000-0000-000000000002',
    'Added senior driver (60+) risk category with adjusted premiums; increased young driver HIGH risk premium from 1.5 to 1.8'
)
ON CONFLICT (rule_id, version) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 3. Advance rules.latest_version to 2
--    Guard with WHERE latest_version = 1 so a re-run is a no-op.
-- ----------------------------------------------------------------------------
UPDATE rules
SET    latest_version = 2,
       updated_at     = NOW()
WHERE  id             = 'e0000000-0000-0000-0000-000000000001'
  AND  latest_version = 1;

UPDATE rules
SET    latest_version = 2,
       updated_at     = NOW()
WHERE  id             = 'e0000000-0000-0000-0000-000000000002'
  AND  latest_version = 1;


-- ----------------------------------------------------------------------------
-- 4. Create PENDING approval request for Loan Eligibility v2
--    Editor (b0000000-...-0002) submits; Admin (b0000000-...-0001) is assigned.
--    The partial index (V036) only blocks a second PENDING for the same
--    rule_id + version, so this coexists with the v1 request from V035.
-- ----------------------------------------------------------------------------
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000031',
    'e0000000-0000-0000-0000-000000000001',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Q1 2026 risk model update: please verify the new PREMIUM/STANDARD/LIMITED tier thresholds before publishing.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000031',
    'f0000000-0000-0000-0000-000000000031',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 5. Create PENDING approval request for Insurance Risk Assessment v2
--    Editor (b0000000-...-0002) submits; Admin (b0000000-...-0001) is assigned.
-- ----------------------------------------------------------------------------
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000032',
    'e0000000-0000-0000-0000-000000000002',
    2,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Actuarial review requested: added senior (60+) bracket and raised young-driver HIGH premium to 1.8 per underwriting memo UW-2026-03.',
    NOW() + INTERVAL '72 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000032',
    'f0000000-0000-0000-0000-000000000032',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;
