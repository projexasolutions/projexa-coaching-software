CREATE TABLE account_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role_code VARCHAR(40) NOT NULL CHECK (role_code IN ('TEACHER','STUDENT','PARENT')),
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_account_invitations_tenant_email
    ON account_invitations(tenant_id,lower(email),created_at DESC);
CREATE INDEX idx_account_invitations_expiry
    ON account_invitations(expires_at) WHERE accepted_at IS NULL;

INSERT INTO permissions(code,description)
VALUES ('users.manage','Invite and manage institute user accounts')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN') AND p.code='users.manage'
ON CONFLICT DO NOTHING;
CREATE UNIQUE INDEX uq_roles_tenant_code ON roles(tenant_id,code) WHERE tenant_id IS NOT NULL;
