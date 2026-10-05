-- Enforce one submitted attempt per student and exam.
-- Keep the most recently submitted attempt when legacy duplicates exist.
WITH ranked AS (
  SELECT id,
         row_number() OVER (
           PARTITION BY tenant_id, exam_id, student_id
           ORDER BY coalesce(submitted_at, started_at) DESC NULLS LAST, id DESC
         ) AS rn
  FROM exam_attempts
  WHERE status = 'SUBMITTED'
)
DELETE FROM attempt_answers aa
USING ranked r
WHERE aa.attempt_id = r.id
  AND r.rn > 1;

WITH ranked AS (
  SELECT id,
         row_number() OVER (
           PARTITION BY tenant_id, exam_id, student_id
           ORDER BY coalesce(submitted_at, started_at) DESC NULLS LAST, id DESC
         ) AS rn
  FROM exam_attempts
  WHERE status = 'SUBMITTED'
)
DELETE FROM exam_attempts ea
USING ranked r
WHERE ea.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_attempt_submitted
  ON exam_attempts(tenant_id, exam_id, student_id)
  WHERE status = 'SUBMITTED';
