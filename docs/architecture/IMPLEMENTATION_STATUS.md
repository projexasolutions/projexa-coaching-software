# Projexa Coaching SaaS — Implementation Status

## Implemented source boundaries
- Authentication: tenant-aware login, JWT access tokens, refresh rotation, logout, `/me`.
- Tenancy/RBAC: tenant context, roles, permissions, method security, tenant-scoped repositories.
- Academics: years, classes, streams, subjects, batches, classrooms.
- People: students, parents, enrollments, teachers and assignments.
- Operations: attendance, timetable, homework/submissions.
- Exams: question bank, objective attempts, answer grading, negative marking, result generation/publication, ranking and analysis.
- Finance: invoices, verified backend payment state, partial payments, idempotency, receipts schema and fee-attendance policy.
- Communication: conversations, messages, notifications, video session abstraction.
- Admissions: leads, pipeline stage movement, follow-ups schema.
- Support: tickets/status workflow.
- Automation: persisted rules/conditions/actions/runs, event polling through outbox, retries, cooldown, idempotency and test execution endpoint.
- Reports: dashboard, attendance, finance summary.
- Portal: student/parent-compatible student summary endpoint.
- AI-ready intelligence: deterministic operational risk signals with review-required semantics.
- Audit/outbox/documents/settings schemas.

## Explicit runtime caveat
This source archive was statically audited in the provided environment. Maven and Docker are unavailable here, and `npm install` timed out, so a complete dependency-backed compile, migration run, browser E2E run and container integration test could not be honestly certified in this environment.

Before production launch:
1. Start PostgreSQL/Redis/RabbitMQ/MinIO.
2. Set production secrets and remove the development seed migration.
3. Run Flyway migrations against a fresh database.
4. Run `mvn test` and `mvn verify` in a Java 21 + Maven environment.
5. Run `npm ci && npm run build` in the frontend.
6. Execute Playwright E2E flows for each role.
7. Configure real payment, email/SMS/WhatsApp, push and video providers behind the existing abstractions.
8. Enable object-storage signed URLs and production CORS.
9. Add PostgreSQL RLS as defense-in-depth for high-risk tenant tables.
10. Run load, security, backup/restore and disaster-recovery tests.
