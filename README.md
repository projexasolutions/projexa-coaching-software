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
