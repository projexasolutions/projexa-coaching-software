-- Attendance engine hardening: tenant-safe uniqueness, valid states, fee restriction overrides and query indexes.

CREATE TABLE IF NOT EXISTS student_attendance_restrictions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
  reason VARCHAR(200) NOT NULL DEFAULT 'FEE_OVERDUE',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_student_attendance_restriction_active
  ON student_attendance_restrictions(tenant_id, student_id)
  WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_attendance_sessions_tenant_batch_date
  ON attendance_sessions(tenant_id, batch_id, session_date);

CREATE INDEX IF NOT EXISTS idx_attendance_records_session_status
  ON attendance_records(tenant_id, session_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_session_slot
  ON attendance_sessions(
    tenant_id,
    batch_id,
    session_date,
    subject_id,
    COALESCE(start_time, TIME '00:00:00')
  );

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname='ck_attendance_record_status'
  ) THEN
    ALTER TABLE attendance_records
      ADD CONSTRAINT ck_attendance_record_status
      CHECK (status IN ('UNMARKED','PRESENT','ABSENT','LATE','LEAVE'));
  END IF;
END $$;
