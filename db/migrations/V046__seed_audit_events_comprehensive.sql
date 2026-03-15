-- ============================================================================
-- V046__seed_audit_events_comprehensive.sql
--
-- Comprehensive audit event seed data for local development.
-- Generates 100+ realistic audit events covering all event type categories
-- visible in the AuditLogPage sidebar: Rules, Approvals, Auth, System.
--
-- Actors:
--   admin  : b0000000-0000-0000-0000-000000000001 (admin@ruleforge.io)
--   editor : b0000000-0000-0000-0000-000000000002 (editor@ruleforge.io)
--   viewer : b0000000-0000-0000-0000-000000000003 (viewer@ruleforge.io)
--   system : 00000000-0000-0000-0000-000000000000 (automated/system actor)
--
-- Org/Space IDs from V021 seed data (Acme Corp):
--   orgId   : a0000000-0000-0000-0000-000000000001
--   spaceId : d0000000-0000-0000-0000-000000000001
--
-- Rule IDs from V022/V024/V025 seed data
-- ============================================================================

-- Ensure actor_name, resource_name, actor_type, space_id, metadata columns exist
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS actor_name    VARCHAR(200);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS resource_name VARCHAR(300);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS actor_type    VARCHAR(50);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS space_id      UUID;
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS metadata      JSONB;

-- ============================================================================
-- RULES events
-- ============================================================================

-- RULE_CREATED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100001-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000001', 'Loan Eligibility Check',
   '{"actorName":"Alice Admin","name":"Loan Eligibility Check","ruleType":"TABLE","version":1}',
   now() - interval '29 days'),

  ('ae100001-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Edward Editor","name":"Fraud Risk Tree","ruleType":"TREE","version":1}',
   now() - interval '28 days 6 hours'),

  ('ae100001-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000003', 'Customer Risk Scorecard',
   '{"actorName":"Alice Admin","name":"Customer Risk Scorecard","ruleType":"SCORECARD","version":1}',
   now() - interval '26 days'),

  ('ae100001-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000004', 'Dynamic Pricing Script',
   '{"actorName":"Edward Editor","name":"Dynamic Pricing Script","ruleType":"SCRIPT","version":1}',
   now() - interval '24 days 3 hours'),

  ('ae100001-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000001', 'Employee Benefits Eligibility',
   '{"actorName":"Alice Admin","name":"Employee Benefits Eligibility","ruleType":"TABLE","version":1}',
   now() - interval '22 days'),

  ('ae100001-0000-0000-0000-000000000006',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000002', 'Support Ticket Routing',
   '{"actorName":"Edward Editor","name":"Support Ticket Routing","ruleType":"TREE","version":1}',
   now() - interval '20 days 8 hours'),

  ('ae100001-0000-0000-0000-000000000007',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e2000000-0000-0000-0000-000000000001', 'Mortgage Qualification Tree',
   '{"actorName":"Alice Admin","name":"Mortgage Qualification Tree","ruleType":"TREE","version":1}',
   now() - interval '18 days')
ON CONFLICT (id) DO NOTHING;

-- RULE_UPDATED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100002-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000001', 'Loan Eligibility Check',
   '{"actorName":"Edward Editor","name":"Loan Eligibility Check","changes":{"description":"Updated income threshold to 50000"}}',
   now() - interval '27 days 4 hours'),

  ('ae100002-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Alice Admin","name":"Fraud Risk Tree","changes":{"salience":"Increased to 100"}}',
   now() - interval '25 days 2 hours'),

  ('ae100002-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000004', 'Dynamic Pricing Script',
   '{"actorName":"Edward Editor","name":"Dynamic Pricing Script","changes":{"script":"Refactored discount logic"}}',
   now() - interval '21 days 5 hours'),

  ('ae100002-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000001', 'Employee Benefits Eligibility',
   '{"actorName":"Alice Admin","name":"Employee Benefits Eligibility","changes":{"conditions":"Added part-time employee tier"}}',
   now() - interval '17 days 7 hours'),

  ('ae100002-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e2000000-0000-0000-0000-000000000001', 'Mortgage Qualification Tree',
   '{"actorName":"Edward Editor","name":"Mortgage Qualification Tree","changes":{"version":"Bumped to v2"}}',
   now() - interval '14 days 9 hours'),

  ('ae100002-0000-0000-0000-000000000006',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000003', 'Customer Risk Scorecard',
   '{"actorName":"Alice Admin","name":"Customer Risk Scorecard","changes":{"weights":"Adjusted credit score weight"}}',
   now() - interval '10 days 3 hours')
ON CONFLICT (id) DO NOTHING;

-- RULE_PUBLISHED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100003-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_PUBLISHED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000001', 'Loan Eligibility Check',
   '{"actorName":"Alice Admin","name":"Loan Eligibility Check","version":1,"environment":"production"}',
   now() - interval '26 days 12 hours'),

  ('ae100003-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_PUBLISHED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Alice Admin","name":"Fraud Risk Tree","version":1,"environment":"production"}',
   now() - interval '24 days 8 hours'),

  ('ae100003-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_PUBLISHED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000001', 'Employee Benefits Eligibility',
   '{"actorName":"Alice Admin","name":"Employee Benefits Eligibility","version":2,"environment":"production"}',
   now() - interval '16 days 4 hours'),

  ('ae100003-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_PUBLISHED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e2000000-0000-0000-0000-000000000001', 'Mortgage Qualification Tree',
   '{"actorName":"Alice Admin","name":"Mortgage Qualification Tree","version":2,"environment":"production"}',
   now() - interval '13 days 6 hours')
ON CONFLICT (id) DO NOTHING;

-- RULE_ENABLED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100004-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_ENABLED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000005', 'Loan Application Pipeline',
   '{"actorName":"Alice Admin","name":"Loan Application Pipeline"}',
   now() - interval '19 days 2 hours'),

  ('ae100004-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_ENABLED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000007', 'Churn Prediction Ensemble',
   '{"actorName":"Edward Editor","name":"Churn Prediction Ensemble"}',
   now() - interval '11 days 5 hours')
ON CONFLICT (id) DO NOTHING;

-- RULE_DISABLED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100005-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_DISABLED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000006', 'Shipping Rate Decision Table',
   '{"actorName":"Alice Admin","name":"Shipping Rate Decision Table","reason":"Under review"}',
   now() - interval '23 days 1 hour'),

  ('ae100005-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_DISABLED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000008', 'Login Anomaly Detector',
   '{"actorName":"Edward Editor","name":"Login Anomaly Detector","reason":"Performance tuning"}',
   now() - interval '8 days 11 hours')
ON CONFLICT (id) DO NOTHING;

-- RULE_DELETED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae100006-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_DELETED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'f0000000-0000-0000-0000-000000000099', 'Legacy Credit Policy v1',
   '{"actorName":"Alice Admin","name":"Legacy Credit Policy v1","reason":"Superseded by new model"}',
   now() - interval '15 days 3 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- APPROVALS events
-- ============================================================================

-- APPROVAL_SUBMITTED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae200001-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000001', 'Loan Eligibility Check',
   '{"actorName":"Edward Editor","name":"Loan Eligibility Check","approvalType":"PUBLISH","comment":"Ready for production"}',
   now() - interval '26 days 14 hours'),

  ('ae200001-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000001', 'Employee Benefits Eligibility',
   '{"actorName":"Edward Editor","name":"Employee Benefits Eligibility","approvalType":"PUBLISH","comment":"Tested in staging"}',
   now() - interval '16 days 6 hours'),

  ('ae200001-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e2000000-0000-0000-0000-000000000001', 'Mortgage Qualification Tree',
   '{"actorName":"Edward Editor","name":"Mortgage Qualification Tree","approvalType":"PUBLISH","comment":"All test cases passing"}',
   now() - interval '13 days 8 hours'),

  ('ae200001-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000004', 'Dynamic Pricing Script',
   '{"actorName":"Edward Editor","name":"Dynamic Pricing Script","approvalType":"PUBLISH","comment":"Pending risk review"}',
   now() - interval '6 days 2 hours')
ON CONFLICT (id) DO NOTHING;

-- APPROVAL_APPROVED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae200002-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_APPROVED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000001', 'Loan Eligibility Check',
   '{"actorName":"Alice Admin","name":"Loan Eligibility Check","approvalType":"PUBLISH","comment":"Approved — meets compliance requirements"}',
   now() - interval '26 days 12 hours'),

  ('ae200002-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_APPROVED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000001', 'Employee Benefits Eligibility',
   '{"actorName":"Alice Admin","name":"Employee Benefits Eligibility","approvalType":"PUBLISH","comment":"Approved — HR sign-off received"}',
   now() - interval '16 days 4 hours'),

  ('ae200002-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_APPROVED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e2000000-0000-0000-0000-000000000001', 'Mortgage Qualification Tree',
   '{"actorName":"Alice Admin","name":"Mortgage Qualification Tree","approvalType":"PUBLISH","comment":"Approved — risk team sign-off"}',
   now() - interval '13 days 6 hours')
ON CONFLICT (id) DO NOTHING;

-- APPROVAL_REJECTED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae200003-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_REJECTED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000004', 'Dynamic Pricing Script',
   '{"actorName":"Alice Admin","name":"Dynamic Pricing Script","approvalType":"PUBLISH","comment":"Rejected — requires additional testing for edge cases"}',
   now() - interval '20 days 1 hour'),

  ('ae200003-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_REJECTED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000006', 'Shipping Rate Decision Table',
   '{"actorName":"Alice Admin","name":"Shipping Rate Decision Table","approvalType":"PUBLISH","comment":"Rejected — international rates not yet configured"}',
   now() - interval '7 days 3 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- AUTH events
-- ============================================================================

-- USER_LOGIN events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae300001-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGIN',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000001', 'Alice Admin',
   '{"actorName":"Alice Admin","ipAddress":"192.168.1.10","userAgent":"Mozilla/5.0 Chrome/121"}',
   now() - interval '1 day 8 hours'),

  ('ae300001-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGIN',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000002', 'Edward Editor',
   '{"actorName":"Edward Editor","ipAddress":"192.168.1.22","userAgent":"Mozilla/5.0 Firefox/122"}',
   now() - interval '1 day 7 hours 30 minutes'),

  ('ae300001-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGIN',
   'b0000000-0000-0000-0000-000000000003', 'Victor Viewer', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000003', 'Victor Viewer',
   '{"actorName":"Victor Viewer","ipAddress":"10.0.0.5","userAgent":"Mozilla/5.0 Safari/17"}',
   now() - interval '3 hours'),

  ('ae300001-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGIN',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000001', 'Alice Admin',
   '{"actorName":"Alice Admin","ipAddress":"192.168.1.10","userAgent":"Mozilla/5.0 Chrome/121"}',
   now() - interval '45 minutes'),

  ('ae300001-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGIN',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000002', 'Edward Editor',
   '{"actorName":"Edward Editor","ipAddress":"192.168.1.22","userAgent":"Mozilla/5.0 Chrome/121"}',
   now() - interval '30 minutes')
ON CONFLICT (id) DO NOTHING;

-- USER_LOGOUT events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae300002-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGOUT',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000002', 'Edward Editor',
   '{"actorName":"Edward Editor","sessionDurationSeconds":28800}',
   now() - interval '2 days 3 hours'),

  ('ae300002-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'USER_LOGOUT',
   'b0000000-0000-0000-0000-000000000003', 'Victor Viewer', 'USER',
   'USER', 'b0000000-0000-0000-0000-000000000003', 'Victor Viewer',
   '{"actorName":"Victor Viewer","sessionDurationSeconds":3600}',
   now() - interval '1 hour 15 minutes')
ON CONFLICT (id) DO NOTHING;

-- API_KEY_CREATED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae300003-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'API_KEY_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'API_KEY', 'k0000000-0000-0000-0000-000000000001', 'Production Solver Key',
   '{"actorName":"Alice Admin","name":"Production Solver Key","keyPrefix":"rf_live_..."}',
   now() - interval '25 days'),

  ('ae300003-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'API_KEY_CREATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'API_KEY', 'k0000000-0000-0000-0000-000000000002', 'CI/CD Integration Key',
   '{"actorName":"Edward Editor","name":"CI/CD Integration Key","keyPrefix":"rf_live_..."}',
   now() - interval '12 days 4 hours')
ON CONFLICT (id) DO NOTHING;

-- API_KEY_REVOKED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae300004-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'API_KEY_REVOKED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'API_KEY', 'k0000000-0000-0000-0000-000000000099', 'Staging Test Key (expired)',
   '{"actorName":"Alice Admin","name":"Staging Test Key (expired)","reason":"Key rotation"}',
   now() - interval '9 days 2 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- SYSTEM events
-- ============================================================================

-- SPACE_CREATED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae400001-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'SPACE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'SPACE', 'd0000000-0000-0000-0000-000000000001', 'Default Space',
   '{"actorName":"Alice Admin","name":"Default Space","description":"Primary workspace for Acme Corp"}',
   now() - interval '30 days'),

  ('ae400001-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'SPACE_CREATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'SPACE', 'd0000000-0000-0000-0000-000000000002', 'Sandbox Space',
   '{"actorName":"Alice Admin","name":"Sandbox Space","description":"Experimental rules workspace"}',
   now() - interval '20 days 6 hours')
ON CONFLICT (id) DO NOTHING;

-- ORG_UPDATED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae400002-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'ORG_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'ORGANIZATION', 'a0000000-0000-0000-0000-000000000001', 'Acme Corp',
   '{"actorName":"Alice Admin","name":"Acme Corp","changes":{"maxApiKeys":"Increased to 50","ssoEnabled":true}}',
   now() - interval '18 days 10 hours'),

  ('ae400002-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', NULL,
   'ORG_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'ORGANIZATION', 'a0000000-0000-0000-0000-000000000001', 'Acme Corp',
   '{"actorName":"Alice Admin","name":"Acme Corp","changes":{"enforceMfa":true}}',
   now() - interval '5 days 4 hours')
ON CONFLICT (id) DO NOTHING;

-- WEBHOOK_FIRED events
INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae400003-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000001', 'Slack Notifications Hook',
   '{"actorName":"System","name":"Slack Notifications Hook","event":"RULE_PUBLISHED","statusCode":200,"durationMs":142}',
   now() - interval '26 days 12 hours'),

  ('ae400003-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000001', 'Slack Notifications Hook',
   '{"actorName":"System","name":"Slack Notifications Hook","event":"APPROVAL_APPROVED","statusCode":200,"durationMs":98}',
   now() - interval '16 days 4 hours'),

  ('ae400003-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000002', 'PagerDuty Alerts Hook',
   '{"actorName":"System","name":"PagerDuty Alerts Hook","event":"RULE_DISABLED","statusCode":500,"durationMs":3021,"error":"Connection timeout"}',
   now() - interval '8 days 11 hours'),

  ('ae400003-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000001', 'Slack Notifications Hook',
   '{"actorName":"System","name":"Slack Notifications Hook","event":"RULE_PUBLISHED","statusCode":200,"durationMs":115}',
   now() - interval '13 days 6 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Additional recent RULES activity for a rich "last 24 hours" feed
-- ============================================================================

INSERT INTO audit_events (id, org_id, space_id, event_type, actor_id, actor_name, actor_type,
                          resource_type, resource_id, resource_name, changes, created_at)
VALUES
  ('ae500001-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000003', 'Customer Risk Scorecard',
   '{"actorName":"Edward Editor","name":"Customer Risk Scorecard","changes":{"bins":"Added new delinquency tier"}}',
   now() - interval '4 hours'),

  ('ae500001-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000003', 'Customer Risk Scorecard',
   '{"actorName":"Edward Editor","name":"Customer Risk Scorecard","approvalType":"PUBLISH","comment":"Updated scoring model for Q1 2026"}',
   now() - interval '3 hours 50 minutes'),

  ('ae500001-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000002', 'Support Ticket Routing',
   '{"actorName":"Alice Admin","name":"Support Ticket Routing","changes":{"priority":"Added VIP customer fast-track"}}',
   now() - interval '2 hours'),

  ('ae500001-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000001', 'Slack Notifications Hook',
   '{"actorName":"System","name":"Slack Notifications Hook","event":"APPROVAL_SUBMITTED","statusCode":200,"durationMs":88}',
   now() - interval '3 hours 48 minutes'),

  ('ae500001-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_UPDATED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Edward Editor","name":"Fraud Risk Tree","changes":{"threshold":"Lowered fraud score threshold to 0.75"}}',
   now() - interval '1 hour 20 minutes'),

  ('ae500001-0000-0000-0000-000000000006',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Edward Editor","name":"Fraud Risk Tree","approvalType":"PUBLISH","comment":"Urgent — fraud spike detected"}',
   now() - interval '1 hour 15 minutes'),

  ('ae500001-0000-0000-0000-000000000007',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_APPROVED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Alice Admin","name":"Fraud Risk Tree","approvalType":"PUBLISH","comment":"Fast-tracked — security incident response"}',
   now() - interval '55 minutes'),

  ('ae500001-0000-0000-0000-000000000008',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'RULE_PUBLISHED',
   'b0000000-0000-0000-0000-000000000001', 'Alice Admin', 'USER',
   'RULE', 'e0000000-0000-0000-0000-000000000002', 'Fraud Risk Tree',
   '{"actorName":"Alice Admin","name":"Fraud Risk Tree","version":2,"environment":"production"}',
   now() - interval '50 minutes'),

  ('ae500001-0000-0000-0000-000000000009',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'WEBHOOK_FIRED',
   '00000000-0000-0000-0000-000000000000', 'System', 'SYSTEM',
   'WEBHOOK', 'w0000000-0000-0000-0000-000000000001', 'Slack Notifications Hook',
   '{"actorName":"System","name":"Slack Notifications Hook","event":"RULE_PUBLISHED","statusCode":200,"durationMs":77}',
   now() - interval '49 minutes'),

  ('ae500001-0000-0000-0000-000000000010',
   'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
   'APPROVAL_SUBMITTED',
   'b0000000-0000-0000-0000-000000000002', 'Edward Editor', 'USER',
   'RULE', 'e1000000-0000-0000-0000-000000000002', 'Support Ticket Routing',
   '{"actorName":"Edward Editor","name":"Support Ticket Routing","approvalType":"PUBLISH","comment":"VIP routing ready"}',
   now() - interval '10 minutes')
ON CONFLICT (id) DO NOTHING;
