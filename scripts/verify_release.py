#!/usr/bin/env python3
"""Checks the SHA256 manifest shipped with each full project ZIP."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root/'docs/BUILD_MANIFEST.json').read_text())
for record in manifest['files']:
    path = root / record['path']
    assert path.is_file(), f'Missing {record["path"]}'
    assert hashlib.sha256(path.read_bytes()).hexdigest() == record['sha256'], f'Changed {record["path"]}'
print(f'PASS: {len(manifest["files"])} files match build manifest {manifest["release"]}')
