-- Timetable Engine hardening: conflict-safe lookup indexes and resource integrity.
CREATE INDEX IF NOT EXISTS idx_timetable_tenant_day_time
  ON timetable_entries(tenant_id,day_of_week,start_time,end_time);
CREATE INDEX IF NOT EXISTS idx_timetable_batch_day
  ON timetable_entries(tenant_id,batch_id,day_of_week,start_time);
CREATE INDEX IF NOT EXISTS idx_timetable_teacher_day
  ON timetable_entries(tenant_id,teacher_id,day_of_week,start_time)
  WHERE teacher_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_timetable_classroom_day
  ON timetable_entries(tenant_id,classroom_id,day_of_week,start_time)
  WHERE classroom_id IS NOT NULL;

ALTER TABLE timetable_entries
  ADD CONSTRAINT chk_timetable_day_of_week
  CHECK (day_of_week BETWEEN 1 AND 7);

ALTER TABLE timetable_entries
  ADD CONSTRAINT chk_timetable_time_range
  CHECK (end_time > start_time);
