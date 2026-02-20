CREATE TABLE sso_configs (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                   UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    provider_type            VARCHAR(20) NOT NULL CHECK (provider_type IN ('SAML','OIDC')),
    metadata_url             TEXT,
    client_id                VARCHAR(255),
    client_secret_encrypted  TEXT,
    attribute_mapping        JSONB NOT NULL DEFAULT '{}',
    enabled                  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_sso_configs_org_provider_unique ON sso_configs(org_id, provider_type) WHERE enabled = TRUE;
CREATE INDEX idx_sso_configs_org_id ON sso_configs(org_id);
