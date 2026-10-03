CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_idempotency ON payments(tenant_id,idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_attempt_answers_attempt ON attempt_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_results_student_published ON results(tenant_id,student_id,status,published_at);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records(tenant_id,student_id);
