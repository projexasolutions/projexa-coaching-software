-- Automation execution idempotency: one rule may execute once for a given tenant event.
CREATE UNIQUE INDEX IF NOT EXISTS uq_automation_run_tenant_rule_event
  ON automation_runs(tenant_id, rule_id, event_id);
