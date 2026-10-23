"""Offline static checks for the ReelNest distribution.
Does not replace Maven, Next.js, Docker, or browser runtime tests.
"""
import json
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
required = [
    'compose.yaml',
    'apps/api/pom.xml',
    'apps/api/src/main/resources/db/migration/V1__initial_schema.sql',
    'apps/api/src/main/resources/db/migration/V2__demo_catalog.sql',
    'apps/api/src/main/java/com/reelnest/ReelNestApplication.java',
    'apps/web/package.json',
    'apps/web/src/app/page.tsx',
    'apps/web/src/app/browse/page.tsx',
    'apps/web/src/components/catalog-browser.tsx',
    'infrastructure/keycloak/reelnest-realm.json',
    'preview/index.html',
    'apps/api/src/main/resources/db/migration/V3__creator_rights_and_uploads.sql',
    'apps/api/src/main/java/com/reelnest/uploads/UploadService.java',
    'apps/api/src/main/java/com/reelnest/rights/RightsService.java',
    'apps/web/src/components/creator-studio.tsx',
    'apps/web/src/components/review-console.tsx',
    'docs/PROJECT_STATE.md',
]
for path in required:
    assert (root / path).is_file(), f'Missing {path}'
for path in root.rglob('*.json'):
    json.loads(path.read_text())
from xml.etree import ElementTree
ElementTree.parse(root / 'apps/api/pom.xml')
realm = json.loads((root/'infrastructure/keycloak/reelnest-realm.json').read_text())
assert realm['realm'] == 'reelnest'
assert any(c['clientId']=='reelnest-web' for c in realm['clients'])
assert len(realm['users']) == 2
assert any('ADMIN' in u.get('realmRoles', []) for u in realm['users'])
backend = (root/'apps/api/src/main/resources/db/migration/V2__demo_catalog.sql').read_text()
frontend = (root/'apps/web/src/lib/demo-data.ts').read_text()
for suffix in range(1,9):
    uid = f'00000000-0000-4000-8000-{suffix:012d}'
    assert uid in backend and uid in frontend, f'Demo catalog mismatch {uid}'
try:
    import yaml
except ImportError:
    print('PyYAML not installed: skipping YAML parsing')
else:
    with (root/'compose.yaml').open() as handle:
        compose = yaml.safe_load(handle)
    assert {'api','web','postgres','keycloak','redis','rabbitmq','minio','minio-init'}.issubset(compose['services'])
    assert 'postgres' in compose['services']['api']['depends_on']
    assert 'minio-init' in compose['services']['api']['depends_on']
    with (root/'.github/workflows/checks.yml').open() as handle:
        yaml.safe_load(handle)
migration = (root/'apps/api/src/main/resources/db/migration/V3__creator_rights_and_uploads.sql').read_text()
for table in ('creators','content_rights','rights_territories','upload_intents','audit_logs'):
    assert f'CREATE TABLE {table}' in migration
upload_api = (root/'apps/api/src/main/java/com/reelnest/uploads/UploadService.java').read_text()
for marker in ('presignUploadPart','completeMultipartUpload','abortMultipartUpload','headObject'):
    assert marker in upload_api
print('PASS: Build 02 files, JSON, SQL checks, XML, catalog parity and YAML checks')
