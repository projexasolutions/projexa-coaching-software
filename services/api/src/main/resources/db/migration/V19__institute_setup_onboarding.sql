CREATE TABLE institute_setup_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    institute_type VARCHAR(80),
    setup_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    current_step VARCHAR(80) NOT NULL DEFAULT 'INSTITUTE_PROFILE',
    completed_steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    notes TEXT,
    go_live_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_setup_profiles_status ON institute_setup_profiles(setup_status);

INSERT INTO institute_setup_profiles(tenant_id)
SELECT t.id FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM institute_setup_profiles p WHERE p.tenant_id=t.id
);

INSERT INTO permissions(code,description) VALUES
('setup.read','View institute setup and onboarding status'),
('setup.manage','Manage institute onboarding configuration')
ON CONFLICT (code) DO NOTHING;