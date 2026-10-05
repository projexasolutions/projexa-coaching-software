-- Exam attempt concurrency hardening: a student may have at most one in-progress attempt per exam.
CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_attempt_in_progress
  ON exam_attempts(tenant_id, exam_id, student_id)
  WHERE status = 'IN_PROGRESS';
