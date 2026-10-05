-- Automation execution idempotency: one rule may execute once for a given tenant event.
-- Remove legacy duplicates before enforcing the unique invariant.
WITH ranked AS (
  SELECT id,
         row_number() OVER (
           PARTITION BY tenant_id, rule_id, event_id
           ORDER BY coalesce(completed_at, started_at) DESC NULLS LAST, id DESC
         ) AS rn
  FROM automation_runs
  WHERE event_id IS NOT NULL
)
DELETE FROM automation_runs ar
USING ranked r
WHERE ar.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_automation_run_tenant_rule_event
  ON automation_runs(tenant_id, rule_id, event_id)
  WHERE event_id IS NOT NULL;
