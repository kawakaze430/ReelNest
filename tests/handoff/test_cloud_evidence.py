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


def test_stack_validator_checks_every_migration_v1_to_v25():
    path = ROOT / 'scripts/validate_local_stack.sh'
    text = path.read_text(encoding='utf-8')
    assert 'COUNT(DISTINCT version)' in text
    assert "2[0-5]" in text
    assert '=25' in text
    assert 'COUNT(*)>=15' not in text


def test_build25_preserves_282_task_ids():
    import csv
    f = ROOT / 'docs/BUILD25_282_TASKS.csv'
    with f.open(newline='', encoding='utf-8') as stream:
        tasks = list(csv.DictReader(stream))
    ids = [item['task_id'] for item in tasks]
    assert len(ids) == len(set(ids)) == 282
    assert {'A-13', 'B-10'}.issubset(ids)
