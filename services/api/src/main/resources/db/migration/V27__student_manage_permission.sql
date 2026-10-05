INSERT INTO permissions(code, description) VALUES
('students.manage', 'Manage student records and enrollment')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN')
  AND p.code='students.manage'
ON CONFLICT DO NOTHING;
