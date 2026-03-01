-- ============================================================================
-- V042: Seed schedules and schedule_runs for local development
--
-- Adds rule_name and rule_type columns to schedules table, then inserts
-- 4 schedules with 13 historical runs each (52 total) covering various
-- statuses (SUCCESS, FAILED, CANCELLED) for realistic dashboard data.
-- ============================================================================

-- Step 1: Add denormalized columns for display convenience
ALTER TABLE schedules ADD COLUMN IF NOT EXISTS rule_name VARCHAR(255);
ALTER TABLE schedules ADD COLUMN IF NOT EXISTS rule_type VARCHAR(50);

-- Step 2: Insert 4 schedules
INSERT INTO schedules (id, space_id, rule_id, name, description, cron_expression, enabled, rule_name, rule_type, input_template, created_by, created_at, updated_at)
VALUES
  (
    'f1000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'Nightly Loan Eligibility Check',
    'Automated nightly credit eligibility screening for pending applications',
    '0 0 * * *',
    true,
    'Loan Eligibility',
    'TABLE',
    '{"creditScore":720,"income":55000}',
    'c0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '1 day'
  ),
  (
    'f1000000-0000-0000-0000-000000000002',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000002',
    'Hourly Insurance Risk Assessment',
    'Continuous risk assessment for new insurance applications',
    '0 * * * *',
    true,
    'Insurance Risk Assessment',
    'TREE',
    '{"age":35,"accidents":0}',
    'c0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '2 hours'
  ),
  (
    'f1000000-0000-0000-0000-000000000003',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000004',
    'Dynamic Pricing Recalculation',
    'Real-time price adjustments based on demand index',
    '*/5 * * * *',
    true,
    'Dynamic Pricing Engine',
    'SCRIPT',
    '{"basePrice":99.99,"demandIndex":75,"month":3,"loyaltyYears":2}',
    'c0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '5 minutes'
  ),
  (
    'f1000000-0000-0000-0000-000000000004',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000006',
    'Weekly Vacation Days Audit',
    'Weekly compliance audit of vacation day allocations',
    '0 9 * * 1',
    false,
    'Vacation Days Calculator',
    'DMN',
    '{"yearsOfService":3,"role":"Junior","remote":false}',
    'c0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '90 days',
    NOW() - INTERVAL '7 days'
  )
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Step 3: Insert schedule_runs
-- Schedule 1 — Nightly Loan Eligibility Check (13 runs over 7 days)
-- 9 SUCCESS, 3 FAILED, 1 CANCELLED
-- ============================================================================

INSERT INTO schedule_runs (id, schedule_id, status, started_at, completed_at, records_processed, error_message)
VALUES
  -- Run 01: SUCCESS
  ('f2000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '168 hours', NOW() - INTERVAL '168 hours' + INTERVAL '2300 milliseconds', 87, NULL),
  -- Run 02: SUCCESS
  ('f2000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '144 hours', NOW() - INTERVAL '144 hours' + INTERVAL '1800 milliseconds', 112, NULL),
  -- Run 03: FAILED
  ('f2000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001', 'FAILED',
   NOW() - INTERVAL '120 hours', NOW() - INTERVAL '120 hours' + INTERVAL '4500 milliseconds', 0, 'Connection timeout to solver service'),
  -- Run 04: SUCCESS
  ('f2000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '96 hours', NOW() - INTERVAL '96 hours' + INTERVAL '1500 milliseconds', 45, NULL),
  -- Run 05: SUCCESS
  ('f2000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '72 hours', NOW() - INTERVAL '72 hours' + INTERVAL '3200 milliseconds', 134, NULL),
  -- Run 06: FAILED
  ('f2000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000001', 'FAILED',
   NOW() - INTERVAL '60 hours', NOW() - INTERVAL '60 hours' + INTERVAL '3000 milliseconds', 0, 'Rule execution timeout after 30000ms'),
  -- Run 07: SUCCESS
  ('f2000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '48 hours', NOW() - INTERVAL '48 hours' + INTERVAL '1200 milliseconds', 28, NULL),
  -- Run 08: CANCELLED
  ('f2000000-0000-0000-0000-000000000008', 'f1000000-0000-0000-0000-000000000001', 'CANCELLED',
   NOW() - INTERVAL '36 hours', NOW() - INTERVAL '36 hours' + INTERVAL '500 milliseconds', 0, NULL),
  -- Run 09: SUCCESS
  ('f2000000-0000-0000-0000-000000000009', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '24 hours', NOW() - INTERVAL '24 hours' + INTERVAL '2700 milliseconds', 98, NULL),
  -- Run 10: SUCCESS
  ('f2000000-0000-0000-0000-000000000010', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours' + INTERVAL '1900 milliseconds', 67, NULL),
  -- Run 11: FAILED
  ('f2000000-0000-0000-0000-000000000011', 'f1000000-0000-0000-0000-000000000001', 'FAILED',
   NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours' + INTERVAL '800 milliseconds', 0, 'Input validation failed: creditScore must be positive'),
  -- Run 12: SUCCESS
  ('f2000000-0000-0000-0000-000000000012', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours' + INTERVAL '2100 milliseconds', 150, NULL),
  -- Run 13: SUCCESS
  ('f2000000-0000-0000-0000-000000000013', 'f1000000-0000-0000-0000-000000000001', 'SUCCESS',
   NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour' + INTERVAL '1600 milliseconds', 10, NULL)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Schedule 2 — Hourly Insurance Risk Assessment (13 runs, hourly intervals)
-- 10 SUCCESS, 2 FAILED, 1 CANCELLED
-- ============================================================================

INSERT INTO schedule_runs (id, schedule_id, status, started_at, completed_at, records_processed, error_message)
VALUES
  -- Run 14: SUCCESS
  ('f2000000-0000-0000-0000-000000000014', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '13 hours', NOW() - INTERVAL '13 hours' + INTERVAL '1400 milliseconds', 23, NULL),
  -- Run 15: SUCCESS
  ('f2000000-0000-0000-0000-000000000015', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours' + INTERVAL '2200 milliseconds', 41, NULL),
  -- Run 16: FAILED
  ('f2000000-0000-0000-0000-000000000016', 'f1000000-0000-0000-0000-000000000002', 'FAILED',
   NOW() - INTERVAL '11 hours', NOW() - INTERVAL '11 hours' + INTERVAL '4800 milliseconds', 0, 'Solver service returned 503'),
  -- Run 17: SUCCESS
  ('f2000000-0000-0000-0000-000000000017', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours' + INTERVAL '1100 milliseconds', 55, NULL),
  -- Run 18: SUCCESS
  ('f2000000-0000-0000-0000-000000000018', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '9 hours', NOW() - INTERVAL '9 hours' + INTERVAL '1700 milliseconds', 38, NULL),
  -- Run 19: SUCCESS
  ('f2000000-0000-0000-0000-000000000019', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours' + INTERVAL '2500 milliseconds', 72, NULL),
  -- Run 20: CANCELLED
  ('f2000000-0000-0000-0000-000000000020', 'f1000000-0000-0000-0000-000000000002', 'CANCELLED',
   NOW() - INTERVAL '7 hours', NOW() - INTERVAL '7 hours' + INTERVAL '300 milliseconds', 0, NULL),
  -- Run 21: SUCCESS
  ('f2000000-0000-0000-0000-000000000021', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours' + INTERVAL '1900 milliseconds', 19, NULL),
  -- Run 22: SUCCESS
  ('f2000000-0000-0000-0000-000000000022', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours' + INTERVAL '3100 milliseconds', 64, NULL),
  -- Run 23: FAILED
  ('f2000000-0000-0000-0000-000000000023', 'f1000000-0000-0000-0000-000000000002', 'FAILED',
   NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours' + INTERVAL '2000 milliseconds', 0, 'Unexpected null in field ''accidents'''),
  -- Run 24: SUCCESS
  ('f2000000-0000-0000-0000-000000000024', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours' + INTERVAL '1300 milliseconds', 47, NULL),
  -- Run 25: SUCCESS
  ('f2000000-0000-0000-0000-000000000025', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours' + INTERVAL '2800 milliseconds', 83, NULL),
  -- Run 26: SUCCESS
  ('f2000000-0000-0000-0000-000000000026', 'f1000000-0000-0000-0000-000000000002', 'SUCCESS',
   NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour' + INTERVAL '1500 milliseconds', 31, NULL)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Schedule 3 — Dynamic Pricing Recalculation (13 runs, every 5 min)
-- 8 SUCCESS, 4 FAILED, 1 CANCELLED
-- ============================================================================

INSERT INTO schedule_runs (id, schedule_id, status, started_at, completed_at, records_processed, error_message)
VALUES
  -- Run 27: SUCCESS
  ('f2000000-0000-0000-0000-000000000027', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '65 minutes', NOW() - INTERVAL '65 minutes' + INTERVAL '3400 milliseconds', 120, NULL),
  -- Run 28: FAILED
  ('f2000000-0000-0000-0000-000000000028', 'f1000000-0000-0000-0000-000000000003', 'FAILED',
   NOW() - INTERVAL '60 minutes', NOW() - INTERVAL '60 minutes' + INTERVAL '5000 milliseconds', 0, 'GraalVM script execution timeout'),
  -- Run 29: SUCCESS
  ('f2000000-0000-0000-0000-000000000029', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '55 minutes', NOW() - INTERVAL '55 minutes' + INTERVAL '2600 milliseconds', 95, NULL),
  -- Run 30: FAILED
  ('f2000000-0000-0000-0000-000000000030', 'f1000000-0000-0000-0000-000000000003', 'FAILED',
   NOW() - INTERVAL '50 minutes', NOW() - INTERVAL '50 minutes' + INTERVAL '1200 milliseconds', 0, 'Memory limit exceeded during script evaluation'),
  -- Run 31: SUCCESS
  ('f2000000-0000-0000-0000-000000000031', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '45 minutes' + INTERVAL '2900 milliseconds', 78, NULL),
  -- Run 32: SUCCESS
  ('f2000000-0000-0000-0000-000000000032', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes' + INTERVAL '1800 milliseconds', 143, NULL),
  -- Run 33: FAILED
  ('f2000000-0000-0000-0000-000000000033', 'f1000000-0000-0000-0000-000000000003', 'FAILED',
   NOW() - INTERVAL '35 minutes', NOW() - INTERVAL '35 minutes' + INTERVAL '700 milliseconds', 0, 'Invalid demand index: NaN'),
  -- Run 34: CANCELLED
  ('f2000000-0000-0000-0000-000000000034', 'f1000000-0000-0000-0000-000000000003', 'CANCELLED',
   NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes' + INTERVAL '200 milliseconds', 0, NULL),
  -- Run 35: SUCCESS
  ('f2000000-0000-0000-0000-000000000035', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '25 minutes', NOW() - INTERVAL '25 minutes' + INTERVAL '3700 milliseconds', 56, NULL),
  -- Run 36: SUCCESS
  ('f2000000-0000-0000-0000-000000000036', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes' + INTERVAL '2100 milliseconds', 102, NULL),
  -- Run 37: FAILED
  ('f2000000-0000-0000-0000-000000000037', 'f1000000-0000-0000-0000-000000000003', 'FAILED',
   NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '15 minutes' + INTERVAL '4200 milliseconds', 0, 'Solver service connection refused'),
  -- Run 38: SUCCESS
  ('f2000000-0000-0000-0000-000000000038', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes' + INTERVAL '1600 milliseconds', 88, NULL),
  -- Run 39: SUCCESS
  ('f2000000-0000-0000-0000-000000000039', 'f1000000-0000-0000-0000-000000000003', 'SUCCESS',
   NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes' + INTERVAL '2400 milliseconds', 67, NULL)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Schedule 4 — Weekly Vacation Days Audit (13 runs, weekly intervals over 13 weeks)
-- 10 SUCCESS, 2 FAILED, 1 CANCELLED
-- ============================================================================

INSERT INTO schedule_runs (id, schedule_id, status, started_at, completed_at, records_processed, error_message)
VALUES
  -- Run 40: SUCCESS
  ('f2000000-0000-0000-0000-000000000040', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '91 days', NOW() - INTERVAL '91 days' + INTERVAL '4100 milliseconds', 34, NULL),
  -- Run 41: SUCCESS
  ('f2000000-0000-0000-0000-000000000041', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '84 days', NOW() - INTERVAL '84 days' + INTERVAL '3500 milliseconds', 52, NULL),
  -- Run 42: FAILED
  ('f2000000-0000-0000-0000-000000000042', 'f1000000-0000-0000-0000-000000000004', 'FAILED',
   NOW() - INTERVAL '77 days', NOW() - INTERVAL '77 days' + INTERVAL '2800 milliseconds', 0, 'DMN evaluation error: unknown role ''Intern'''),
  -- Run 43: SUCCESS
  ('f2000000-0000-0000-0000-000000000043', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '70 days', NOW() - INTERVAL '70 days' + INTERVAL '1900 milliseconds', 41, NULL),
  -- Run 44: SUCCESS
  ('f2000000-0000-0000-0000-000000000044', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '63 days', NOW() - INTERVAL '63 days' + INTERVAL '2300 milliseconds', 67, NULL),
  -- Run 45: SUCCESS
  ('f2000000-0000-0000-0000-000000000045', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '56 days', NOW() - INTERVAL '56 days' + INTERVAL '3800 milliseconds', 29, NULL),
  -- Run 46: CANCELLED
  ('f2000000-0000-0000-0000-000000000046', 'f1000000-0000-0000-0000-000000000004', 'CANCELLED',
   NOW() - INTERVAL '49 days', NOW() - INTERVAL '49 days' + INTERVAL '400 milliseconds', 0, NULL),
  -- Run 47: SUCCESS
  ('f2000000-0000-0000-0000-000000000047', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '42 days', NOW() - INTERVAL '42 days' + INTERVAL '2600 milliseconds', 58, NULL),
  -- Run 48: FAILED
  ('f2000000-0000-0000-0000-000000000048', 'f1000000-0000-0000-0000-000000000004', 'FAILED',
   NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days' + INTERVAL '3200 milliseconds', 0, 'No matching rule found for input combination'),
  -- Run 49: SUCCESS
  ('f2000000-0000-0000-0000-000000000049', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days' + INTERVAL '1700 milliseconds', 73, NULL),
  -- Run 50: SUCCESS
  ('f2000000-0000-0000-0000-000000000050', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days' + INTERVAL '2900 milliseconds', 45, NULL),
  -- Run 51: SUCCESS
  ('f2000000-0000-0000-0000-000000000051', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days' + INTERVAL '2100 milliseconds', 61, NULL),
  -- Run 52: SUCCESS
  ('f2000000-0000-0000-0000-000000000052', 'f1000000-0000-0000-0000-000000000004', 'SUCCESS',
   NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days' + INTERVAL '1400 milliseconds', 39, NULL)
ON CONFLICT (id) DO NOTHING;
