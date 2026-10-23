"""Local unit tests for the prereq checker; no external tools or network needed."""
from pathlib import Path
import importlib.util

PATH = Path(__file__).resolve().parents[2] / "scripts" / "codex_preflight.py"
spec = importlib.util.spec_from_file_location("codex_preflight", PATH)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def test_detects_project_files(tmp_path):
    for p in ("compose.yaml", "apps/api/pom.xml", "apps/web/package.json", "scripts/release_gate.py", "AGENTS.md"):
        target = tmp_path / p
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text("sample")
    outcome = module.diagnose(tmp_path)
    assert all(outcome["project_files"].values())
    assert outcome["actual_build_and_runtime_results"] == "NOT_RUN_BY_PREFLIGHT"
    assert any("lockfile" in w for w in outcome["warnings"])


def test_missing_files_are_reported(tmp_path):
    outcome = module.diagnose(tmp_path)
    assert "compose_yaml" in outcome["missing_required"]
    assert "spring_pom" in outcome["missing_required"]
    assert not outcome["local_integration_ready_for_attempt"]
