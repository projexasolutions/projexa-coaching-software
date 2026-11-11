INSERT INTO permissions(code,description)
VALUES ('roles.manage','Create institute roles and configure non-privileged permissions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id
FROM roles r
JOIN permissions p ON p.code='roles.manage'
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN')
  AND (r.tenant_id IS NOT NULL OR r.tenant_id IS NULL)
ON CONFLICT DO NOTHING;
