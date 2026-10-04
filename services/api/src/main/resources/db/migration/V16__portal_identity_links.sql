ALTER TABLE students ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_students_tenant_user ON students(tenant_id,user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_parents_tenant_user ON parents(tenant_id,user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_student_parents_parent ON student_parents(parent_id);
