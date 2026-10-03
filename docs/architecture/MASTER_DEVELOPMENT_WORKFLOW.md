# PROJEXA COACHING OS — MASTER DEVELOPMENT WORKFLOW

This is the canonical engineering workflow merging the existing repository plan with the latest Coaching OS product specification.

## Product North Star
**One platform to Manage, Teach, Test, Analyze, Engage & Grow a coaching institute.**

Supported configurations include JEE/JEE Advanced, NEET, MHT-CET/other CETs, UPSC/MPSC/State PSC, SSC, Banking, Railway, Defence, Teaching exams, CUET, CAT/GATE, CLAT/AILET, NIFT/NID/IPMAT and custom exams. Build one configurable platform, not separate exam products.

## Architecture — preserve
- Modular Monolith, Java 21 + Spring Boot 3.x
- PostgreSQL + Flyway
- Redis + RabbitMQ
- MinIO/S3 abstraction
- React + TypeScript + Vite + TanStack Query + MUI
- JWT access/refresh + tenant-aware RBAC
- REST /api/v1
- Transactional outbox/event-driven processing
- Docker + GitHub Actions

## Product layers
### MANAGE
Institute configuration, academic years, classes, streams, courses, subjects, batches, sections, classrooms, students, parents, teachers, enrollments, history, attendance, records and staff.

### TEACH
Faculty allocation, timetable, classes, syllabus progress, study materials, recorded lectures, assignments, submissions, evaluation, feedback, question bank and doubts.

### TEST
Universal configurable exam engine, question bank, objective exams, sectional/topic/chapter/full mocks, PYQs, negative marking, randomization, auto-evaluation and instant results.

### ANALYZE
Scores, ranks, percentile, accuracy, speed, subject/chapter/topic performance, weak topics, trends, batch analytics, faculty performance, risk detection and Institute Health Score.

### ENGAGE
Student/parent portals, internal communication, announcements, notifications, email/SMS/push, optional WhatsApp and video-session abstraction.

### GROW
Admissions CRM, counsellor pipeline, marketing-source analytics, conversion analytics, AI admission assistant and owner growth intelligence.

## Golden workflow
Institute → Course/Exam Profile → Batch → Student → Teacher → Classes → Attendance → Fees → Study Material → Assignments → Exams → Results → AI Performance Analysis → Parent Updates → Student Improvement → Institute Analytics → Admissions & Growth.

Every module must connect to the surrounding workflow; avoid isolated CRUD.

# IMPLEMENTATION PHASES

## PHASE 0 — FOUNDATION
**Status: implemented / hardening**
- Docker/local infrastructure
- PostgreSQL/Flyway
- Spring Boot API
- React/Vite shell + MUI
- JWT auth + refresh rotation
- Tenant context
- RBAC/permissions
- Audit/settings/outbox foundations

Hardening remains: production secrets, dev seed separation, RLS defense-in-depth, observability, backup/restore/DR.

## PHASE 1 — MANAGE / CORE OPERATIONS
**Status: in progress; core operational slices completed**

### Academic configuration — DONE
Years, classes, streams, subjects, batches, tenant isolation and lifecycle validation.

### Student operations — DONE
Student CRUD, admission validation, status lifecycle, operational view, enrollment, academic history and batch/class/stream/year consistency.

### Attendance Engine — DONE / CI GREEN
Sessions, validation, conflict protection, records, PRESENT/ABSENT/LATE/LEAVE/UNMARKED, bulk marking, summaries, batch percentage, student self-check-in and configurable fee-based restriction.

Latest verified CI: **run 37156464165 — success**, commit `ec988cd52307ff10fc28f40806babe24ca8ad4e0`.

### Next MANAGE work
1. Timetable Engine
2. Faculty/teacher operations
3. Classroom/resource conflict handling
4. Leave/substitution
5. Parent/student linking
6. Documents/digital records
7. Excel import/export and migration
8. Academic rollover/promotion
9. Institute settings/module toggles
10. Multi-branch foundation

## PHASE 2 — TEACH
### Faculty
Teacher profiles, subject/batch allocation, timetable, classes conducted, syllabus progress, faculty metrics.

### Learning
PDFs, notes, documents, videos, recordings, permissions and batch/subject/topic mapping.

### Assignments
Create → assign → submit → evaluate → marks → feedback, due dates, late policy and notifications.

### Doubts
Student → doubt → teacher queue → answer → notification.
Future: AI first-level assistance → teacher escalation.

## PHASE 3 — UNIVERSAL EXAM ENGINE
**Core platform capability; never build separate JEE/NEET/UPSC engines.**

### Exam profiles
JEE, NEET, CET, UPSC/MPSC/PSC, SSC, Banking, Railway, Defence, Teaching, CUET, CAT/GATE, CLAT/AILET, NIFT/NID/IPMAT and custom exams.

### Question bank
Subject, chapter, topic, difficulty, question type, exam category, PYQ tag, explanation and media.

### Objective engine
MCQ single/multiple correct, numerical, T/F, fill blank, match, assertion/reason, image-based, passage/case; sectional/chapter/topic/full mocks; PYQs; timer; negative marking; randomized questions/options; auto evaluation; instant results.

### Exam lifecycle
Create → select questions → configure pattern → assign batch → attempt → auto-evaluate → generate result → publish → rank → analyze.

## PHASE 4 — RESULTS & PERFORMANCE ANALYTICS
Student: score, %, rank, percentile, accuracy, attempt rate, speed, time/question, correct/wrong/unattempted.

Academic: subject/chapter/topic, weak/strong topics, improvement trend, previous-test comparison.

Institute: batch/class performance, pass %, top performers, weak areas, participation and trends.

Rank visibility must be configurable by owner/institute policy.

## PHASE 5 — AI ACADEMIC INTELLIGENCE
AI sits on top of deterministic source-of-truth data.

### Risk detection
Attendance decline, missed classes, declining scores, poor accuracy, missed tests, incomplete assignments, sustained underperformance.

### Weak-topic detection
Identify why a student is underperforming and recommend concrete next actions.

### AI Study Planner
Inputs: exam, target date, current score, weak subjects, study time, previous performance, test history.
Output: personalized daily/weekly plan.

### AI question generation
Generate candidate questions with difficulty/topic/exam tags. Teacher review is required before publication.

## PHASE 6 — OWNER INTELLIGENCE
### Institute Health Score
Composite /100 using attendance, fee collection, test participation, student/faculty performance, admissions conversion, revenue, dropout/risk and batch health.

### AI Coaching Director
Owner asks “How is my institute doing?” AI summarizes what changed, risks, causes and recommended actions. Recommendations must be traceable to underlying records and reviewable.

## PHASE 7 — ENGAGE / PORTALS
### Student
Today's classes, attendance, assignments, upcoming tests, results, study plan, performance, weak topics, notices and fees.
Journey: **Learn → Practice → Test → Analyze → Improve**.

### Parent
Attendance, fees, results, rank, performance, progress, assignments, notices and teacher communication.
Alerts for absence, fee due, result publication and performance decline.

### Communication
1:1, batch/group, announcements, notifications, internal history, email/SMS/push, optional WhatsApp and video abstraction.

## PHASE 8 — GROW / ADMISSIONS CRM
Pipeline:
**Enquiry → Contacted → Counselling → Demo → Follow-up → Admission → Converted**

Track student/parent, exam interest, course, source, counsellor, follow-up, status, conversion/lost reason.

Marketing analytics: Instagram, Google, website, walk-ins, referrals, campaigns and counsellor performance; leads, admissions, conversion and revenue attribution where available.

## PHASE 9 — AI ADMISSION ASSISTANT
FAQs, course explanation, batch recommendation, follow-up generation, high-intent detection and conversation summaries.
AI recommendations remain reviewable and automated outreach respects institute permissions/settings.

## PHASE 10 — MULTI-BRANCH
Central owner sees branch students, revenue, attendance, results, performance, comparisons, branch admins and central reports. Branch boundaries are enforced server-side.

## PHASE 11 — AUTOMATION
**Event → Rule → Conditions → Actions → Notification/Workflow → Audit**

Examples: overdue fee reminder, payment receipt, absence alert, result notification, risk task, admission follow-up.

Controls: idempotency, retries, cooldown, deduplication, loop/depth protection, audit, test mode and owner approval for AI-suggested rules.

## PHASE 12 — FINANCE HARDENING
Fee plans, installments, invoices, discounts, scholarships, partial payments, refunds, receipts, outstanding, reports, expenses and payroll.

Payment lifecycle:
Fee Plan → Installment → Invoice → Razorpay/UPI/QR/manual → backend verification → receipt → ledger → notification → automation.

Frontend payment success is never payment truth.

## PHASE 13 — SECURITY / PRODUCTION
Tenant-isolation tests, resource authorization, RBAC tests, secure files, rate limits, audit completeness, refresh rotation, secret management, PostgreSQL RLS defense-in-depth, CORS, signed object URLs, backup/restore, DR, load and security tests.

## PHASE 14 — QUALITY GATES
Every major module requires:
1. Unit tests
2. Service tests
3. Controller/security tests
4. Tenant-isolation tests
5. Migration validation
6. API integration tests
7. Frontend build
8. Playwright E2E
9. Role-based workflow validation

**Definition of done = Code + migration + API + UI + authorization + tenant isolation + tests + CI green.**

# CURRENT EXECUTION QUEUE

Completed operational slices:
- Foundation
- Authentication/RBAC hardening
- Academic configuration
- Student/enrollment operations
- Attendance Engine

Next:
**1 Timetable → 2 Faculty → 3 Learning Resources → 4 Assignments → 5 Universal Exam Engine → 6 Results/Analytics → 7 AI Academic Intelligence → 8 Student/Parent Portals → 9 Admissions CRM → 10 Marketing Analytics → 11 AI Admission Assistant → 12 Multi-branch → 13 Production Hardening**

Do not jump to AI before the underlying academic/exam data is reliable.

# NON-NEGOTIABLE PRINCIPLES
- One configurable Coaching OS, not separate exam products.
- Backend is the source of truth.
- Tenant-owned data is tenant-scoped.
- Sensitive endpoints are authorization-protected.
- AI is explainable/reviewable.
- Payments are server-verified.
- Automation is idempotent/auditable.
- Workflows must connect across modules.
- Owner-configurable behavior must be represented as configuration.
- Build reusable engines, not exam-specific implementations.
- Keep modular-monolith boundaries until extraction is justified.
- Never call a module production-ready just because its UI exists.

## END STATE
**MANAGE → TEACH → TEST → ANALYZE → ENGAGE → GROW**

The goal is a true coaching-institute operating system, not a collection of dashboards.
