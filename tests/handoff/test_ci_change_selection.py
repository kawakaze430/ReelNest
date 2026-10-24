"""Execute CI's real change selector so essential integration changes cannot skip G1."""
from pathlib import Path
import subprocess

import pytest
import yaml


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / '.github/workflows/checks.yml'


def selection_program():
    workflow = yaml.load(WORKFLOW.read_text(), Loader=yaml.BaseLoader)
    step = next(step for step in workflow['jobs']['changes']['steps'] if step.get('id') == 'scope')
    lines = step['run'].splitlines()
    assert lines[0] == "python3 - <<'PY'" and lines[-1] == 'PY'
    return '\n'.join(lines[1:-1])


def selected(paths, tmp_path, monkeypatch, event='push', base='a' * 40):
    output = tmp_path / 'github-output'
    monkeypatch.setenv('BASE_SHA', base)
    monkeypatch.setenv('EVENT_NAME', event)
    monkeypatch.setenv('GITHUB_OUTPUT', str(output))
    calls = []

    def changed_files(command, *, text):
        assert command == ['git', 'diff', '--name-only', base, 'HEAD']
        assert text is True
        calls.append(command)
        return '\n'.join(paths)

    monkeypatch.setattr(subprocess, 'check_output', changed_files)
    exec(compile(selection_program(), str(WORKFLOW), 'exec'), {})
    return output.read_text().strip(), calls


@pytest.mark.parametrize('path', [
    'apps/api/src/main/java/com/reelnest/ReelNestApplication.java',
    'apps/web/src/components/protected-player.tsx',
    'apps/video-worker/Dockerfile',
    'apps/web/.dockerignore',
    'compose.yaml',
    'infrastructure/minio/Dockerfile',
    'infrastructure/keycloak/reelnest-realm.json',
    'scripts/e2e_processing_live.py',
    'scripts/live_e2e_helpers.py',
    'scripts/live_smoke.py',
    'scripts/security_smoke_live.sh',
    'scripts/test_restore_disposable.sh',
    'scripts/verify_flyway_postgres.py',
    'scripts/validate_local_stack.sh',
    'scripts/cloud_evidence.py',
    '.github/workflows/checks.yml',
    '.github/workflows/manual-local-integration.yml',
])
def test_runtime_or_integration_change_enables_full_g1(path, tmp_path, monkeypatch):
    output, calls = selected([path], tmp_path, monkeypatch)
    assert output == 'integration=true'
    assert len(calls) == 1


@pytest.mark.parametrize('paths', [
    [],
    ['README.md', 'docs/BUILD29_STATUS.md'],
    ['docs/BUILD28_282_TASKS.csv'],
    ['infrastructure/aws/services.tf'],
    ['composeXyaml', '.github/workflows/checksXyml'],
    ['archive/apps/api/Dockerfile', 'scripts/not_e2e.py'],
])
def test_documentation_or_unrelated_change_avoids_duplicate_g1(paths, tmp_path, monkeypatch):
    output, calls = selected(paths, tmp_path, monkeypatch)
    assert output == 'integration=false'
    assert len(calls) == 1


def test_mixed_change_list_runs_g1_when_any_runtime_path_changes(tmp_path, monkeypatch):
    output, _ = selected(['docs/BUILD29_STATUS.md', 'scripts/live_e2e_helpers.py'], tmp_path, monkeypatch)
    assert output == 'integration=true'


@pytest.mark.parametrize('event,base', [('workflow_dispatch', 'a' * 40), ('push', ''), ('push', '0' * 40)])
def test_manual_or_initial_run_does_not_depend_on_unavailable_diff(event, base, tmp_path, monkeypatch):
    output, calls = selected([], tmp_path, monkeypatch, event=event, base=base)
    assert output == 'integration=true'
    assert calls == []
