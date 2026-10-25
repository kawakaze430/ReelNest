"""Pure, offline contracts for the tightly scoped Codex Cloud evidence runner."""
from pathlib import Path
import importlib.util
import pytest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('cloud_evidence', ROOT / 'scripts/cloud_evidence.py')
cloud = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(cloud)


def test_requires_explicit_disposable_confirmation():
    with pytest.raises(SystemExit) as caught:
        cloud.main(['--phase', 'g1'])
    assert caught.value.code == 2


def test_redacts_known_sensitive_assignment_and_aws_access_key():
    unsafe = 'PASSWORD=my-real-value\nAWS_ACCESS_KEY_ID=AKIA1234567890123456\nAuthorization: BearerTooLong'
    safe = cloud.redact(unsafe)
    assert 'my-real-value' not in safe
    assert 'AKIA1234567890123456' not in safe
    assert 'BearerTooLong' not in safe


def test_pipeline_has_no_destructive_cloud_or_docker_reset_commands():
    source = (ROOT / 'scripts/cloud_evidence.py').read_text(encoding='utf-8')
    for banned in ['terraform apply', 'terraform destroy', 'docker compose down -v',
                   'aws cloudformation deploy', 'razorpay create', 'docker system prune']:
        assert banned not in source


def test_stack_validator_checks_every_tracked_migration():
    path = ROOT / 'scripts/validate_local_stack.sh'
    text = path.read_text(encoding='utf-8')
    assert 'SELECT version,success FROM flyway_schema_history ORDER BY installed_rank' in text
    assert 'from scripts.verify_flyway_postgres import validate_history' in text
    assert 'validate_history(rows)' in text
    spec = importlib.util.spec_from_file_location('verify_history', ROOT / 'scripts/verify_flyway_postgres.py')
    verifier = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(verifier)
    versions = verifier.expected_versions()
    rows = [(str(version), True) for version in versions]
    assert verifier.validate_history(rows) == len(versions)
    for invalid in (rows[:-1], rows + [(str(versions[-1]), True)], [('0', True)] + rows[1:],
                    [('1', False)] + rows[1:], list(reversed(rows))):
        with pytest.raises(AssertionError):
            verifier.validate_history(invalid)


def test_build25_preserves_282_task_ids():
    import csv
    f = ROOT / 'docs/BUILD25_282_TASKS.csv'
    with f.open(newline='', encoding='utf-8') as stream:
        tasks = list(csv.DictReader(stream))
    ids = [item['task_id'] for item in tasks]
    assert len(ids) == len(set(ids)) == 282
    assert {'A-13', 'B-10'}.issubset(ids)


@pytest.fixture
def temporary_evidence(monkeypatch, tmp_path):
    monkeypatch.setattr(cloud, 'ROOT', tmp_path)
    monkeypatch.setattr(cloud, 'EVIDENCE', tmp_path / 'build-evidence')
    return tmp_path


def test_command_logs_have_unique_paths_and_keep_previous_output(temporary_evidence):
    import sys
    first = cloud.run('first', [sys.executable, '-c', 'print("first diagnostic")'], temporary_evidence, 'g0', 5)
    second = cloud.run('second', [sys.executable, '-c', 'print("second diagnostic")'], temporary_evidence, 'g0', 5)
    assert first['status'] == second['status'] == 'PASS'
    assert first['log'] != second['log']
    assert 'first diagnostic' in (temporary_evidence / first['log']).read_text()
    assert 'second diagnostic' in (temporary_evidence / second['log']).read_text()


def test_timeout_keeps_sanitized_output_and_stops_descendants(temporary_evidence):
    import sys
    import time
    marker = temporary_evidence / 'child-survived'
    child = f'import pathlib,time; time.sleep(0.8); pathlib.Path({str(marker)!r}).write_text("unexpected")'
    command = ('import subprocess,sys,time; '
               f'subprocess.Popen([sys.executable,"-c",{child!r}]); '
               'print("before-timeout PASSWORD=synthetic-sensitive",flush=True); time.sleep(8)')
    result = cloud.run('timeout', [sys.executable, '-c', command], temporary_evidence, 'g0', 0.2)
    assert result['status'] == 'TIMEOUT'
    output = (temporary_evidence / result['log']).read_text()
    assert 'before-timeout' in output
    assert 'synthetic-sensitive' not in output
    assert 'synthetic-sensitive' not in repr(result)
    time.sleep(0.9)
    assert not marker.exists(), 'Timed-out child escaped runner termination'


def test_redacts_bearer_json_url_query_jwt_and_private_key():
    samples = [
        'Authorization: Bearer authorization-value\n',
        'Cookie: session=private-cookie\n',
        '{"client_secret": "quoted secret with spaces"}',
        'DATABASE_URL=postgresql://database-user:database-password@localhost/reelnest',
        'https://example.test/log?access_token=query-sensitive&safe=1',
        'eyJhbGciOiJub25lIn0.eyJzdWIiOiJwcml2YXRlIn0.signature',
        '-----BEGIN PRIVATE KEY-----\nprivate-material\n-----END PRIVATE KEY-----',
    ]
    safe = cloud.redact('\n'.join(samples))
    for sensitive in ['authorization-value', 'private-cookie', 'quoted secret with spaces',
                      'database-password', 'query-sensitive', 'eyJhbGciOiJub25lIn0', 'private-material']:
        assert sensitive not in safe
    assert 'safe=1' in safe


def test_missing_command_does_not_claim_execution(temporary_evidence):
    result = cloud.run('absent', ['reelnest-unavailable-test-executable'], temporary_evidence, 'g0', 1)
    assert result['status'] == 'BLOCKED'
    assert result['exit_code'] is None
    assert 'log' not in result


def test_empty_and_skipped_only_results_do_not_pass():
    assert cloud.outcome([]) == 'NOT_RUN'
    assert cloud.outcome([cloud.skipped('test', 'not executed')]) == 'NOT_RUN'
    assert cloud.outcome([{'status': 'PASS'}, {'status': 'NOT_RUN'}]) == 'NOT_RUN'


@pytest.fixture
def source_and_proof(monkeypatch, tmp_path):
    import json
    state = {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False}
    monkeypatch.setattr(cloud, 'source_state', lambda: dict(state))
    proof = {
        'phase': 'g0', 'source_commit': state['commit'], 'source_fingerprint': state['fingerprint'],
        'source_dirty': False, 'outcome': 'PASS',
        'results': [{'step': step, 'status': 'PASS', 'exit_code': 0} for step in cloud.REQUIRED_G0_CHECKS],
    }
    path = tmp_path / 'g0.json'
    def save():
        path.write_text(json.dumps(proof))
        return path
    save()
    return state, proof, save


def test_native_gate_rejects_stale_commit_and_modified_worktree(source_and_proof):
    state, proof, save = source_and_proof
    assert cloud.g0_readiness_error(save()) is None
    state['commit'] = 'c' * 40
    assert 'commit' in cloud.g0_readiness_error(save())
    state['commit'] = proof['source_commit']
    state['fingerprint'] = 'd' * 64
    state['dirty'] = True
    assert 'Source files changed' in cloud.g0_readiness_error(save())


def test_native_gate_accepts_honest_matching_tested_dirty_snapshot(source_and_proof):
    state, proof, save = source_and_proof
    state['dirty'] = proof['source_dirty'] = True
    assert cloud.g0_readiness_error(save()) is None


@pytest.mark.parametrize('bad_status', ['FAIL', 'BLOCKED', 'TIMEOUT', 'NOT_RUN', 'SKIPPED'])
def test_native_gate_requires_all_actual_successful_g0_commands(source_and_proof, bad_status):
    state, proof, save = source_and_proof
    proof['results'][0]['status'] = bad_status
    assert cloud.g0_readiness_error(save()) is not None


def test_native_gate_rejects_incomplete_passing_report(source_and_proof):
    state, proof, save = source_and_proof
    proof['results'].pop()
    assert 'omits required' in cloud.g0_readiness_error(save())


def test_ci_gate_binds_successful_needs_to_actual_run_and_clean_sha(source_and_proof, monkeypatch):
    state, proof, save = source_and_proof
    proof.update(format='github-actions-g0', workflow_run_id='123',
                 components={name: {'result': 'success'} for name in cloud.REQUIRED_CI_COMPONENTS})
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    monkeypatch.setenv('GITHUB_SHA', state['commit'])
    monkeypatch.setenv('GITHUB_RUN_ID', '123')
    assert cloud.g0_readiness_error(save()) is None
    proof['components']['compose-validation']['result'] = 'failure'
    assert 'compose-validation' in cloud.g0_readiness_error(save())
    proof['components']['compose-validation']['result'] = 'success'
    proof['workflow_run_id'] = '124'
    assert 'Actions run' in cloud.g0_readiness_error(save())
    proof['workflow_run_id'] = '123'
    state['dirty'] = True
    assert 'unchanged checkout' in cloud.g0_readiness_error(save())


def test_ci_proof_cannot_bypass_native_g0_outside_actions(source_and_proof, monkeypatch):
    state, proof, save = source_and_proof
    proof.update(format='github-actions-g0', workflow_run_id='123',
                 components={name: 'success' for name in cloud.REQUIRED_CI_COMPONENTS})
    monkeypatch.delenv('GITHUB_ACTIONS', raising=False)
    assert 'Actions run' in cloud.g0_readiness_error(save())


def test_g1_blocks_before_any_docker_command_without_disposable_scope(source_and_proof, monkeypatch):
    state, proof, save = source_and_proof
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', 'false')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'shared-project')
    monkeypatch.setattr(cloud, 'run', lambda *args, **kwargs: pytest.fail('Docker should never be called'))
    results = cloud.evaluate('g1', disposable_confirmed=True, g0_evidence=save())
    assert len(results) == 1 and results[0]['status'] == 'BLOCKED'
    assert 'REELNEST_DISPOSABLE_STACK' in results[0]['detail']


def test_sanitize_log_does_not_execute_phase_or_modify_raw_log(tmp_path, monkeypatch):
    raw = tmp_path / 'raw.log'
    safe = tmp_path / 'artifacts' / 'safe.log'
    raw.write_bytes(b'PASSWORD=private-test-value\nnon-utf8:\xff')
    monkeypatch.setattr(cloud, 'evaluate', lambda *args, **kwargs: pytest.fail('Phase executed during sanitization'))
    assert cloud.main(['--sanitize-log', str(raw), str(safe)]) == 0
    assert 'private-test-value' not in safe.read_text()
    assert b'private-test-value' in raw.read_bytes()
    with pytest.raises(SystemExit) as caught:
        cloud.main(['--sanitize-log', str(raw), str(raw)])
    assert caught.value.code == 2


def test_report_preserves_immutable_run_copy_and_current_source_metadata(temporary_evidence, monkeypatch):
    state = {'commit': 'e' * 40, 'fingerprint': 'f' * 64, 'dirty': True}
    monkeypatch.setattr(cloud, 'source_state', lambda: state)
    monkeypatch.setattr(cloud, 'SOURCE_AT_START', {**state, 'dirty': False})
    monkeypatch.setattr(cloud, 'RUN_ID', 'first-test-run')
    result = {'step': 'one real test', 'status': 'PASS', 'exit_code': 0}
    report = cloud.report('test', [result])
    assert report['source_commit'] == state['commit']
    assert report['source_dirty'] is True
    assert report['source_changed_during_run'] is True
    first = (cloud.EVIDENCE / 'cloud-test-first-test-run.json').read_text()
    monkeypatch.setattr(cloud, 'RUN_ID', 'second-test-run')
    cloud.report('test', [{'step': 'next test', 'status': 'FAIL', 'exit_code': 1}])
    assert (cloud.EVIDENCE / 'cloud-test-first-test-run.json').read_text() == first
    assert 'second-test-run' in (cloud.EVIDENCE / 'cloud-test-latest.json').read_text()


@pytest.mark.parametrize('status,code,severity,outcome', [
    ('FAIL', 17, 'error', 'FAIL'), ('BLOCKED', None, 'warning', 'BLOCKED'),
    ('TIMEOUT', -15, 'error', 'FAIL'),
])
def test_failed_ci_report_annotates_original_step_and_exit_without_changing_results(
        temporary_evidence, monkeypatch, capsys, status, code, severity, outcome):
    import json
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    result = {'step': 'actual component', 'status': status, 'exit_code': code,
              'detail': 'PASSWORD=synthetic-password\nhttps://blob.example.invalid/object?sig=synthetic-sas&safe=1'}
    original = json.dumps(result)
    data = cloud.report('g1', [result])
    output = capsys.readouterr().out
    annotations = [line for line in output.splitlines() if line.startswith('::')]
    assert len(annotations) == 1 and annotations[0].startswith(f'::{severity}::')
    assert 'actual component' in annotations[0] and f'exit={code}' in annotations[0]
    assert 'synthetic-password' not in output and 'synthetic-sas' not in output
    assert 'safe=1' in annotations[0] and '[REDACTED]' in annotations[0]
    assert data['outcome'] == outcome and data['results'] == [result]
    assert json.dumps(result) == original


@pytest.mark.parametrize('actions,status', [(None, 'FAIL'), ('false', 'BLOCKED'),
                                         ('true', 'PASS'), ('true', 'SKIPPED')])
def test_report_never_annotates_passing_unrun_or_non_actions_checks(
        temporary_evidence, monkeypatch, capsys, actions, status):
    if actions is None:
        monkeypatch.delenv('GITHUB_ACTIONS', raising=False)
    else:
        monkeypatch.setenv('GITHUB_ACTIONS', actions)
    cloud.report('test', [{'step': 'component', 'status': status, 'exit_code': 0 if status == 'PASS' else None}])
    assert not any(line.startswith('::') for line in capsys.readouterr().out.splitlines())


@pytest.mark.parametrize('mismatch', [None, 'source', 'project', 'old', 'future', 'naive',
                                      'missing-start', 'malformed', 'oversized', 'not-validator'])
def test_g1_annotation_names_only_this_invocations_matching_failed_stack_stage(
        temporary_evidence, monkeypatch, capsys, mismatch):
    import datetime as dt
    import json
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-ci-current')
    monkeypatch.setattr(cloud, 'source_state', lambda: {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False})
    now = dt.datetime.now(dt.timezone.utc)
    result = {'step': 'complete stack', 'status': 'FAIL', 'exit_code': 1,
              'command': ['bash', 'scripts/validate_local_stack.sh', '--full'],
              'started_at_utc': (now - dt.timedelta(seconds=5)).isoformat(), 'detail': 'validator exited'}
    stack = {'project': 'reelnest-ci-current', 'sourceCommit': 'a' * 40,
             'startedAt': (now - dt.timedelta(seconds=4)).isoformat(),
             'steps': [{'step': 'configuration', 'status': 'PASS', 'exitCode': 0},
                       {'step': 'worker readiness', 'status': 'FAIL', 'exitCode': 42}]}
    if mismatch == 'source':
        stack['sourceCommit'] = 'c' * 40
    elif mismatch == 'project':
        stack['project'] = 'reelnest-ci-previous'
    elif mismatch == 'old':
        stack['startedAt'] = (now - dt.timedelta(seconds=10)).isoformat()
    elif mismatch == 'future':
        stack['startedAt'] = (now + dt.timedelta(minutes=1)).isoformat()
    elif mismatch == 'naive':
        stack['startedAt'] = now.replace(tzinfo=None).isoformat()
    elif mismatch == 'missing-start':
        result.pop('started_at_utc')
    elif mismatch == 'not-validator':
        result['command'] = ['docker', 'info']
    cloud.EVIDENCE.mkdir(parents=True)
    payload = '{broken JSON' if mismatch == 'malformed' else json.dumps(stack)
    if mismatch == 'oversized':
        payload += ' ' * 65537
    (cloud.EVIDENCE / 'local-stack-latest.json').write_text(payload)
    data = cloud.report('g1', [result])
    annotations = [line for line in capsys.readouterr().out.splitlines() if line.startswith('::')]
    assert len(annotations) == 1 and data['outcome'] == 'FAIL'
    if mismatch is None:
        assert 'Local stack stage: worker readiness; status=FAIL; exit=42' in annotations[0]
    else:
        assert 'Local stack stage:' not in annotations[0]


def test_reusable_annotation_redacts_and_escapes_workflow_command_injection(monkeypatch, capsys):
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    message = ('original%0A::warning::forged\r\n::error::second-command\n'
               'Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiJwcml2YXRlIn0.signature\n'
               'https://blob.example.invalid/archive?sig=synthetic-sas')
    cloud.emit_github_annotation(message)
    lines = capsys.readouterr().out.splitlines()
    assert len(lines) == 1 and lines[0].startswith('::error::')
    assert '%250A::warning::forged%0D%0A::error::second-command%0A' in lines[0]
    assert 'eyJhbGciOiJub25lIn0' not in lines[0] and 'synthetic-sas' not in lines[0]


def test_reusable_annotation_is_byte_bounded_after_redaction_and_valid_utf8(monkeypatch, capsys):
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    cloud.emit_github_annotation('PASSWORD=synthetic-password\n' + '\u754c' * 4000, level='warning')
    line = capsys.readouterr().out.rstrip('\n')
    assert line.startswith('::warning::') and line.endswith(' [truncated]')
    assert len(line.encode('utf-8')) <= cloud.GITHUB_ANNOTATION_MAX_BYTES
    assert 'synthetic-password' not in line and '[REDACTED]' in line


def test_reusable_annotation_rejects_injected_level(monkeypatch, capsys):
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    with pytest.raises(ValueError):
        cloud.emit_github_annotation('detail', level='error\n::warning::')
    assert not capsys.readouterr().out


@pytest.mark.parametrize('status', ['FAIL', 'BLOCKED'])
def test_ci_annotation_does_not_change_main_exit_or_underlying_result(
        temporary_evidence, monkeypatch, capsys, status):
    import json
    monkeypatch.setenv('GITHUB_ACTIONS', 'true')
    monkeypatch.setattr(cloud, 'source_state', lambda: {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False})
    original = {'step': 'component failed', 'status': status, 'exit_code': 9, 'detail': 'actual failure'}
    monkeypatch.setattr(cloud, 'evaluate', lambda *args, **kwargs: [original])
    assert cloud.main(['--phase', 'g0']) == 1
    saved = json.loads((cloud.EVIDENCE / 'cloud-g0-latest.json').read_text())
    assert saved['results'] == [original] and saved['outcome'] == status
    assert f"status={status}; exit=9" in capsys.readouterr().out



def test_incomplete_g0_report_does_not_claim_phase_pass(temporary_evidence, monkeypatch):
    monkeypatch.setattr(cloud, 'source_state', lambda: {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False})
    data = cloud.report('g0', [{'step': 'TypeScript typecheck', 'status': 'PASS', 'exit_code': 0}])
    assert data['outcome'] == 'NOT_RUN'


def test_g1_keeps_validator_missing_prerequisite_as_blocked(source_and_proof, monkeypatch):
    state, proof, save = source_and_proof
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', 'true')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-test-behavior')
    commands = []
    def fake_run(label, cmd, cwd, phase, timeout):
        commands.append((cmd, timeout))
        exit_code = 3 if cmd[0] == 'bash' else 0
        return {'step': label, 'command': cmd, 'status': 'FAIL' if exit_code else 'PASS', 'exit_code': exit_code}
    monkeypatch.setattr(cloud, 'run', fake_run)
    results = cloud.evaluate('g1', disposable_confirmed=True, g0_evidence=save())
    assert len(commands) == 2
    assert commands[-1] == (['bash', 'scripts/validate_local_stack.sh', '--full'], 2400)
    assert results[-1]['status'] == 'BLOCKED' and results[-1]['exit_code'] == 3


def test_g0_source_change_during_run_cannot_certify_final_worktree(temporary_evidence, monkeypatch):
    initial = {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False}
    final = {'commit': 'a' * 40, 'fingerprint': 'c' * 64, 'dirty': True}
    monkeypatch.setattr(cloud, 'SOURCE_AT_START', initial)
    monkeypatch.setattr(cloud, 'source_state', lambda: final)
    results = [{'step': step, 'status': 'PASS', 'exit_code': 0} for step in cloud.REQUIRED_G0_CHECKS]
    report = cloud.report('g0', results)
    assert report['source_meaning_changed_during_run'] is True
    assert report['outcome'] == 'NOT_RUN'
    assert 'not validated by every' in report['readiness_detail']
    assert 'Source changed during G0' in cloud.g0_readiness_error(cloud.EVIDENCE / 'cloud-g0-latest.json')


def test_g0_known_generated_next_changes_are_recorded_without_hiding_real_edits(temporary_evidence, monkeypatch):
    initial = {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'application_fingerprint': 'd' * 64, 'dirty': False}
    final = {**initial, 'fingerprint': 'c' * 64, 'dirty': True}
    monkeypatch.setattr(cloud, 'SOURCE_AT_START', initial)
    monkeypatch.setattr(cloud, 'source_state', lambda: final)
    results = [{'step': step, 'status': 'PASS', 'exit_code': 0} for step in cloud.REQUIRED_G0_CHECKS]
    report = cloud.report('g0', results)
    assert report['source_changed_during_run'] is True
    assert report['source_meaning_changed_during_run'] is False
    assert report['source_dirty'] is True and report['outcome'] == 'PASS'
    assert cloud.g0_readiness_error(cloud.EVIDENCE / 'cloud-g0-latest.json') is None


def test_next_normalization_preserves_meaningful_configuration_and_directives(tmp_path):
    import json
    config = tmp_path / 'tsconfig.json'
    base = {'compilerOptions': {'strict': True}, 'include': ['src/**/*.ts']}
    config.write_text(json.dumps(base))
    before = cloud.normalized_next_source(config)
    base['include'].append('.next/dev/types/**/*.ts')
    config.write_text(json.dumps(base, indent=4))
    assert cloud.normalized_next_source(config) == before
    base['compilerOptions']['strict'] = False
    config.write_text(json.dumps(base))
    assert cloud.normalized_next_source(config) != before
    declarations = tmp_path / 'next-env.d.ts'
    declarations.write_text('/// <reference types="next" />\n// This file is normally maintained by Next.js.\n')
    before = cloud.normalized_next_source(declarations)
    declarations.write_text('/// <reference types="next" />\nimport "./.next/types/routes.d.ts";\n'
                            'import "./.next/types/root-params.d.ts";\n// NOTE: This file should not be edited\n')
    assert cloud.normalized_next_source(declarations) == before
    declarations.write_text(declarations.read_text() + '// @ts-nocheck\n')
    assert cloud.normalized_next_source(declarations) != before


def test_signed_log_urls_do_not_expose_signature_or_credentials():
    unsafe = 'https://logs.example.test/log?X-Amz-Credential=private-account&X-Amz-Signature=private-signature&safe=1'
    safe = cloud.redact(unsafe)
    assert 'private-account' not in safe and 'private-signature' not in safe
    assert 'safe=1' in safe


@pytest.mark.parametrize('parameters', [
    'sig=synthetic-sas-value%2Bencoded%3D&safe=1',
    'safe=1&sig=synthetic-sas-value%2Bencoded%3D',
    'sv=2025-01-05&SIG=synthetic-sas-value%2Bencoded%3D&safe=1',
])
def test_azure_artifact_sas_signature_is_redacted_without_losing_diagnostics(parameters):
    from urllib.parse import parse_qsl, urlsplit
    url = 'https://productionresultssa11.blob.core.windows.net/actions/artifact.zip?' + parameters
    safe = cloud.redact(url)
    assert 'synthetic-sas-value' not in safe
    assert urlsplit(safe).hostname == 'productionresultssa11.blob.core.windows.net'
    assert urlsplit(safe).path == '/actions/artifact.zip'
    fields = dict(parse_qsl(urlsplit(safe).query))
    assert fields.get('sig', fields.get('SIG')) == '[REDACTED]'
    assert fields['safe'] == '1'


@pytest.mark.parametrize('text', [
    'sig=synthetic-sas-value',
    'SIG="synthetic SAS value with spaces"',
    '{"sig": "synthetic SAS value with spaces", "status": "denied"}',
])
def test_exact_sig_assignments_and_json_fields_are_redacted(text):
    safe = cloud.redact(text)
    assert 'synthetic' not in safe
    assert '[REDACTED]' in safe
    if 'status' in text:
        assert '"status": "denied"' in safe


def test_sig_argument_is_sensitive_but_unrelated_signal_and_design_names_are_preserved():
    command = ['tool', '--sig', 'synthetic-sas-value', '--signal', 'TERM', '--design=preview']
    assert cloud.safe_command(command) == ['tool', '--sig', '[REDACTED]', '--signal', 'TERM', '--design=preview']
    diagnostic = 'signal=TERM design=preview signatureCount=3 sigmoid=enabled'
    assert cloud.redact(diagnostic) == diagnostic
    url = 'https://example.invalid/test?signal=TERM&design=preview&signatureCount=3&sigmoid=enabled'
    # The existing broader signature parameter protection is retained. Other
    # words containing the letters "sig" must not be treated as SAS signatures.
    safe = cloud.redact(url)
    assert 'signal=TERM' in safe and 'design=preview' in safe and 'sigmoid=enabled' in safe


@pytest.mark.parametrize('parameters', [
    'GoogleAccessId=google-account-sensitive&Signature=google-signature-sensitive&Expires=1234567890',
    'X-Amz-Credential=aws-account-sensitive&X-Amz-Signature=aws-signature-sensitive&X-Amz-Security-Token=aws-token-sensitive',
    'X-Goog-Credential=goog-account-sensitive&X-Goog-Signature=goog-signature-sensitive',
    'AWSAccessKeyId=aws-id-sensitive&access_token=oauth-token-sensitive',
])
def test_signed_artifact_urls_redact_all_credential_query_values_but_keep_diagnostic_path(parameters):
    from urllib.parse import parse_qsl
    url = 'https://storage.googleapis.com/proxy-golang-org-prod/dependency.zip?' + parameters + '&safe=1'
    safe = cloud.redact(url)
    assert safe.startswith('https://storage.googleapis.com/proxy-golang-org-prod/dependency.zip?')
    for key, value in parse_qsl(parameters):
        if key != 'Expires':
            assert value not in safe
    assert 'safe=1' in safe
    if 'Expires=' in parameters:
        assert 'Expires=1234567890' in safe


def test_real_git_fingerprint_tracks_source_edits_and_only_normalizes_known_next_output(tmp_path, monkeypatch):
    import json
    import subprocess
    def git(*args):
        return subprocess.run(['git', '-C', str(tmp_path), *args], check=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    git('init', '--quiet', '--initial-branch=main')
    web = tmp_path / 'apps' / 'web'
    web.mkdir(parents=True)
    (web / 'tsconfig.json').write_text(json.dumps({'compilerOptions': {'strict': True}, 'include': ['src/**/*.ts']}))
    (web / 'next-env.d.ts').write_text('/// <reference types="next" />\n// This file is normally maintained by Next.js.\n')
    source = web / 'code.ts'
    source.write_text('export const value = 1;\n')
    git('add', '.')
    git('-c', 'user.name=ReelNest Test', '-c', 'user.email=tests@example.invalid', 'commit', '--quiet', '-m', 'fixture')
    monkeypatch.setattr(cloud, 'ROOT', tmp_path)
    clean = cloud.source_state()
    assert clean['dirty'] is False and clean['commit']
    config = json.loads((web / 'tsconfig.json').read_text())
    config['include'].append('.next/dev/types/**/*.ts')
    (web / 'tsconfig.json').write_text(json.dumps(config, indent=4))
    (web / 'next-env.d.ts').write_text('/// <reference types="next" />\nimport "./.next/types/routes.d.ts";\n')
    generated = cloud.source_state()
    assert generated['dirty'] is True and generated['fingerprint'] != clean['fingerprint']
    assert generated['application_fingerprint'] == clean['application_fingerprint']
    source.write_text('export const value = 2;\n')
    changed = cloud.source_state()
    assert changed['application_fingerprint'] != generated['application_fingerprint']
    (web / 'untracked.ts').write_text('export const neverExpose = "synthetic-source-sentinel";\n')
    untracked = cloud.source_state()
    assert untracked['application_fingerprint'] != changed['application_fingerprint']
    assert 'synthetic-source-sentinel' not in repr(untracked)


def test_g0_runs_lint_before_typecheck_build_and_does_not_mask_failed_lint(temporary_evidence, monkeypatch):
    web = temporary_evidence / 'apps/web'
    web.mkdir(parents=True)
    (web / 'package-lock.json').write_text('{}')
    state = {'commit': 'a' * 40, 'fingerprint': 'b' * 64, 'dirty': False}
    monkeypatch.setattr(cloud, 'source_state', lambda: state)
    monkeypatch.setattr(cloud, 'SOURCE_AT_START', state)
    commands = []
    def fake_run(label, cmd, cwd, phase, timeout):
        commands.append((cmd, cwd))
        exit_code = 1 if cmd == ['npm', 'run', 'lint'] else 0
        return {'step': label, 'command': cmd, 'status': 'FAIL' if exit_code else 'PASS', 'exit_code': exit_code}
    monkeypatch.setattr(cloud, 'run', fake_run)
    results = cloud.evaluate('g0')
    web_commands = [cmd for cmd, cwd in commands if cwd == web]
    assert web_commands == [
        ['npm', 'ci', '--no-audit', '--no-fund'], ['npm', 'run', 'lint'],
        ['npm', 'run', 'typecheck'], ['npm', 'run', 'build'],
    ]
    assert next(item for item in results if item['step'] == 'Frontend ESLint')['status'] == 'FAIL'
    assert next(item for item in results if item['step'] == 'TypeScript typecheck')['status'] == 'PASS'
    assert next(item for item in results if item['step'] == 'Next.js production build')['status'] == 'PASS'
    assert cloud.report('g0', results)['outcome'] == 'FAIL'
    assert cloud.g0_readiness_error(cloud.EVIDENCE / 'cloud-g0-latest.json') is not None


def test_g0_proof_without_lint_is_incomplete_even_if_other_checks_pass(source_and_proof):
    state, proof, save = source_and_proof
    proof['results'] = [item for item in proof['results'] if item['step'] != 'Frontend ESLint']
    assert 'omits required' in cloud.g0_readiness_error(save())


def test_missing_dependencies_skip_lint_and_record_it_as_unrun(temporary_evidence, monkeypatch):
    web = temporary_evidence / 'apps/web'
    web.mkdir(parents=True)
    (web / 'package-lock.json').write_text('{}')
    commands = []
    def fake_run(label, cmd, cwd, phase, timeout):
        commands.append(cmd)
        exit_code = 1 if cmd[:2] == ['npm', 'ci'] else 0
        return {'step': label, 'command': cmd, 'status': 'FAIL' if exit_code else 'PASS', 'exit_code': exit_code}
    monkeypatch.setattr(cloud, 'run', fake_run)
    results = cloud.evaluate('g0')
    assert ['npm', 'run', 'lint'] not in commands
    assert ['npm', 'run', 'typecheck'] not in commands
    assert ['npm', 'run', 'build'] not in commands
    assert any(item['status'] == 'SKIPPED' and 'Frontend ESLint' in item['step'] for item in results)
    assert cloud.outcome(results) == 'FAIL'
