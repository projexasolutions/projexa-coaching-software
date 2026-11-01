-- Grant the dashboard summary endpoint permission to institute owners/admins.
-- DashboardController requires hasAuthority('dashboard.read').
INSERT INTO permissions(code, description) VALUES
('dashboard.read', 'View the institute command center dashboard')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER', 'INSTITUTE_ADMIN')
  AND p.code = 'dashboard.read'
ON CONFLICT DO NOTHING;
