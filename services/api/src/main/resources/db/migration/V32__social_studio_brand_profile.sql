CREATE TABLE social_brand_profiles (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    institute_name VARCHAR(160) NOT NULL,
    target_audience VARCHAR(240),
    brand_tone VARCHAR(80) NOT NULL DEFAULT 'Friendly and professional',
    website_url VARCHAR(240),
    call_to_action VARCHAR(180),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
