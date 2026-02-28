-- V036: Fix approval_requests unique constraint
-- Replace the broad UNIQUE(rule_id, version) with a partial unique index that
-- only enforces uniqueness for ACTIVE (PENDING) requests.
-- This allows multiple historical records (REJECTED/CANCELLED/APPROVED) per
-- rule+version while still preventing duplicate concurrent approvals.

-- Drop the over-broad unique constraint
ALTER TABLE approval_requests DROP CONSTRAINT IF EXISTS approval_requests_rule_id_version_key;

-- Partial unique index: only one PENDING request per (rule_id, version) at a time
CREATE UNIQUE INDEX IF NOT EXISTS uq_approval_requests_pending
    ON approval_requests(rule_id, version)
    WHERE status = 'PENDING';
