"""Helpers for real, private developer-stack tests; never logs browser credentials."""
from __future__ import annotations

import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urljoin, urlsplit
import uuid

ROOT = Path(__file__).resolve().parents[1]
WEB = 'http://localhost:3000'
API = 'http://localhost:8080'
FIXTURE = ROOT / 'build-evidence' / 'processing-fixture.local.json'


def require_disposable_stack():
    project = os.getenv('COMPOSE_PROJECT_NAME', '')
    if os.getenv('REELNEST_DISPOSABLE_STACK') != 'true' or not re.fullmatch(r'reelnest-(ci|test)-[a-z0-9][a-z0-9_-]*', project):
        raise RuntimeError('BLOCKED: set REELNEST_DISPOSABLE_STACK=true and a unique reelnest-ci-/reelnest-test- COMPOSE_PROJECT_NAME')


def chromium_launch_options():
    options = {'headless': True}
    executable = os.getenv('REELNEST_CHROMIUM_EXECUTABLE')
    if executable:
        path = Path(executable)
        if not path.is_absolute() or not path.is_file() or not os.access(path, os.X_OK):
            raise RuntimeError('BLOCKED: REELNEST_CHROMIUM_EXECUTABLE must name an existing absolute executable')
        options['executable_path'] = str(path)
    return options


def load_fixture(path=FIXTURE):
    data = json.loads(Path(path).read_text())
    if data.get('status') != 'PASS':
        raise RuntimeError('Processing fixture did not complete successfully; run e2e_processing_live.py first')
    if data.get('project') != os.getenv('COMPOSE_PROJECT_NAME'):
        raise RuntimeError('Processing fixture belongs to a different disposable stack')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    if data.get('sourceCommit') != commit:
        raise RuntimeError('Processing fixture belongs to a different source commit')
    uuid.UUID(data['titleId'])
    if not data.get('titleName'):
        raise ValueError('Processing fixture title missing')
    return data


def hls_child_url(parent, child):
    """Resolve only paths within the same authorized local playback session."""
    source = urlsplit(parent)
    target = urlsplit(urljoin(parent, child))
    prefix = source.path.split('/files/', 1)[0] + '/files/'
    if (source.scheme, source.netloc) != ('http', 'localhost:8080') or '/files/' not in source.path:
        raise ValueError('Expected a local private playback manifest')
    decoded = unquote(target.path)
    if ((target.scheme, target.netloc) != (source.scheme, source.netloc)
            or target.query or target.fragment or not decoded.startswith(prefix)
            or '..' in decoded.split('/') or '\\' in decoded):
        raise ValueError('Playlist child escaped the authorized playback session')
    return target.geturl()


async def identity(page):
    raw = await page.evaluate('''() => {
      for (const storage of [sessionStorage, localStorage]) {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          if (key && key.startsWith('oidc.user:')) {
            const value = JSON.parse(storage.getItem(key) || 'null');
            if (value?.access_token && value?.profile?.sub) return value;
          }
        }
      }
      return null;
    }''')
    if not raw:
        raise AssertionError('Authenticated browser identity missing')
    return raw


async def login(page, username, password, path='/profiles', button='Sign in'):
    await page.goto(WEB + path)
    await page.get_by_role('button', name=button, exact=True).click()
    await page.locator('#username').fill(username)
    await page.locator('#password').fill(password)
    await page.locator('#kc-login').click()
    # The callback is transient. Wait for the authenticated landing page and
    # usable OIDC state instead of racing callback navigation with fixed sleeps.
    await page.wait_for_url(WEB + '/profiles', timeout=60000)
    await page.wait_for_function('''() => {
      for (const storage of [sessionStorage, localStorage]) {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          if (key && key.startsWith('oidc.user:')) {
            try {
              const value = JSON.parse(storage.getItem(key) || 'null');
              if (value?.access_token && value?.profile?.sub) return true;
            } catch { /* Wait for OIDC persistence to finish. */ }
          }
        }
      }
      return false;
    }''', timeout=15000)
    return await identity(page)
