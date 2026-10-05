INSERT INTO permissions(code, description) VALUES
('students.read', 'View students'),
('students.create', 'Create students')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN')
  AND p.code IN ('students.read','students.create')
ON CONFLICT DO NOTHING;
