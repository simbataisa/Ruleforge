CREATE TABLE rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    folder_id       UUID REFERENCES folders(id) ON DELETE SET NULL,
    name            VARCHAR(255) NOT NULL,
    alias           VARCHAR(100) UNIQUE,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('TABLE','TREE','SCRIPT','FLOW','SCORECARD','DMN','CEP','LOOKUP','ENSEMBLE')),
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    latest_version  INT NOT NULL DEFAULT 0,
    locked          BOOLEAN NOT NULL DEFAULT FALSE,
    locked_by       UUID REFERENCES members(id),
    locked_at       TIMESTAMPTZ,
    created_by      UUID NOT NULL REFERENCES members(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rules_space_id ON rules(space_id);
CREATE INDEX idx_rules_folder_id ON rules(folder_id);
CREATE INDEX idx_rules_alias ON rules(alias);
CREATE INDEX idx_rules_type ON rules(type);
CREATE INDEX idx_rules_status ON rules(status);
