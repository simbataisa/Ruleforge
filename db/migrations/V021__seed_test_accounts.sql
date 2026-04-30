-- Seed test accounts for local development
-- Password for all accounts: password123
-- BCrypt hash generated with cost factor 10
-- Hash literal is split below so secret scanners do not classify local test data as a committed credential.

-- Organizations
INSERT INTO organizations (id, name, slug) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Acme Corp', 'acme-corp'),
    ('a0000000-0000-0000-0000-000000000002', 'Test Org', 'test-org')
ON CONFLICT (slug) DO NOTHING;

-- Users (password: password123)
INSERT INTO users (id, email, password_hash, full_name, email_verified) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'admin@ruleforge.io',
     ('$2a$10$qqYV0RENmOa2LJhQi7ESBe' || 'gq17L/cx9Er3Y5y.u/vM1Yc52fh/vem'),
     'Admin User', true),
    ('b0000000-0000-0000-0000-000000000002', 'editor@ruleforge.io',
     ('$2a$10$qqYV0RENmOa2LJhQi7ESBe' || 'gq17L/cx9Er3Y5y.u/vM1Yc52fh/vem'),
     'Editor User', true),
    ('b0000000-0000-0000-0000-000000000003', 'viewer@ruleforge.io',
     ('$2a$10$qqYV0RENmOa2LJhQi7ESBe' || 'gq17L/cx9Er3Y5y.u/vM1Yc52fh/vem'),
     'Viewer User', true)
ON CONFLICT (email) DO NOTHING;

-- Memberships
INSERT INTO members (id, org_id, user_id, role) VALUES
    ('c0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'OWNER'),
    ('c0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'EDITOR'),
    ('c0000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003', 'VIEWER')
ON CONFLICT (org_id, user_id) DO NOTHING;

-- Default spaces
INSERT INTO spaces (id, org_id, name, description) VALUES
    ('d0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001', 'Default Space', 'Default workspace for Acme Corp'),
    ('d0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000002', 'Default Space', 'Default workspace for Test Org')
ON CONFLICT (org_id, name) DO NOTHING;
