create table if not exists exam_questions (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
 exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
 question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
 subject_id UUID NOT NULL REFERENCES subjects(id),
 display_order INT NOT NULL DEFAULT 0,
 UNIQUE(exam_id,question_id)
);
create index if not exists idx_exam_questions_exam_order on exam_questions(tenant_id,exam_id,display_order);
create index if not exists idx_exam_questions_question on exam_questions(tenant_id,question_id);
alter table exams drop constraint if exists chk_exam_status;
alter table exams add constraint chk_exam_status check (status in ('DRAFT','SCHEDULED','LIVE','COMPLETED','ARCHIVED'));
alter table exam_subjects drop constraint if exists chk_exam_subject_marks;
alter table exam_subjects add constraint chk_exam_subject_marks check (max_marks > 0 and (pass_marks is null or (pass_marks >= 0 and pass_marks <= max_marks)) and negative_marking >= 0);
alter table questions drop constraint if exists chk_question_marks;
alter table questions add constraint chk_question_marks check (marks > 0);
