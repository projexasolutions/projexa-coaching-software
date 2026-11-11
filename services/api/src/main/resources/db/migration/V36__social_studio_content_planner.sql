CREATE TABLE social_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    category VARCHAR(40) NOT NULL,
    platforms VARCHAR(80) NOT NULL DEFAULT 'INSTAGRAM,FACEBOOK',
    format VARCHAR(40) NOT NULL DEFAULT 'Single image',
    caption TEXT NOT NULL,
    hashtags TEXT,
    scheduled_for TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_social_posts_category CHECK (category IN ('EDUCATIONAL','PROMOTIONAL','ENGAGEMENT','BRANDING')),
    CONSTRAINT chk_social_posts_status CHECK (status IN ('DRAFT','APPROVED','SCHEDULED','PUBLISHED'))
);
CREATE INDEX idx_social_posts_tenant_status ON social_posts(tenant_id,status,scheduled_for);
CREATE INDEX idx_social_posts_tenant_created ON social_posts(tenant_id,created_at DESC);

INSERT INTO permissions(code, description)
VALUES ('social.manage', 'Manage institute social media content and publishing plans')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN')
  AND p.code = 'social.manage'
ON CONFLICT DO NOTHING;
