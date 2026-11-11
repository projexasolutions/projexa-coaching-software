# Projexa Coaching Management SaaS

Production-oriented multi-tenant coaching institute operating system built as a modular monolith.

## Stack

- Frontend: React + TypeScript + Vite + MUI + TanStack Query + Zustand
- API: Java 21 + Spring Boot 3.5 + Spring Security + JPA/JdbcTemplate
- Data: PostgreSQL + Flyway
- Async: RabbitMQ + transactional outbox boundary
- Cache: Redis
- Object storage: S3-compatible / MinIO boundary
- Deployment: Docker + Nginx + GitHub Actions

## Architecture

`apps/web` contains one role-aware frontend for Owner/Admin/Teacher/Student/Parent. `services/api` is a modular monolith with strict domain boundaries. Tenant identity comes from the authenticated JWT and is propagated through `TenantContext`.

Business events are designed for an outbox -> broker -> automation/notification workflow. Provider-specific payment, messaging and video integrations sit behind abstractions.

## Implemented domains

Authentication/RBAC, tenant isolation, academic configuration, students/parents/teachers, enrollments, attendance, timetable, homework, question bank, objective exams, automatic scoring, results, finance, notifications, communication, video-session abstraction, admissions CRM, support tickets, automation rules/runs, reports, AI-ready operational signals, audit/document/outbox infrastructure.

## Security rules

- Never accept a tenant ID from the browser as the source of authorization.
- Tenant context is derived from the authenticated identity.
- Student/parent/teacher portal access is resource-scoped server-side.
- Payment status is server-owned; frontend success is not trusted.
- Payment idempotency keys are unique per tenant.
- JWT secret is required through `JWT_SECRET`; production must not use a default secret.
- Do not commit `.env` files or credentials.

## Local development

1. Copy `.env.example` to `.env` and set `JWT_SECRET` to a random secret.
2. Start core infrastructure with `docker compose up -d`.
3. Start local S3-compatible object storage only when needed with `docker compose --profile storage up -d minio`.
4. Run the API with Maven from `services/api`.
5. Run the web app from `apps/web` with `npm install && npm run dev`.

Development seed credentials are documented in the migration and login screen. Replace them before any shared deployment.

## Verification

Run `./scripts-verify.sh` for source/migration static checks.

CI runs API tests and the frontend production build. The repository intentionally does not contain a generated `node_modules` directory or build artifacts.

## Production checklist

- Provision PostgreSQL, Redis, RabbitMQ and S3-compatible storage.
- Set strong secrets and provider credentials.
- Configure `FRONTEND_URL` to the exact production origin.
- Put the API behind TLS and a reverse proxy/load balancer.
- Configure backups, database migrations, logs, metrics and alerting.
- Implement/enable the concrete payment, email/SMS/WhatsApp/push and video providers before exposing those channels to customers.
- Run integration/E2E tests against real infrastructure before production release.

## Account email configuration

Institute signup verification, teacher/student/parent invitations, and password reset links require SMTP settings (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, and `MAIL_FROM`). Configure `SIGNUP_VERIFICATION_URL`, `INVITATION_ACCEPT_URL`, and `PASSWORD_RESET_URL` to the public frontend routes. Password reset links are single-use, expire after 30 minutes, and revoke existing refresh tokens after a successful reset. Reset requests return a generic response to avoid confirming whether an email belongs to an active account. SMTP must be configured and tested before enabling these flows for customers.

## Roles and demo-account safety

Institute owners and administrators with `roles.manage` can create custom roles from the Roles & Permissions workspace. A custom role can receive only permissions already held by the manager; platform permissions and role/user/institute-settings administration permissions are never assignable. Built-in roles are protected, and a custom role cannot be deleted while it is assigned to users.

The historical development migrations seed a predictable `demo` institute and demo credentials. The API now disables that tenant and its seeded owner/student accounts on startup unless `DEMO_ACCOUNTS_ENABLED=true` is explicitly set. Keep this disabled in production. For a fresh local development database only, opt in before starting the API. If the guard has already disabled an existing demo tenant, restore it deliberately in a local development database before enabling it; the guard does not automatically reactivate disabled accounts.

Public login, institute signup/verification, invitation acceptance and password-reset endpoints have a bounded per-IP, 15-minute in-process throttle with HTTP 429 and `Retry-After`. This is defense in depth for a single API process; production deployments with multiple API instances must also enforce a shared Redis or edge rate limit. The filter intentionally uses the socket peer address and does not trust caller-supplied `X-Forwarded-For` headers.
