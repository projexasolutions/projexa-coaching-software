-- LOCAL DEVELOPMENT ONLY.
-- Run manually against the local coaching database after Flyway migrations.
-- Never execute this script against staging or production.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
  tenant UUID := '00000000-0000-0000-0000-000000000001';
  owner_user UUID := '00000000-0000-0000-0000-000000000100';
  teacher_user UUID := '00000000-0000-0000-0000-000000000102';
  parent_user UUID := '00000000-0000-0000-0000-000000000103';
  admin_user UUID := '00000000-0000-0000-0000-000000000104';
  accountant_user UUID := '00000000-0000-0000-0000-000000000105';
  platform_user UUID := '00000000-0000-0000-0000-000000000106';
  teacher_id UUID := '61000000-0000-0000-0000-000000000001';
  parent_id UUID := '62000000-0000-0000-0000-000000000001';
BEGIN
  IF NOT EXISTS (SELECT 1 FROM tenants WHERE id=tenant AND slug='demo' AND status='ACTIVE') THEN
    RAISE EXCEPTION 'Expected active demo tenant. Run Flyway migrations first.';
  END IF;

  -- Roles
  INSERT INTO roles(id,tenant_id,name,code,description,system_role) VALUES
    ('00000000-0000-0000-0000-000000000012',tenant,'Institute Admin','INSTITUTE_ADMIN','Institute operations administrator',TRUE),
    ('00000000-0000-0000-0000-000000000013',tenant,'Accountant','ACCOUNTANT','Fees and finance operations',TRUE),
    ('00000000-0000-0000-0000-000000000014',tenant,'Teacher','TEACHER','Assigned batches and academic work',TRUE),
    ('00000000-0000-0000-0000-000000000015',tenant,'Parent','PARENT','Linked student guardian portal',TRUE)
  ON CONFLICT (id) DO NOTHING;

  -- Institute admin receives institute permissions, never platform permissions.
  INSERT INTO role_permissions(role_id,permission_id)
  SELECT '00000000-0000-0000-0000-000000000012',p.id
  FROM permissions p
  WHERE p.code NOT LIKE 'platform.%'
  ON CONFLICT DO NOTHING;

  INSERT INTO role_permissions(role_id,permission_id)
  SELECT '00000000-0000-0000-0000-000000000013',p.id
  FROM permissions p
  WHERE p.code IN ('dashboard.read','fees.read','fees.create','finance.manage','reports.read')
  ON CONFLICT DO NOTHING;

  INSERT INTO role_permissions(role_id,permission_id)
  SELECT '00000000-0000-0000-0000-000000000014',p.id
  FROM permissions p
  WHERE p.code IN ('dashboard.read','students.read','teachers.read','attendance.read','attendance.mark',
                   'exams.read','exams.create','exams.manage','questions.manage','results.read','results.manage',
                   'reports.read')
  ON CONFLICT DO NOTHING;

  -- Demo accounts. Existing Owner and Student credentials are preserved.
  INSERT INTO users(id,tenant_id,email,password_hash,first_name,last_name,status) VALUES
    (admin_user,tenant,'admin@demo.projexa.local',crypt('Admin@12345',gen_salt('bf',12)),'Demo','Admin','ACTIVE'),
    (accountant_user,tenant,'accountant@demo.projexa.local',crypt('Accountant@12345',gen_salt('bf',12)),'Demo','Accountant','ACTIVE'),
    (teacher_user,tenant,'teacher@demo.projexa.local',crypt('Teacher@12345',gen_salt('bf',12)),'Demo','Teacher','ACTIVE'),
    (parent_user,tenant,'parent@demo.projexa.local',crypt('Parent@12345',gen_salt('bf',12)),'Demo','Parent','ACTIVE'),
    (platform_user,tenant,'platform@demo.projexa.local',crypt('Platform@12345',gen_salt('bf',12)),'Projexa','Platform Admin','ACTIVE')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO user_roles(user_id,role_id) VALUES
    (admin_user,'00000000-0000-0000-0000-000000000012'),
    (accountant_user,'00000000-0000-0000-0000-000000000013'),
    (teacher_user,'00000000-0000-0000-0000-000000000014'),
    (parent_user,'00000000-0000-0000-0000-000000000015'),
    (platform_user,'00000000-0000-0000-0000-000000000020')
  ON CONFLICT DO NOTHING;

  -- Ensure the owner is not also a platform administrator.
  DELETE FROM user_roles WHERE user_id=owner_user AND role_id='00000000-0000-0000-0000-000000000020';
  DELETE FROM platform_admins WHERE user_id=owner_user;

  INSERT INTO platform_admins(user_id) VALUES(platform_user)
  ON CONFLICT (user_id) DO UPDATE SET status='ACTIVE';

  -- Link teacher to the demo's 12-A batch.
  INSERT INTO teachers(id,tenant_id,user_id,employee_code,first_name,last_name,email,status)
  VALUES(teacher_id,tenant,teacher_user,'DEMO-TCH-01','Demo','Teacher','teacher@demo.projexa.local','ACTIVE')
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO teacher_batch_assignments(teacher_id,batch_id)
  VALUES(teacher_id,'50000000-0000-0000-0000-000000000001')
  ON CONFLICT DO NOTHING;

  -- Link parent to demo student Aarav Sharma.
  INSERT INTO parents(id,tenant_id,first_name,last_name,email,phone)
  VALUES(parent_id,tenant,'Demo','Parent','parent@demo.projexa.local','9000000001')
  ON CONFLICT (id) DO NOTHING;
  UPDATE parents SET user_id=parent_user WHERE id=parent_id;
  INSERT INTO student_parents(student_id,parent_id,relation,is_primary)
  VALUES('60000000-0000-0000-0000-000000000001',parent_id,'FATHER',TRUE)
  ON CONFLICT DO NOTHING;
END $$;

-- Quick verification (run after the script):
-- SELECT u.email, r.code FROM users u JOIN user_roles ur ON ur.user_id=u.id
-- JOIN roles r ON r.id=ur.role_id WHERE u.email LIKE '%@demo.projexa.local' ORDER BY u.email,r.code;
