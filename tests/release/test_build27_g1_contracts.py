"""Build 27 contracts — offline preflight, not Docker or G1 acceptance."""
from pathlib import Path
import ast
import subprocess

ROOT = Path(__file__).resolve().parents[2]
WORKER = ROOT / "apps/video-worker"


def _local_modules(file_name: str, seen: set[str] | None = None) -> set[str]:
    seen = set() if seen is None else seen
    if file_name in seen:
        return seen
    seen.add(file_name)
    tree = ast.parse((WORKER / file_name).read_text())
    for node in ast.walk(tree):
        modules = []
        if isinstance(node, ast.ImportFrom) and node.module:
            modules = [node.module.split(".", 1)[0]]
        elif isinstance(node, ast.Import):
            modules = [item.name.split(".", 1)[0] for item in node.names]
        for module in modules:
            target = module + ".py"
            if (WORKER / target).is_file():
                _local_modules(target, seen)
    return seen


def test_worker_image_copies_transitive_python_imports():
    dockerfile = (WORKER / "Dockerfile").read_text()
    needed = _local_modules("worker.py")
    assert len(needed) >= 5, f"Unexpectedly small worker graph: {needed}"
    assert "COPY *.py ./" in dockerfile
    assert 'RUN python -c "import worker' in dockerfile
    assert "*.py" not in (WORKER / ".dockerignore").read_text().splitlines()
    assert "timed_text.py" in needed and "caption_packaging.py" in needed


def test_full_stack_acceptance_requires_original_asset_and_runtime_worker_import():
    script = (ROOT / "scripts/validate_local_stack.sh").read_text()
    assert "docker compose up --build -d" in script
    assert "test -s apps/web/public/sample/demo.mp4" in script
    assert "docker compose exec -T video-worker python -c" in script
    assert "COUNT(DISTINCT version)" in script and "=25" in script
    assert "python3 scripts/e2e_processing_live.py" in script
    assert "python3 scripts/e2e_playback_live.py" in script


def test_manual_ci_is_disposable_with_scoped_cleanup():
    workflow = (ROOT / ".github/workflows/manual-local-integration.yml").read_text()
    for marker in ["workflow_dispatch:", "COMPOSE_PROJECT_NAME: reelnest-ci-",
                   "docker compose down --volumes --remove-orphans",
                   "reelnest-ci-*", "if: always()", "contents: read",
                   "scripts/test_restore_disposable.sh",
                   "validate_local_stack.sh --full", "timeout-minutes:"]:
        assert marker in workflow, marker


def test_shell_scripts_parse_without_launching_containers():
    for path in ["scripts/validate_local_stack.sh", "scripts/test_restore_disposable.sh"]:
        proc = subprocess.run(["bash", "-n", str(ROOT / path)], capture_output=True, text=True)
        assert proc.returncode == 0, proc.stderr
