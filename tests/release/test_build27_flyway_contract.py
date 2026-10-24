"""Pure Flyway contract tests; real PostgreSQL is checked separately."""
import importlib.util
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("flyway_postgres", ROOT / "scripts/verify_flyway_postgres.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def test_exact_v1_to_v27_local_migrations():
    assert module.expected_versions() == list(range(1, 28))


def test_migration_history_rejects_gaps_failed_baselines_and_extras():
    good = [(str(n), True) for n in range(1, 28)]
    assert module.validate_history(good) == 27
    invalid = [
        good[:-1], good + [("28", True)], good[1:] + good[:1],
        good[:6] + [("6", True)] + good[7:],
        good[:10] + [("11", False)] + good[11:],
        good[:10] + [(None, True)] + good[11:],
    ]
    for history in invalid:
        with pytest.raises(AssertionError):
            module.validate_history(history)


def test_verifier_requires_disposable_opt_in_and_never_prints_dsn(monkeypatch, capsys):
    monkeypatch.delenv("REELNEST_TEST_DB_DSN", raising=False)
    assert module.main() == 3
    monkeypatch.setenv("REELNEST_TEST_DB_DSN", "postgresql://someone:neverprint@example.invalid/db")
    monkeypatch.delenv("REELNEST_DISPOSABLE_DB", raising=False)
    assert module.main() == 3
    messages = capsys.readouterr()
    assert "neverprint" not in messages.out + messages.err
