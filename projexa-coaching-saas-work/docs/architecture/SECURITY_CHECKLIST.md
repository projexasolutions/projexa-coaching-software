# Security Release Checklist

- [x] JWT is verified server-side.
- [x] Access tokens are short-lived.
- [x] Refresh tokens are stored hashed and rotated.
- [x] Tenant context comes from authenticated identity, not request body.
- [x] Method-level authorization exists.
- [x] Tenant-scoped repository patterns exist.
- [x] Payment status is backend-owned.
- [x] Payment idempotency index is defined.
- [x] Automation runs have event uniqueness.
- [x] Automation retries/cooldowns exist.
- [ ] Production secret rotation configured.
- [ ] Real provider credentials configured in secret manager.
- [ ] PostgreSQL RLS enabled for high-risk tables.
- [ ] Dependency/SAST/container scans executed in CI.
- [ ] Full E2E/security suite executed against deployed staging.
