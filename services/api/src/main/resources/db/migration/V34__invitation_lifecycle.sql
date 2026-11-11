ALTER TABLE account_invitations
    ADD COLUMN revoked_at TIMESTAMP;

CREATE INDEX idx_account_invitations_pending_tenant
    ON account_invitations(tenant_id,created_at DESC)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;
