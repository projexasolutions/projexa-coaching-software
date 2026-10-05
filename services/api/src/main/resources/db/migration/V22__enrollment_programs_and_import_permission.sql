ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS program_id UUID REFERENCES programs(id);

ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS enrollments_tenant_id_student_id_academic_year_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollment_tenant_student_year_program
    ON enrollments(tenant_id, student_id, academic_year_id, COALESCE(program_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE INDEX IF NOT EXISTS idx_enrollments_tenant_student
    ON enrollments(tenant_id, student_id);

INSERT INTO permissions(code, description) VALUES
('students.import', 'Import students from structured files')
ON CONFLICT(code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('INSTITUTE_OWNER','INSTITUTE_ADMIN')
  AND p.code='students.import'
ON CONFLICT DO NOTHING;
