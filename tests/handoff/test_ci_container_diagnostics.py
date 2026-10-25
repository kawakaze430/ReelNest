"""Exercise the actual diagnostics entrypoint without Docker or remote requests."""
import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace

import pytest


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def diagnostics(monkeypatch):
    monkeypatch.syspath_prepend(str(ROOT / 'scripts'))
    spec = importlib.util.spec_from_file_location('ci_diagnostics', ROOT / 'scripts/ci_container_diagnostics.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', 'true')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-ci-unit')
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    return module


def response(stdout='', stderr='', code=0):
    return SimpleNamespace(stdout=stdout, stderr=stderr, returncode=code)


def test_diagnostics_refuse_unconfirmed_project_before_docker(diagnostics, monkeypatch):
    monkeypatch.delenv('REELNEST_DISPOSABLE_STACK')
    monkeypatch.setattr(diagnostics, 'compose', lambda *args: pytest.fail('Must refuse before Docker'))
    with pytest.raises(RuntimeError, match='BLOCKED'):
        diagnostics.main()


@pytest.mark.parametrize('ndjson', [False, True])
def test_only_failed_services_are_read_and_annotations_remove_credentials(diagnostics, monkeypatch, capsys, ndjson):
    rows = [{'Service': 'minio-init', 'State': 'exited', 'ExitCode': 1, 'Health': ''},
            {'Service': 'api', 'State': 'running', 'ExitCode': 0},
            {'Service': 'init-ok', 'State': 'exited', 'ExitCode': 0},
            {'Service': '--unexpected-option', 'State': 'exited', 'ExitCode': 1}]
    commands = []
    def fake_compose(*args):
        commands.append(args)
        if args[0] == 'ps':
            return response('\n'.join(map(json.dumps, rows)) if ndjson else json.dumps(rows))
        assert args == ('logs', '--no-color', '--tail', '40', 'minio-init')
        return response('setup failed\nsecret=synthetic-secret\nhttps://storage.invalid/object?sig=synthetic-sas\n::error::nested')
    monkeypatch.setattr(diagnostics, 'compose', fake_compose)
    diagnostics.main()
    output = capsys.readouterr().out
    assert output.startswith('::error') and 'minio-init' in output
    assert 'synthetic-secret' not in output and 'synthetic-sas' not in output
    assert len(output.splitlines()) == 1, 'Log lines must not inject workflow commands'
    assert len(commands) == 2


def test_running_readiness_logs_are_diagnostics_without_a_failure_claim(diagnostics, monkeypatch, capsys):
    calls = []
    def fake_compose(*args):
        calls.append(args)
        return response(json.dumps([{'Service': 'api', 'State': 'running', 'ExitCode': 0}])) if args[0] == 'ps' else response('startup information')
    monkeypatch.setattr(diagnostics, 'compose', fake_compose)
    diagnostics.main()
    output = capsys.readouterr().out
    assert output.startswith('::warning') and 'readiness diagnostics' in output
    assert calls[-1][-1] == 'api'


def test_subprocess_uses_only_scoped_compose_arguments(diagnostics, monkeypatch):
    def fake_run(command, **kwargs):
        assert command == ['docker', 'compose', 'logs', '--no-color', '--tail', '40', 'minio-init']
        assert kwargs == {'capture_output': True, 'text': True, 'timeout': 25, 'check': False}
        return response()
    monkeypatch.setattr(diagnostics.subprocess, 'run', fake_run)
    diagnostics.compose('logs', '--no-color', '--tail', '40', 'minio-init')


@pytest.mark.parametrize('metadata', ['not-json', '[null]', '[42]'])
def test_bad_metadata_reports_only_safe_warning(diagnostics, monkeypatch, capsys, metadata):
    monkeypatch.setattr(diagnostics, 'compose', lambda *args: response(metadata))
    diagnostics.main()
    output = capsys.readouterr().out
    assert output.startswith('::warning') and 'diagnostics unavailable' in output


def test_missing_docker_does_not_break_report_collection(diagnostics, monkeypatch, capsys):
    def unavailable(*args):
        raise FileNotFoundError('synthetic-secret')
    monkeypatch.setattr(diagnostics, 'compose', unavailable)
    diagnostics.main()
    output = capsys.readouterr().out
    assert output.startswith('::warning') and 'FileNotFoundError' in output
    assert 'synthetic-secret' not in output
