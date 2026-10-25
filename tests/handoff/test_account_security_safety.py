"""Account mutation tests must refuse an unconfirmed stack before using services."""
import asyncio
import importlib.util
from pathlib import Path

import pytest


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def account_security(monkeypatch):
    monkeypatch.syspath_prepend(str(ROOT / 'scripts'))
    spec = importlib.util.spec_from_file_location('account_security_live', ROOT / 'scripts/e2e_account_security_live.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_account_mutation_refuses_missing_disposable_confirmation(account_security, monkeypatch):
    monkeypatch.delenv('REELNEST_DISPOSABLE_STACK', raising=False)
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-test-unit')
    with pytest.raises(RuntimeError, match='BLOCKED'):
        asyncio.run(account_security.main())


@pytest.mark.parametrize('profile_id', ["x' OR true --", '../shared', '', None])
def test_pin_counter_read_rejects_non_uuid_before_docker(account_security, monkeypatch, profile_id):
    def unexpected(*args, **kwargs):
        pytest.fail('Docker must not receive an invalid profile identifier')
    monkeypatch.setattr(account_security.subprocess, 'run', unexpected)
    with pytest.raises(ValueError):
        account_security.pin_failures(profile_id)
