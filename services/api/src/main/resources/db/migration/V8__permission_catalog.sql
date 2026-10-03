-- Complete permission catalog used by API method-security expressions.
INSERT INTO permissions(code, description) VALUES
('teachers.manage', 'Create/update teachers'),
('attendance.manage', 'Manage attendance'),
('exams.manage', 'Manage exams and attempts'),
('questions.manage', 'Manage question bank'),
('results.manage', 'Generate and publish results'),
('finance.manage', 'Manage invoices and payments'),
('communication.send', 'Send internal communications'),
('automation.manage', 'Manage automation rules'),
('admissions.manage', 'Manage admissions CRM'),
('support.manage', 'Manage support tickets'),
('reports.read', 'View reports and analytics')
ON CONFLICT (code) DO NOTHING;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
-- Grant the complete catalog to the bootstrap owner role; tenant admins can be configured separately.
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code='INSTITUTE_OWNER'
ON CONFLICT DO NOTHING;
