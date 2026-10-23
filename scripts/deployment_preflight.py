#!/usr/bin/env python3
"""Audit LOCAL AWS/full-stack deployment prerequisites without credentials or side effects.

Readiness is intentionally conservative: source checks cannot prove a live release.
"""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def command_available(exe: str) -> bool:
    return shutil.which(exe) is not None


def collect(root: Path = ROOT) -> dict:
    required = ['docker', 'java', 'mvn', 'node', 'npm', 'terraform', 'aws']
    binaries = {name: command_available(name) for name in required}
    files = {
        'local_compose': (root / 'compose.yaml').is_file(),
        'backend_pom': (root / 'apps/api/pom.xml').is_file(),
        'frontend_package': (root / 'apps/web/package.json').is_file(),
        'frontend_lockfile': (root / 'apps/web/package-lock.json').is_file(),
        'aws_terraform': (root / 'infrastructure/aws/versions.tf').is_file(),
        'terraform_reviewed_vars': (root / 'infrastructure/aws/terraform.tfvars').is_file(),
        'deploy_guide': (root / 'docs/AWS_DOMAIN_AND_DEPLOYMENT.md').is_file(),
        'amplify_preview_archive': (root / 'deploy/reelnest-amplify-preview.zip').is_file(),
    }
    # Never display/access raw values or API keys. Missing evidence stays blocked.
    # AWS_PROFILE by itself is not sufficient evidence that access is authorized.
    manual_gates = [
        'GitHub CI Maven verify, npm typecheck/build, Terraform provider validate',
        'Docker integration and original-film creator-to-viewer E2E',
        'Razorpay merchant TEST captures, renewal, reversals and webhooks',
        'AWS staging Terraform plan/apply and HTTPS OIDC/Keycloak',
        'Real private S3 and signed CloudFront HLS access with rights geofencing',
        'Security testing, identity MFA, privacy deletion, backup restoration',
        'Licensed media and Indian OTT/consumer/privacy/legal clearance',
    ]
    return {
        'release_decision': 'NO-GO',
        'explanation': 'Cannot certify deployment from local files or source-only checks.',
        'local_binaries': binaries,
        'files': files,
        'external_gates_unverified': manual_gates,
        'static_preview_manual_deploy_ready': files['amplify_preview_archive'],
        'full_ott_production_ready': False,
    }


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--json', type=Path, default=None)
    args = ap.parse_args()
    data = collect()
    result = json.dumps(data, indent=2)
    print(result)
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(result+'\n', encoding='utf-8')
    return 0  # reporting incomplete prerequisites is not a script execution error


if __name__ == '__main__':
    raise SystemExit(main())
