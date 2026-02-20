CREATE TABLE connectors (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id              UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    name                  VARCHAR(255) NOT NULL,
    type                  VARCHAR(20) NOT NULL CHECK (type IN ('JDBC','HTTP','REDIS')),
    config                JSONB NOT NULL DEFAULT '{}',
    credentials_encrypted TEXT,
    enabled               BOOLEAN NOT NULL DEFAULT TRUE,
    description           TEXT,
    created_by            UUID NOT NULL REFERENCES members(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_connectors_space_id ON connectors(space_id);
CREATE INDEX idx_connectors_type ON connectors(type);
CREATE INDEX idx_connectors_enabled ON connectors(enabled);
