"""Safety and failure propagation for real integration entrypoints; no live services."""
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

import pytest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('live_e2e_helpers', ROOT / 'scripts/live_e2e_helpers.py')
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)


def executable(path, content):
    path.write_text('#!/usr/bin/env bash\n' + content)
    path.chmod(0o755)


def sandbox_script(tmp_path, name, docker):
    (tmp_path / 'scripts').mkdir()
    (tmp_path / 'bin').mkdir()
    shutil.copyfile(ROOT / 'scripts' / name, tmp_path / 'scripts' / name)
    if name == 'test_restore_disposable.sh':
        (tmp_path / 'scripts/advanced').mkdir()
        shutil.copyfile(ROOT / 'scripts/advanced/recovery_objectives.py', tmp_path / 'scripts/advanced/recovery_objectives.py')
    calls = tmp_path / 'docker-calls'
    executable(tmp_path / 'bin/docker', 'printf "%s\\n" "$*" >> "$TEST_DOCKER_CALLS"\n' + docker)
    executable(tmp_path / 'bin/git', "printf '%040d\\n' 1\n")
    env = dict(os.environ, PATH=str(tmp_path / 'bin') + os.pathsep + os.environ['PATH'],
               TEST_DOCKER_CALLS=str(calls), COMPOSE_PROJECT_NAME='reelnest-test-unit',
               REELNEST_DISPOSABLE_STACK='true')
    return tmp_path / 'scripts' / name, env, calls


def test_full_validation_refuses_unconfirmed_stack_before_docker(tmp_path):
    script, env, calls = sandbox_script(tmp_path, 'validate_local_stack.sh', 'exit 0\n')
    env.pop('REELNEST_DISPOSABLE_STACK')
    result = subprocess.run(['bash', str(script), '--full'], env=env, capture_output=True, text=True)
    assert result.returncode == 3
    assert not calls.exists(), 'Docker must not be contacted before disposable-stack confirmation'


def test_validator_records_underlying_failed_build_and_stops(tmp_path):
    script, env, calls = sandbox_script(tmp_path, 'validate_local_stack.sh', '''
case "$*" in
  info|"compose config -q") exit 0 ;;
  "compose up --build -d") exit 42 ;;
  *) exit 99 ;;
esac
''')
    result = subprocess.run(['bash', str(script), '--smoke'], env=env, capture_output=True, text=True)
    assert result.returncode == 42, result.stdout + result.stderr
    report = json.loads((tmp_path / 'build-evidence/local-stack-latest.json').read_text())
    assert report['status'] == 'FAIL' and report['exitCode'] == 42
    assert report['steps'][-1] == {'step': 'service image build and startup', 'status': 'FAIL', 'exitCode': 42}
    assert 'psql' not in calls.read_text(), 'No downstream check may run after failed startup'


def test_restore_never_drops_database_when_createdb_failed(tmp_path):
    script, env, calls = sandbox_script(tmp_path, 'test_restore_disposable.sh', '''
case " $* " in
  *" pg_isready "*) exit 0 ;;
  *" pg_dump "*) printf 'test-only archive'; exit 0 ;;
  *" createdb "*) exit 9 ;;
  *) exit 99 ;;
esac
''')
    result = subprocess.run(['bash', str(script)], env=env, capture_output=True, text=True)
    assert result.returncode == 9, result.stdout + result.stderr
    assert 'dropdb' not in calls.read_text(), 'A pre-existing or unowned scratch DB must never be deleted'
    report = json.loads((tmp_path / 'build-evidence/restore-latest.json').read_text())
    assert report['status'] == 'FAIL' and report['exitCode'] == 9
    assert report['scratchCleanup'] == 'not_created'
    assert report['checksVerified'] is False


def test_restore_cleanup_failure_cannot_report_pass(tmp_path):
    script, env, calls = sandbox_script(tmp_path, 'test_restore_disposable.sh', '''
case " $* " in
  *" pg_isready "*|*" createdb "*|*" pg_restore "*) exit 0 ;;
  *" pg_dump "*) printf 'test-only archive'; exit 0 ;;
  *" psql "*) printf '5\\n'; exit 0 ;;
  *" dropdb "*) exit 7 ;;
  *) exit 99 ;;
esac
''')
    result = subprocess.run(['bash', str(script)], env=env, capture_output=True, text=True)
    assert result.returncode == 1, result.stdout + result.stderr
    report = json.loads((tmp_path / 'build-evidence/restore-latest.json').read_text())
    assert report['status'] == 'FAIL' and report['scratchCleanup'] == 'failed'
    assert report['checksVerified'] is True
    assert 'reelnest_restore_check_' in calls.read_text()


def test_fixture_requires_successful_unique_processing_result(tmp_path, monkeypatch):
    monkeypatch.setenv("COMPOSE_PROJECT_NAME", "reelnest-test-unit")
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    path = tmp_path / 'fixture.json'
    path.write_text(json.dumps({'status': 'RUNNING', 'project': 'reelnest-test-unit', 'titleName': 'test', 'titleId': 'a37fe46a-1111-4111-8111-b390e53b71e9'}))
    with pytest.raises(RuntimeError):
        helpers.load_fixture(path)
    fixture = {'status': 'PASS', 'project': 'reelnest-test-unit', 'sourceCommit': commit,
               'titleName': 'test', 'titleId': 'a37fe46a-1111-4111-8111-b390e53b71e9'}
    path.write_text(json.dumps(fixture))
    assert helpers.load_fixture(path)['titleName'] == 'test'
    for other_commit in (None, '0' * 40):
        path.write_text(json.dumps(dict(fixture, sourceCommit=other_commit)))
        with pytest.raises(RuntimeError, match='different source commit'):
            helpers.load_fixture(path)
    path.write_text(json.dumps(fixture))
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-test-different')
    with pytest.raises(RuntimeError, match='different disposable stack'):
        helpers.load_fixture(path)


def test_playlist_children_stay_inside_same_authorized_session():
    parent = 'http://localhost:8080/api/v1/playback/sessions/test/files/480p/index.m3u8'
    assert helpers.hls_child_url(parent, 'segment_001.ts') == parent.rsplit('/', 1)[0] + '/segment_001.ts'
    for target in ('https://outside.invalid/segment.ts', '//outside.invalid/segment.ts',
                   '../../../other/files/segment.ts', '%2e%2e/%2e%2e/private.ts',
                   'segment.ts?token=secret', 'segment.ts#fragment', 'path%5csegment.ts'):
        with pytest.raises(ValueError):
            helpers.hls_child_url(parent, target)


@pytest.mark.parametrize('target', ['http://localhost:8080@outside.invalid', 'http://localhost:8080/path',
                                  'http://localhost:8080?next=outside.invalid', 'http://localhost.evil:8080'])
def test_readonly_smoke_rejects_disguised_remote_origins(target):
    result = subprocess.run([sys.executable, str(ROOT / 'scripts/live_smoke.py'), '--api', target],
                            capture_output=True, text=True)
    assert result.returncode == 2
    assert 'passed; evidence saved' not in result.stdout


def test_chromium_override_is_explicit_executable(tmp_path, monkeypatch):
    monkeypatch.delenv('REELNEST_CHROMIUM_EXECUTABLE', raising=False)
    assert helpers.chromium_launch_options() == {'headless': True}
    browser = tmp_path / 'chromium'
    executable(browser, 'exit 0\n')
    monkeypatch.setenv('REELNEST_CHROMIUM_EXECUTABLE', str(browser))
    assert helpers.chromium_launch_options()['executable_path'] == str(browser)
    monkeypatch.setenv('REELNEST_CHROMIUM_EXECUTABLE', 'relative-path')
    with pytest.raises(RuntimeError, match='existing absolute executable'):
        helpers.chromium_launch_options()


@pytest.mark.parametrize('project', ['', 'reelnest', 'reelnest-test-', 'reelnest-test-a/../../shared', 'reelnest-ci-X'])
def test_disposable_confirmation_rejects_invalid_projects(project, monkeypatch):
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', 'true')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', project)
    with pytest.raises(RuntimeError):
        helpers.require_disposable_stack()


def test_demo_realm_users_satisfy_required_keycloak_profile_fields():
    realm = json.loads((ROOT / 'infrastructure/keycloak/reelnest-realm.json').read_text())
    users = {user['username']: user for user in realm['users']}
    for name in ('demo', 'reviewer'):
        user = users[name]
        assert all(isinstance(user.get(field), str) and user[field].strip()
                   for field in ('username', 'email', 'firstName', 'lastName'))
        assert user['enabled'] and user['emailVerified']
    client = next(client for client in realm['clients'] if client['clientId'] == 'reelnest-web')
    assert client['directAccessGrantsEnabled'] is False, 'Keep browser PKCE rather than enabling password grants'
