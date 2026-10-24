"""Ensure live catalog checks cannot be pointed at production by accident."""
import importlib.util
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location("catalog_live",ROOT/"scripts/test_public_catalog_revocation_live.py")
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
def test_loopback_only_and_disposable_required():
    assert m.safe_dsn("postgresql://someone:pass@127.0.0.1:5432/reelnest")
    for dsn in ["postgresql://remote.example:5432/reelnest",
                "postgresql://127.0.0.1:5432/production",
                "postgresql://127.0.0.1:5433/reelnest",
                "file://127.0.0.1/reelnest",""]:
        assert not m.safe_dsn(dsn)
def test_no_opt_in_blocks_before_database_connection(monkeypatch):
    monkeypatch.delenv("REELNEST_TEST_DB_DSN",raising=False)
    monkeypatch.delenv("REELNEST_DISPOSABLE_DB",raising=False)
    assert m.main()==3
