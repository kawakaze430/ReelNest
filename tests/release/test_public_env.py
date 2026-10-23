"""Subprocess tests for fail-closed public Next.js build URL validation."""
import os
from pathlib import Path
import subprocess
import shutil
import pytest

ROOT=Path(__file__).resolve().parents[2]
NODE=shutil.which('node')
GOOD={
  'NEXT_PUBLIC_API_BASE_URL':'https://api.reelnest.test',
  'NEXT_PUBLIC_APP_ORIGIN':'https://app.reelnest.test',
  'NEXT_PUBLIC_AUTHORITY':'https://login.reelnest.test/realms/reelnest',
  'NEXT_PUBLIC_OIDC_CLIENT_ID':'reelnest-web'
}

def test_valid_public_urls():
    if not NODE:pytest.skip('Node not installed')
    proc=subprocess.run([NODE,'apps/web/scripts/validate-public-env.mjs'],cwd=ROOT,
                        env={**os.environ,**GOOD},capture_output=True,text=True)
    assert proc.returncode==0,proc.stderr

@pytest.mark.parametrize('name,value',[
  ('NEXT_PUBLIC_API_BASE_URL','http://api.reelnest.test'),
  ('NEXT_PUBLIC_API_BASE_URL','https://localhost:8080'),
  ('NEXT_PUBLIC_APP_ORIGIN','https://app.example.com'),
  ('NEXT_PUBLIC_APP_ORIGIN','https://app.reelnest.test/path'),
  ('NEXT_PUBLIC_AUTHORITY','https://login.reelnest.test/not-a-realm'),
  ('NEXT_PUBLIC_API_BASE_URL','https://user:pass@api.reelnest.test')
])
def test_unsafe_urls_rejected(name,value):
    if not NODE:pytest.skip('Node not installed')
    proc=subprocess.run([NODE,'apps/web/scripts/validate-public-env.mjs'],cwd=ROOT,
                        env={**os.environ,**GOOD,name:value},capture_output=True,text=True)
    assert proc.returncode!=0
