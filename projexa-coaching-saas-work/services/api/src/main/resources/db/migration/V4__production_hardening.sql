-- Query/index hardening and configurable operational defaults.
CREATE INDEX IF NOT EXISTS idx_attendance_records_student_tenant ON attendance_records(tenant_id,student_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student ON exam_attempts(tenant_id,student_id,exam_id);
CREATE INDEX IF NOT EXISTS idx_results_student_status ON results(tenant_id,student_id,status);
CREATE INDEX IF NOT EXISTS idx_payments_invoice_status ON payments(tenant_id,invoice_id,status);
CREATE INDEX IF NOT EXISTS idx_followups_due ON follow_ups(tenant_id,due_at,status);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(tenant_id,status,priority);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(tenant_id,entity_type,entity_id,created_at);
INSERT INTO tenant_settings(tenant_id,settings) VALUES ('00000000-0000-0000-0000-000000000001','{"feeAttendanceRestriction":{"enabled":false,"overdueDays":0,"graceAmount":0},"ranking":{"visibility":"STUDENT_PARENT"},"notifications":{"channels":["IN_APP","PUSH"]},"video":{"enabled":true,"recordingConsentRequired":true},"branding":{"primaryColor":"#0f766e","secondaryColor":"#0f172a"}}'::jsonb) ON CONFLICT(tenant_id) DO NOTHING;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_questions_subject_type ON questions(tenant_id,subject_id,question_type,active);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_idempotency ON payments(tenant_id,idempotency_key) WHERE idempotency_key IS NOT NULL;
