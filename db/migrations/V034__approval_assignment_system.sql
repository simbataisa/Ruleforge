-- V034: Approval Assignment System
-- Sprint 02: EPIC 2 - Approval Request Tracking with Assignees

-- 1. approval_requests table
CREATE TABLE IF NOT EXISTS approval_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    version         INTEGER NOT NULL,
    requested_by    UUID NOT NULL,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note            TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    resolved_by     UUID,
    resolved_at     TIMESTAMPTZ,
    deadline        TIMESTAMPTZ,
    UNIQUE(rule_id, version)
);

CREATE INDEX IF NOT EXISTS idx_approval_requests_rule_id ON approval_requests(rule_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_status ON approval_requests(status);
CREATE INDEX IF NOT EXISTS idx_approval_requests_requested_by ON approval_requests(requested_by);
CREATE INDEX IF NOT EXISTS idx_approval_requests_deadline ON approval_requests(deadline)
    WHERE status = 'PENDING';

-- 2. approval_request_assignees table
CREATE TABLE IF NOT EXISTS approval_request_assignees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    assignee_id     UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    responded_at    TIMESTAMPTZ,
    UNIQUE(request_id, assignee_id)
);

CREATE INDEX IF NOT EXISTS idx_approval_assignees_request_id ON approval_request_assignees(request_id);
CREATE INDEX IF NOT EXISTS idx_approval_assignees_assignee_id ON approval_request_assignees(assignee_id);
CREATE INDEX IF NOT EXISTS idx_approval_assignees_pending ON approval_request_assignees(assignee_id, status)
    WHERE status = 'PENDING';

-- 3. space_approval_settings table
CREATE TABLE IF NOT EXISTS space_approval_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id                UUID NOT NULL UNIQUE REFERENCES spaces(id) ON DELETE CASCADE,
    default_approvers       UUID[] NOT NULL DEFAULT '{}',
    sla_hours               INTEGER NOT NULL DEFAULT 48,
    require_all_approvers   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. RLS policies
ALTER TABLE approval_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_request_assignees ENABLE ROW LEVEL SECURITY;
ALTER TABLE space_approval_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY approval_requests_org_isolation ON approval_requests
    USING (
        EXISTS (
            SELECT 1 FROM rules r
            JOIN spaces s ON s.id = r.space_id
            WHERE r.id = approval_requests.rule_id
              AND s.org_id = current_setting('app.current_org_id')::uuid
        )
    );

CREATE POLICY approval_assignees_org_isolation ON approval_request_assignees
    USING (
        EXISTS (
            SELECT 1 FROM approval_requests ar
            JOIN rules r ON r.id = ar.rule_id
            JOIN spaces s ON s.id = r.space_id
            WHERE ar.id = approval_request_assignees.request_id
              AND s.org_id = current_setting('app.current_org_id')::uuid
        )
    );

CREATE POLICY space_approval_settings_org_isolation ON space_approval_settings
    USING (
        EXISTS (
            SELECT 1 FROM spaces s
            WHERE s.id = space_approval_settings.space_id
              AND s.org_id = current_setting('app.current_org_id')::uuid
        )
    );
