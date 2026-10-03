# Final Engineering Status

## Completed in source

- Modular monolith Spring Boot API
- Tenant context derived from JWT
- JWT access/refresh authentication with refresh-token rotation
- RBAC + permission authorities
- Resource-level student/parent/teacher authorization
- Academic configuration and student operations
- Attendance with configurable fee restriction and automation restriction table
- Timetable/homework foundations
- Question bank and objective exam attempt/scoring flow
- Result/ranking foundations
- Finance with backend-owned payment state and tenant idempotency
- Internal notifications and communication membership checks
- Video session provider boundary
- Admissions CRM and support
- Automation rules, conditions, action handlers, cooldowns and run idempotency
- Transactional outbox storage and RabbitMQ publishing/automation consumption
- Audit/document/settings tables
- Reports and AI-ready operational signals
- Dockerfiles, local infrastructure compose, production compose example
- CI for API tests and frontend build

## Verified in this environment

- Java 21 is installed.
- Static Java brace/class-name checks pass.
- Migration/table presence checks pass.
- Frontend routing duplication was corrected.
- Stale client-controlled sender/creator/host identifiers were removed from sensitive endpoints.

## Not runtime-verified here

- Full Maven test execution: Maven is not installed in the current environment.
- Full Docker integration tests: Docker is not installed in the current environment.
- Frontend production build: npm dependency installation could not complete because registry access timed out in this environment.
- External providers: Razorpay, WhatsApp, SMS/email, push and video providers require real credentials/provider implementations.

These are environment/provider gates, not represented as successful tests.
