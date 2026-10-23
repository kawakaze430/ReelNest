#!/usr/bin/env python3
"""Create an independently deployable, STATIC ReelNest showcase for AWS Amplify.

This never includes source, credentials, admin dashboards or backend endpoints.
Upload deploy/reelnest-amplify-preview.zip directly through Amplify's manual deploy UI.
"""
from __future__ import annotations
import argparse
import hashlib
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def create(root: Path = ROOT, output: Path | None = None) -> tuple[Path, dict[str, str]]:
    preview = root / 'preview'
    static = root / 'deploy' / 'public-preview'
    archive = output or root / 'deploy' / 'reelnest-amplify-preview.zip'
    static.mkdir(parents=True, exist_ok=True)
    # A fixed file allowlist prevents accidentally leaking a developer .env,
    # database dumps, app source, or original upload content.
    for source, dest in [
        ('index.html', 'index.html'),
        ('media/demo.mp4', 'media/demo.mp4'),
    ]:
        p = preview / source
        if not p.is_file():
            raise FileNotFoundError(f'Missing original preview asset: {p}')
        target = static / dest
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, target)
    (static / '404.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Not found | ReelNest</title><body style="font:16px system-ui;background:#090a10;color:#fff;text-align:center;padding:10vh 20px"><h1>Page not found</h1><p>This site is an interactive OTT concept preview, not a streaming service.</p><p><a href="/" style="color:#ffa8a4">Return home</a></p></body></html>''', encoding='utf-8')
    (static / 'robots.txt').write_text('User-agent: *\nDisallow: /\n', encoding='utf-8')
    files = [p for p in sorted(static.rglob('*')) if p.is_file()]
    manifest = {p.relative_to(static).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    archive.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for p in files:
            z.write(p, arcname=p.relative_to(static).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert set(z.namelist()) == set(manifest)
        for name, digest in manifest.items():
            assert hashlib.sha256(z.read(name)).hexdigest() == digest
    return archive, manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=None)
    args = parser.parse_args()
    archive, manifest = create(output=args.output)
    print(f'Created {archive} with {len(manifest)} allowlisted assets')
    for name in manifest:
        print(f'  {name}')
