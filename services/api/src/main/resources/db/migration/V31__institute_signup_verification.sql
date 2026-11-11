CREATE TABLE institute_signup_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_signup_verification_expiry
    ON institute_signup_verifications(expires_at)
    WHERE consumed_at IS NULL;

CREATE UNIQUE INDEX uq_tenants_slug_lower
    ON tenants(lower(slug));

CREATE INDEX idx_users_tenant_email_lower
    ON users(tenant_id, lower(email))
    WHERE email IS NOT NULL;