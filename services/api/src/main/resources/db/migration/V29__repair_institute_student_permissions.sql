-- Ensure institute owners and administrators can access and manage student operations.
-- This migration is intentionally idempotent and does not modify student records.

INSERT INTO permissions(code, description) VALUES
    ('students.read', 'View student records'),
    ('students.create', 'Create student records'),
    ('students.manage', 'Manage student records and enrollment')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER', 'INSTITUTE_ADMIN')
  AND p.code IN ('students.read', 'students.create', 'students.manage')
ON CONFLICT DO NOTHING;
