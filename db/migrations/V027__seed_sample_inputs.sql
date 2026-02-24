-- ============================================================================
-- V027: Seed sample inputs for all 17 rules
--
-- Provides 2-3 realistic sample inputs per rule for one-click testing.
-- First sample for each rule matches the smoke-test.sh data.
--
-- Rules from V022 (9): e0000000-...-01 through e0000000-...-08
-- Rules from V024 (8): e1000000-...-01 through e1000000-...-08
-- Rule from V025 (1):  e2000000-...-01
--
-- created_by = admin user c0000000-0000-0000-0000-000000000001
-- ============================================================================

-- ============================================================================
-- V022 Rules
-- ============================================================================

-- 1. TABLE: Loan Eligibility (e0000000-...-01)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'High credit, high income',
     'Excellent applicant — credit 750+, income 60k. Expects: eligible=true, maxLoan=500000',
     '{"creditScore": 750, "income": 60000}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000001', 'Mid-range applicant',
     'Moderate profile — credit 680, income 35k. Expects: eligible=true, maxLoan=200000',
     '{"creditScore": 680, "income": 35000}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000001', 'Low credit, low income',
     'Poor profile — credit 400, income 20k. Expects: eligible=false, maxLoan=0',
     '{"creditScore": 400, "income": 20000}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 2. TREE: Insurance Risk Assessment (e0000000-...-02)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000002', 'Young driver, no accidents',
     'Under 25, clean record. Expects: riskCategory=MEDIUM, premium=1.2',
     '{"age": 22, "accidents": 0}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000002', 'Young driver, 2 accidents',
     'Under 25 with accidents. Expects: riskCategory=HIGH, premium=1.5',
     '{"age": 20, "accidents": 2}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000002', 'Older driver, clean record',
     'Over 25, no accidents. Expects: riskCategory=LOW, premium=1.0',
     '{"age": 45, "accidents": 0}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 3. SCORECARD: Customer Credit Scoring (e0000000-...-03)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000003', 'Excellent credit profile',
     'Top marks across all factors. Expects high score.',
     '{"paymentHistory": 95, "debtToIncome": 15, "accountAge": 7, "creditMix": 4}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000003', 'Average credit profile',
     'Middle-of-road values. Expects moderate score.',
     '{"paymentHistory": 75, "debtToIncome": 35, "accountAge": 4, "creditMix": 2}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000003', 'Poor credit profile',
     'Low scores across the board. Expects low score.',
     '{"paymentHistory": 40, "debtToIncome": 55, "accountAge": 1, "creditMix": 0}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 4. SCRIPT: Dynamic Pricing (e0000000-...-04)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000004', 'Normal demand',
     'Base price $100 with demand index 120. Expects finalPrice around $120.',
     '{"basePrice": 100, "demandIndex": 120}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000004', 'High demand surge',
     'Base price $50 with very high demand. Price capped at 2x.',
     '{"basePrice": 50, "demandIndex": 300}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000004', 'Low demand discount',
     'Base price $200 with low demand. Price floored at 0.5x.',
     '{"basePrice": 200, "demandIndex": 20}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 5. FLOW: Order Processing Pipeline (e0000000-...-05)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000005', 'Valid order',
     'Order total $500. Expects: status=APPROVED.',
     '{"orderTotal": 500}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000005', 'Zero-value order',
     'Order total $0. Expects: status=REJECTED.',
     '{"orderTotal": 0}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000005', 'Negative order',
     'Negative total (refund scenario). Expects: status=REJECTED.',
     '{"orderTotal": -50}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 6. DMN: Vacation Days (e0000000-...-06)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000006', 'Senior with 3 years',
     'Senior role, 3 years of service. Expects: vacationDays=20.',
     '{"yearsOfService": 3, "role": "Senior"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000006', 'Manager with 10 years',
     'Manager role, 10 years of service. Expects: vacationDays=30.',
     '{"yearsOfService": 10, "role": "Manager"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000006', 'Junior new hire',
     'Junior role, 0 years of service. Expects: vacationDays=15.',
     '{"yearsOfService": 0, "role": "Junior"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 7. ENSEMBLE: Fraud Detection (e0000000-...-07)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000007', 'Standard transaction',
     'Normal-looking $750 domestic transaction.',
     '{"creditScore": 750, "income": 60000}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000007', 'Low credit signal',
     'Low credit score and income — higher fraud signal.',
     '{"creditScore": 400, "income": 20000}', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 8. CEP: Transaction Velocity (e0000000-...-08)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e0000000-0000-0000-0000-000000000008', 'High-value US transaction',
     'Single $200 domestic transaction event.',
     '{"transactionId": "txn-001", "amount": 200, "country": "US"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000008', 'Small foreign transaction',
     'Low-value international transaction. Below alert threshold.',
     '{"transactionId": "txn-002", "amount": 50, "country": "NG"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000008', 'Large suspicious transaction',
     'High-value transaction from high-risk country.',
     '{"transactionId": "txn-003", "amount": 9999, "country": "RU"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- ============================================================================
-- V024 Rules (Showcase)
-- ============================================================================

-- 9. TABLE: Employee Benefits Eligibility (e1000000-...-01)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000001', 'Senior engineer, exceeds',
     'Engineering, 5+ years, exceeds expectations, full-time. Expects: platinum tier.',
     '{"department": "Engineering", "yearsOfService": 5, "performanceRating": "Exceeds", "employeeType": "full-time"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000001', 'Sales contractor',
     'Sales, 3 years, meets expectations, contractor. Expects: no benefits (contractor).',
     '{"department": "Sales", "yearsOfService": 3, "performanceRating": "Meets", "employeeType": "contractor"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000001', 'New support employee',
     'Support, 1 year, meets expectations, full-time. Expects: silver tier.',
     '{"department": "Support", "yearsOfService": 1, "performanceRating": "Meets", "employeeType": "full-time"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 10. TREE: Ticket Routing (e1000000-...-02)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000002', 'P1 Enterprise critical',
     'Critical P1 ticket from Enterprise customer. Expects: Senior Engineering, 1hr SLA.',
     '{"priority": "P1", "customerTier": "Enterprise", "issueCategory": "Technical"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000002', 'P2 billing issue',
     'Medium priority billing ticket. Expects: Finance Support, 8hr SLA.',
     '{"priority": "P2", "customerTier": "Business", "issueCategory": "Billing"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000002', 'P4 free-tier feature request',
     'Low priority from free-tier user. Expects: Self-Service, 72hr SLA.',
     '{"priority": "P4", "customerTier": "Free", "issueCategory": "Feature"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 11. SCORECARD: Vendor Risk (e1000000-...-03)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000003', 'Top-tier vendor',
     'Excellent across all 5 dimensions. Expects high score.',
     '{"financialScore": 92, "complianceCerts": 5, "deliveryOnTime": 97, "securityAuditScore": 88, "yearsInBusiness": 12}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000003', 'Risky startup vendor',
     'New company, few certs, inconsistent delivery. Expects low score.',
     '{"financialScore": 45, "complianceCerts": 1, "deliveryOnTime": 65, "securityAuditScore": 55, "yearsInBusiness": 1}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000003', 'Mid-range vendor',
     'Average performance across categories.',
     '{"financialScore": 72, "complianceCerts": 3, "deliveryOnTime": 88, "securityAuditScore": 76, "yearsInBusiness": 6}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 12. SCRIPT: Tax Calculator (e1000000-...-04)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000004', 'Single filer, $85k CA',
     'Single filer, $85k gross income in California, no dependents.',
     '{"grossIncome": 85000, "filingStatus": "single", "dependents": 0, "deductions": 0, "stateCode": "CA"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000004', 'Married, $150k TX, 2 kids',
     'Married filing, $150k in Texas (no state tax), 2 dependents.',
     '{"grossIncome": 150000, "filingStatus": "married", "dependents": 2, "deductions": 0, "stateCode": "TX"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000004', 'High earner, $500k NY, itemized',
     'High income, New York, large itemized deductions.',
     '{"grossIncome": 500000, "filingStatus": "single", "dependents": 0, "deductions": 50000, "stateCode": "NY"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 13. FLOW: Loan Application Pipeline (e1000000-...-05)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000005', 'Strong applicant',
     'Good credit, high income, reasonable loan amount. Expects: APPROVED.',
     '{"creditScore": 720, "annualIncome": 85000, "requestedAmount": 300000, "existingDebt": 15000}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000005', 'Weak applicant',
     'Low credit and high debt. Expects: REJECTED.',
     '{"creditScore": 500, "annualIncome": 40000, "requestedAmount": 200000, "existingDebt": 25000}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000005', 'Borderline applicant',
     'Credit 620 — between 600 and 650. Expects: MANUAL_REVIEW.',
     '{"creditScore": 620, "annualIncome": 60000, "requestedAmount": 150000, "existingDebt": 10000}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 14. DMN: Shipping Rates (e1000000-...-06)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000006', 'Small domestic express',
     '3kg domestic express package. Expects: $14.99, Priority Mail, 2 days.',
     '{"weightKg": 3, "zone": "domestic", "priority": "express"}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000006', 'Heavy international standard',
     '15kg international standard shipment. Expects: $54.99, Ocean Freight, 21 days.',
     '{"weightKg": 15, "zone": "international", "priority": "standard"}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000006', 'Overnight domestic',
     '1kg domestic overnight package. Expects: $29.99, Express Air, 1 day.',
     '{"weightKg": 1, "zone": "domestic", "priority": "overnight"}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 15. ENSEMBLE: Customer Churn Prediction (e1000000-...-07)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000007', 'Loyal customer profile',
     'Strong signals across all member models. Expects low churn risk.',
     '{"paymentHistory": 95, "debtToIncome": 15, "accountAge": 7, "creditMix": 4, "creditScore": 750, "income": 60000, "age": 35, "accidents": 0}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000007', 'At-risk customer',
     'Poor payment history, high debt. Expects high churn risk.',
     '{"paymentHistory": 40, "debtToIncome": 60, "accountAge": 1, "creditMix": 1, "creditScore": 500, "income": 30000, "age": 22, "accidents": 3}', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- 16. CEP: Login Anomaly Detector (e1000000-...-08)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e1000000-0000-0000-0000-000000000008', 'Failed login from US',
     'Single failed login attempt from known US IP.',
     '{"userId": "user1", "ip": "1.2.3.4", "country": "US", "success": false}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000008', 'Successful login from EU',
     'Successful login from a European IP.',
     '{"userId": "user2", "ip": "82.130.55.10", "country": "DE", "success": true}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000008', 'Failed login from unusual location',
     'Failed login from high-risk geography.',
     '{"userId": "user1", "ip": "103.45.67.89", "country": "CN", "success": false}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- ============================================================================
-- V025 Rule
-- ============================================================================

-- 17. TREE: Loan Decision with Actions (e2000000-...-01)
INSERT INTO rule_sample_inputs (rule_id, name, description, input, sort_order, created_by) VALUES
    ('e2000000-0000-0000-0000-000000000001', 'Prime approval',
     'High credit + high income. Expects: APPROVED, prime tier, 4.5% rate.',
     '{"creditScore": 750, "annualIncome": 80000}', 0, 'c0000000-0000-0000-0000-000000000001'),
    ('e2000000-0000-0000-0000-000000000001', 'Rejection',
     'Low credit + low income. Expects: REJECTED, unqualified.',
     '{"creditScore": 500, "annualIncome": 30000}', 1, 'c0000000-0000-0000-0000-000000000001'),
    ('e2000000-0000-0000-0000-000000000001', 'Subprime approval',
     'Low credit but high income compensates. Expects: APPROVED, subprime, 9.5% rate.',
     '{"creditScore": 600, "annualIncome": 90000}', 2, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;
