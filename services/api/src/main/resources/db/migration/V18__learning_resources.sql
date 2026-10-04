CREATE TABLE IF NOT EXISTS learning_resources (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  batch_id UUID REFERENCES batches(id) ON DELETE SET NULL,
  subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
  teacher_id UUID REFERENCES teachers(id) ON DELETE SET NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  resource_type VARCHAR(30) NOT NULL DEFAULT 'LINK',
  resource_url TEXT NOT NULL,
  topic VARCHAR(160),
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE learning_resources DROP CONSTRAINT IF EXISTS chk_learning_resource_type;
ALTER TABLE learning_resources ADD CONSTRAINT chk_learning_resource_type
  CHECK (resource_type IN ('LINK','PDF','VIDEO','DOCUMENT','RECORDING'));

CREATE INDEX IF NOT EXISTS idx_learning_resources_tenant_batch
  ON learning_resources(tenant_id,batch_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_learning_resources_tenant_subject
  ON learning_resources(tenant_id,subject_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_learning_resources_tenant_published
  ON learning_resources(tenant_id,published,created_at DESC);

INSERT INTO permissions(code, description) VALUES
('learning.manage', 'Create/update learning resources'),
('learning.read', 'View learning resources')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code='INSTITUTE_OWNER' AND p.code IN ('learning.manage','learning.read')
ON CONFLICT DO NOTHING;
