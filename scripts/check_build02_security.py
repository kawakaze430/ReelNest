#!/usr/bin/env python3
"""Offline contract probes. NOT a replacement for live authorization tests."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
api = root/'apps/api/src/main/java/com/reelnest'
admin=(api/'admin/AdminController.java').read_text()
assert '@PreAuthorize("hasRole(\'ADMIN\')")' in admin
assert 'processing_status=\'READY\'' in admin
assert 'r.status()' in admin
uploads=(api/'uploads/UploadService.java').read_text()
for concept in ('creators.requireApproved(jwt)', 'findByIdAndCreatorId', 'presignUploadPart',
                'listParts(', 'completeMultipartUpload(', 'abortMultipartUpload(', 'headObject('):
    assert concept in uploads, f'Missing API boundary {concept}'
assert 'source/' in uploads
rights=(api/'rights/RightsService.java').read_text()
assert 'Locale.getISOCountries()' in rights
assert 'findByIdAndCreatorId' in rights
m=(root/'apps/api/src/main/resources/db/migration/V3__creator_rights_and_uploads.sql').read_text()
assert 'CREATE UNIQUE INDEX idx_upload_intents_active' in m
assert 'UNIQUE REFERENCES users(id)' in m
assert 'rights_valid_interval' in m
# Ensure the browser never forwards its Keycloak bearer token to a signed S3 URL.
web=(root/'apps/web/src/components/creator-studio.tsx').read_text()
assert 'fetch(signed.url,{method:\'PUT\',body:chunk})' in web
assert 'authorization:' not in web.lower()
assert 'await refreshSelected(selected)' in web
print('PASS: admin role guard, rights/creator ownership gates, private multipart flow and UI restrictions present')
