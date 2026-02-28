-- ============================================================================
-- V035: Seed approval requests for testing the Approvals page
--
-- Creates 2 approval requests referencing existing seeded rules:
--   1. Loan Eligibility (e0000000-...-01) — submitted by editor, assigned to admin
--   2. Insurance Risk (e0000000-...-02) — submitted by admin, assigned to admin
--
-- Known IDs (from V021):
--   Org:    a0000000-0000-0000-0000-000000000001 (Acme Corp)
--   Space:  d0000000-0000-0000-0000-000000000001
--   Admin:  b0000000-0000-0000-0000-000000000001 (OWNER)
--   Editor: b0000000-0000-0000-0000-000000000002 (EDITOR)
-- ============================================================================

-- Request 1: Editor submits Loan Eligibility v1 for approval, assigned to Admin
-- This appears in Admin's "My Queue"
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    1,
    'b0000000-0000-0000-0000-000000000002',
    'PENDING',
    'Please review the loan eligibility thresholds before go-live.',
    NOW() + INTERVAL '48 hours'
)
ON CONFLICT (rule_id, version) DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;

-- Request 2: Admin submits Insurance Risk v1 for approval, assigned to self
-- This appears in Admin's "My Requests"
INSERT INTO approval_requests (id, rule_id, version, requested_by, status, note, deadline)
VALUES (
    'f0000000-0000-0000-0000-000000000002',
    'e0000000-0000-0000-0000-000000000002',
    1,
    'b0000000-0000-0000-0000-000000000001',
    'PENDING',
    'Self-review: verify risk categories align with actuarial tables.',
    NOW() + INTERVAL '48 hours'
)
ON CONFLICT (rule_id, version) DO NOTHING;

INSERT INTO approval_request_assignees (id, request_id, assignee_id, status)
VALUES (
    'f1000000-0000-0000-0000-000000000002',
    'f0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000001',
    'PENDING'
)
ON CONFLICT (request_id, assignee_id) DO NOTHING;
