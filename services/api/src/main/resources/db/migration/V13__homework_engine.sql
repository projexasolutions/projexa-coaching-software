create index if not exists idx_assignments_tenant_batch_due on assignments(tenant_id,batch_id,due_at);
create index if not exists idx_assignment_submissions_assignment_status on assignment_submissions(assignment_id,status);
create index if not exists idx_assignment_submissions_student on assignment_submissions(tenant_id,student_id,assignment_id);
alter table assignment_submissions drop constraint if exists chk_assignment_submission_status;
alter table assignment_submissions add constraint chk_assignment_submission_status check (status in ('PENDING','SUBMITTED','GRADED','LATE'));
alter table assignment_submissions drop constraint if exists chk_assignment_submission_score;
alter table assignment_submissions add constraint chk_assignment_submission_score check (score is null or score >= 0);
