#!/usr/bin/env python3
"""Verify real disposable PostgreSQL Flyway V1–V26; NOT complete G1 acceptance."""
from __future__ import annotations
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "apps/api/src/main/resources/db/migration"


def expected_versions():
    versions = sorted(int(p.name.split("__", 1)[0][1:]) for p in MIGRATIONS.glob("V*__*.sql"))
    if versions != list(range(1, 27)):
        raise AssertionError(f"Expected contiguous migration files V1–V26, got {versions}")
    return versions


def validate_history(rows, expected=None):
    """Reject failed, out-of-order, duplicate, baseline and untracked migrations."""
    expected = expected_versions() if expected is None else list(expected)
    if len(rows) != len(expected):
        raise AssertionError(f"History has {len(rows)} rows; expected {len(expected)}")
    seen = []
    for version, success in rows:
        if version is None or success is not True:
            raise AssertionError("Flyway baseline/failed migration exists")
        try:
            seen.append(int(version))
        except (TypeError, ValueError) as exc:
            raise AssertionError("Unexpected Flyway migration version") from exc
    if seen != expected:
        raise AssertionError(f"Flyway order mismatch: {seen}, expected {expected}")
    return len(seen)


def main():
    dsn = os.getenv("REELNEST_TEST_DB_DSN", "")
    if not dsn:
        print("BLOCKED: set REELNEST_TEST_DB_DSN to a disposable PostgreSQL database", file=sys.stderr)
        return 3
    if os.getenv("REELNEST_DISPOSABLE_DB") != "true":
        print("BLOCKED: REELNEST_DISPOSABLE_DB=true is required", file=sys.stderr)
        return 3
    import psycopg
    with psycopg.connect(dsn, connect_timeout=8, options="-c statement_timeout=15000") as conn:
        rows = conn.execute("SELECT version,success FROM flyway_schema_history ORDER BY installed_rank").fetchall()
    count = validate_history(rows)
    print(f"PASS actual disposable PostgreSQL Flyway V1–V{count} installed in order")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
