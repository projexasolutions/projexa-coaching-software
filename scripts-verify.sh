#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

python3 - <<'PY'
from pathlib import Path
import re

root = Path('.')
errors = []

java_files = list(root.glob('services/api/src/main/java/**/*.java'))
for p in java_files:
    s = p.read_text()
    if s.count('{') != s.count('}'):
        errors.append(f"brace mismatch: {p}")
    if s.count('(') != s.count(')'):
        errors.append(f"parenthesis mismatch: {p}")
    m = re.search(r'public\s+(?:final\s+)?class\s+(\w+)', s)
    if m and m.group(1) + '.java' != p.name:
        errors.append(f"public class/file mismatch: {p}")

migration_files = sorted(root.glob('services/api/src/main/resources/db/migration/*.sql'))
versions = {}
for p in migration_files:
    m = re.match(r'V(\d+)__.+\.sql$', p.name)
    if not m:
        errors.append(f"invalid migration filename: {p}")
        continue
    version = int(m.group(1))
    if version in versions:
        errors.append(f"duplicate migration version V{version}: {versions[version]} and {p}")
    versions[version] = p

if versions:
    expected = list(range(1, max(versions) + 1))
    missing = [v for v in expected if v not in versions]
    if missing:
        errors.append("missing migration versions: " + ", ".join(f"V{v}" for v in missing))

required_tables = [
    'tenants','users','roles','permissions','students','parents','teachers',
    'attendance_sessions','attendance_records','exams','questions','exam_attempts',
    'results','invoices','payments','notifications','automation_rules',
    'automation_runs','outbox_events'
]
sql = '\n'.join(p.read_text() for p in migration_files)
for table in required_tables:
    if not re.search(r'CREATE TABLE\s+' + re.escape(table) + r'\b', sql, re.I):
        errors.append('missing table migration: ' + table)

defined_permissions = set(re.findall(r"['\"]([a-zA-Z0-9_.-]+)['\"]\s*,\s*['\"]", sql))
used_permissions = set()
for p in java_files:
    s = p.read_text()
    used_permissions.update(re.findall(r"hasAuthority\('([^']+)'\)", s))
    used_permissions.update(re.findall(r"hasAuthority\(\"([^\"]+)\"\)", s))
missing_permissions = sorted(x for x in used_permissions if x not in defined_permissions)
for permission in missing_permissions:
    errors.append(f"permission referenced by @PreAuthorize but not defined in migrations: {permission}")

if errors:
    print('\n'.join(errors))
    raise SystemExit(1)

print('Static source/migration checks: PASS')
PY
