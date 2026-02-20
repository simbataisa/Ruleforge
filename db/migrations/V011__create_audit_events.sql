CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL,
    space_id        UUID,
    event_type      VARCHAR(100) NOT NULL,
    actor_id        UUID,
    actor_type      VARCHAR(20),
    resource_type   VARCHAR(50),
    resource_id     UUID,
    changes         JSONB,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_org_time ON audit_events(org_id, created_at DESC);
CREATE INDEX idx_audit_events_resource ON audit_events(resource_type, resource_id);
CREATE INDEX idx_audit_events_type ON audit_events(event_type);
