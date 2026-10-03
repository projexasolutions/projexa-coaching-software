CREATE TABLE student_attendance_restrictions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), tenant_id UUID NOT NULL REFERENCES tenants(id), student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  reason VARCHAR(200) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id,student_id)
);
CREATE TABLE student_watchlist (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), tenant_id UUID NOT NULL REFERENCES tenants(id), student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  reason VARCHAR(200), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id,student_id)
);
CREATE TABLE staff_tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), tenant_id UUID NOT NULL REFERENCES tenants(id), assigned_to UUID REFERENCES users(id),
  student_id UUID REFERENCES students(id), lead_id UUID REFERENCES leads(id), title VARCHAR(200) NOT NULL, due_at TIMESTAMP, status VARCHAR(30) NOT NULL DEFAULT 'OPEN', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_student_restrictions_active ON student_attendance_restrictions(tenant_id,student_id,active);
CREATE INDEX idx_watchlist_active ON student_watchlist(tenant_id,student_id,active);
