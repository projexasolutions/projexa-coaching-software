create index if not exists idx_results_exam_rank on results(tenant_id,exam_id,rank);
create index if not exists idx_results_student_published on results(tenant_id,student_id,status,published_at);
create index if not exists idx_result_subjects_subject on result_subjects(subject_id,result_id);
create index if not exists idx_exam_attempts_exam_status_score on exam_attempts(tenant_id,exam_id,status,score);
