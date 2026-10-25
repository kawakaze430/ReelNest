#!/usr/bin/env python3
"""Bounded, sanitized annotations from this disposable Compose project's services."""
import json
import re
import subprocess

from cloud_evidence import emit_github_annotation
from live_e2e_helpers import require_disposable_stack


def compose(*arguments):
    return subprocess.run(['docker', 'compose', *arguments], capture_output=True,
                          text=True, timeout=25, check=False)


def main():
    require_disposable_stack()
    try:
        result = compose('ps', '--all', '--format', 'json')
        if result.returncode:
            emit_github_annotation(f'Could not inspect disposable containers (exit {result.returncode}): {result.stderr}', level='warning')
            return
        text = result.stdout.strip()
        if not text:
            return
        rows = json.loads(text) if text.startswith('[') else [json.loads(line) for line in text.splitlines() if line.strip()]
        failed = []
        available = set()
        for row in rows:
            if not isinstance(row, dict):
                raise ValueError('Invalid Compose container metadata')
            service = row.get('Service', '')
            if not isinstance(service, str) or not re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_.-]{0,62}', service):
                continue
            available.add(service)
            if (row.get('State') in ('exited', 'dead') and int(row.get('ExitCode') or 0) != 0) or row.get('Health') == 'unhealthy':
                failed.append((service, row.get('State'), row.get('ExitCode'), row.get('Health')))
        # HTTP readiness can fail while a container remains RUNNING. These are
        # diagnostics only; do not infer that a running service is faulty.
        if not failed:
            failed = [(name, 'readiness diagnostics', None, None)
                      for name in ('api', 'web', 'keycloak') if name in available]
        for service, state, code, health in sorted(set(failed))[:8]:
            logs = compose('logs', '--no-color', '--tail', '40', service)
            message = (f'Disposable service {service}: state={state}, exit={code}, health={health}. '
                       f'Log command exit={logs.returncode}.\n{logs.stdout}\n{logs.stderr}')
            emit_github_annotation(message, level='error' if code or health == 'unhealthy' else 'warning')
    except (OSError, ValueError, TypeError, subprocess.TimeoutExpired) as error:
        emit_github_annotation(f'Disposable container diagnostics unavailable: {type(error).__name__}', level='warning')


if __name__ == '__main__':
    main()
