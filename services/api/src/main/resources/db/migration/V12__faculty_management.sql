create index if not exists idx_teachers_tenant_status_name on teachers(tenant_id,status,first_name,last_name);
create index if not exists idx_teacher_batch_assignments_batch on teacher_batch_assignments(batch_id,teacher_id);
create index if not exists idx_teacher_subjects_subject on teacher_subjects(subject_id,teacher_id);
create index if not exists idx_classrooms_tenant_active_name on classrooms(tenant_id,active,name);
alter table teachers drop constraint if exists chk_teacher_status;
alter table teachers add constraint chk_teacher_status check (status in ('ACTIVE','INACTIVE','ON_LEAVE','ARCHIVED'));
alter table classrooms drop constraint if exists chk_classroom_capacity;
alter table classrooms add constraint chk_classroom_capacity check (capacity is null or capacity >= 0);
