-- Development/demo student account for exercising the student portal.
-- Scoped exclusively to the existing demo tenant and demo student.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO roles(id,tenant_id,name,code,description,system_role)
VALUES (
  '00000000-0000-0000-0000-000000000011',
  '00000000-0000-0000-0000-000000000001',
  'Student',
  'STUDENT',
  'Student portal access',
  TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users(id,tenant_id,email,password_hash,first_name,last_name,status)
VALUES (
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000001',
  'student@demo.projexa.local',
  crypt('Student@12345',gen_salt('bf',12)),
  'Aarav',
  'Sharma',
  'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles(user_id,role_id)
VALUES (
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000011'
)
ON CONFLICT DO NOTHING;

UPDATE students
SET user_id='00000000-0000-0000-0000-000000000101'
WHERE id='60000000-0000-0000-0000-000000000001'
  AND tenant_id='00000000-0000-0000-0000-000000000001';
