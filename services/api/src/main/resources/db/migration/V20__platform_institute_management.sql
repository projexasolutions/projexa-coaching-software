CREATE TABLE platform_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platform_admin_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    tenant_id UUID REFERENCES tenants(id),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO permissions(code,description) VALUES
('platform.institutes.read','Manage platform institutes'),
('platform.institutes.manage','Create and configure platform institutes')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(id,tenant_id,name,code,description,system_role) VALUES
('00000000-0000-0000-0000-000000000020',NULL,'Projexa Platform Admin','PLATFORM_ADMIN','Internal Projexa platform administration',TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id)
SELECT '00000000-0000-0000-0000-000000000020',id FROM permissions
WHERE code IN ('platform.institutes.read','platform.institutes.manage')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles(user_id,role_id)
SELECT u.id,'00000000-0000-0000-0000-000000000020' FROM users u
WHERE u.email='owner@demo.projexa.local'
ON CONFLICT DO NOTHING;

INSERT INTO platform_admins(user_id)
SELECT u.id FROM users u WHERE u.email='owner@demo.projexa.local'
ON CONFLICT (user_id) DO NOTHING;