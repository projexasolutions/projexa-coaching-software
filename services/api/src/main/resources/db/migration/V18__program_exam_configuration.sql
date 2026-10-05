CREATE TABLE programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80),
    category VARCHAR(80) NOT NULL DEFAULT 'GENERAL',
    level VARCHAR(80),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id,name)
);
CREATE TABLE program_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(tenant_id,program_id,subject_id)
);
ALTER TABLE batches ADD COLUMN program_id UUID REFERENCES programs(id);
CREATE INDEX idx_programs_tenant_active ON programs(tenant_id,active);
CREATE INDEX idx_program_subjects_program ON program_subjects(tenant_id,program_id);
CREATE INDEX idx_batches_program ON batches(tenant_id,program_id);

CREATE TABLE exam_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description TEXT,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    system_template BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id,code)
);
CREATE INDEX idx_exam_templates_visible ON exam_templates(active,system_template,tenant_id);

INSERT INTO exam_templates(id,tenant_id,name,code,category,description,config,system_template)
VALUES
(gen_random_uuid(),NULL,'JEE Main','JEE_MAIN','ENGINEERING','Engineering entrance template',
 '{"stages":["MAIN"],"subjects":["Physics","Chemistry","Mathematics"],"marking":{"correct":4,"incorrect":-1},"durationMinutes":180}',TRUE),
(gen_random_uuid(),NULL,'JEE Advanced','JEE_ADVANCED','ENGINEERING','Advanced engineering entrance template',
 '{"stages":["ADVANCED"],"subjects":["Physics","Chemistry","Mathematics"],"marking":{"configurable":true},"durationMinutes":180}',TRUE),
(gen_random_uuid(),NULL,'NEET UG','NEET','MEDICAL','Medical entrance template',
 '{"stages":["UG"],"subjects":["Physics","Chemistry","Botany","Zoology"],"marking":{"correct":4,"incorrect":-1},"durationMinutes":200}',TRUE),
(gen_random_uuid(),NULL,'UPSC Civil Services','UPSC_CSE','CIVIL_SERVICES','Prelims, Mains and Interview structure',
 '{"stages":["PRELIMS","MAINS","INTERVIEW"],"subjects":[],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'MHT-CET','MHT_CET','STATE_ENTRANCE','Maharashtra CET template',
 '{"stages":["PCM","PCB"],"subjects":["Physics","Chemistry","Mathematics","Biology"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'GATE','GATE','ENGINEERING','Graduate aptitude exam template',
 '{"stages":["EXAM"],"subjects":[],"branches":["CSE","ME","CE","ECE","EE"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'Banking Exams','BANKING','GOVERNMENT','IBPS, SBI and RBI banking preparation template',
 '{"stages":["PRELIMS","MAINS"],"subjects":["Quantitative Aptitude","Reasoning","English"],"marking":{"configurable":true},"analytics":["accuracy","speed"]}',TRUE),
(gen_random_uuid(),NULL,'SSC','SSC','GOVERNMENT','SSC preparation template',
 '{"stages":["TIER_1","TIER_2"],"subjects":["Quantitative Aptitude","Reasoning","English","General Awareness"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'MPSC','MPSC','GOVERNMENT','Maharashtra Public Service Commission template',
 '{"stages":["PRELIMS","MAINS"],"subjects":[],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'CAT','CAT','MANAGEMENT','MBA entrance template',
 '{"stages":["EXAM"],"subjects":["VARC","DILR","QA"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'CLAT','CLAT','LAW','Law entrance template',
 '{"stages":["UG"],"subjects":["English","Current Affairs","Legal Reasoning","Logical Reasoning","Quantitative Techniques"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'NDA','NDA','DEFENCE','National Defence Academy template',
 '{"stages":["WRITTEN","SSB"],"subjects":["Mathematics","General Ability Test"],"marking":{"configurable":true}}',TRUE),
(gen_random_uuid(),NULL,'CUET','CUET','UNIVERSITY_ENTRANCE','Common University Entrance Test template',
 '{"stages":["UG"],"subjects":[],"marking":{"configurable":true}}',TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO permissions(code,description) VALUES
('programs.read','View programs and exam templates'),
('programs.manage','Manage programs and exam configuration')
ON CONFLICT(code) DO NOTHING;
