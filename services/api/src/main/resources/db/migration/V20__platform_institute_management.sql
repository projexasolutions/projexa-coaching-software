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

INSERT INTO platform_admins(user_id)
SELECT u.id FROM users u WHERE u.email='owner@demo.projexa.local'
ON CONFLICT (user_id) DO NOTHING;