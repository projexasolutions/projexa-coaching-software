-- Development bootstrap only. Replace/remove this migration before production provisioning.
INSERT INTO tenants(id,name,slug,status,timezone,contact_email) VALUES
('00000000-0000-0000-0000-000000000001','Demo Coaching Institute','demo','ACTIVE','Asia/Kolkata','owner@demo.projexa.local')
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles(id,tenant_id,name,code,description,system_role) VALUES
('00000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000001','Institute Owner','INSTITUTE_OWNER','Full institute administration',TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id)
SELECT '00000000-0000-0000-0000-000000000010', id FROM permissions
ON CONFLICT DO NOTHING;

INSERT INTO users(id,tenant_id,email,password_hash,first_name,last_name,status)
VALUES ('00000000-0000-0000-0000-000000000100','00000000-0000-0000-0000-000000000001','owner@demo.projexa.local','$2a$12$Y94GWq9AiyzdTnltQ9O..Ozxfb6kjBWmYF.5FZ3jFvXLphIjaHPbm','Demo','Owner','ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles(user_id,role_id) VALUES
('00000000-0000-0000-0000-000000000100','00000000-0000-0000-0000-000000000010')
ON CONFLICT DO NOTHING;
