# Local demo role accounts

This folder contains **manual, local-development-only** seed data. These scripts are intentionally outside Flyway's migration directory and must never be run against staging or production.

## Prerequisites

1. Start the local PostgreSQL service and apply the normal Flyway migrations.
2. Confirm the local database contains the active `demo` tenant.
3. Run the seed script against that local database only.

Example for the local Docker setup when PostgreSQL is mapped to port 5433:

```bash
psql "postgresql://postgres:postgres@localhost:5433/coaching" -v ON_ERROR_STOP=1 -f services/api/scripts/dev/seed-demo-roles.sql
```

If your local database uses a different port, database name, or credentials, adjust the connection string. Do not put production credentials in shell history.

## Demo login accounts

All accounts use tenant slug: `demo`.

| Role | Email | Development password |
|---|---|---|
| Institute Owner | owner@demo.projexa.local | Demo@12345 |
| Institute Admin | admin@demo.projexa.local | Admin@12345 |
| Accountant | accountant@demo.projexa.local | Accountant@12345 |
| Teacher | teacher@demo.projexa.local | Teacher@12345 |
| Student | student@demo.projexa.local | Student@12345 |
| Parent | parent@demo.projexa.local | Parent@12345 |
| Platform Admin | platform@demo.projexa.local | Platform@12345 |

The script preserves the existing Owner and Student passwords. It creates the missing demo users, links the Teacher to the demo 12-A batch, links the Parent to Aarav Sharma, and moves the Platform Admin assignment off the Owner account.

## Important security notes

- These are predictable demo credentials. Never expose them on a public or production deployment.
- Rotate or disable all demo accounts before any public launch.
- This seed establishes role and permission assignments, but does not by itself guarantee that every frontend route or backend endpoint enforces every permission. Verify method-level authorization and add UI permission guards before production.
- The existing V20 Flyway migration originally assigned the platform role to the demo Owner. Running this local seed corrects that local database state. Do not rewrite already-applied Flyway migrations.
