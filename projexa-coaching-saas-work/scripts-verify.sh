#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
python3 - <<'PY'
from pathlib import Path
import re
root=Path('.')
errors=[]
for p in root.glob('services/api/src/main/java/**/*.java'):
    s=p.read_text()
    if s.count('{') != s.count('}'):
        errors.append(f"brace mismatch: {p}")
    m=re.search(r'public\s+(?:final\s+)?class\s+(\w+)', s)
    if m and m.group(1)+'.java' != p.name:
        errors.append(f"public class/file mismatch: {p}")
required=['tenants','users','roles','permissions','students','parents','teachers','attendance_sessions','attendance_records','exams','questions','exam_attempts','results','invoices','payments','notifications','automation_rules','automation_runs','outbox_events']
sql='\n'.join(p.read_text() for p in root.glob('services/api/src/main/resources/db/migration/*.sql'))
for table in required:
    if not re.search(r'CREATE TABLE\s+'+re.escape(table)+r'\b', sql, re.I): errors.append('missing table migration: '+table)
if errors:
    print('\n'.join(errors)); raise SystemExit(1)
print('Static source/migration checks: PASS')
PY
