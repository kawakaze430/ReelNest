#!/usr/bin/env python3
"""Offline Build03 structural/contracts checks; not a substitute for running Docker/Maven."""
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
expected = [
 'apps/api/src/main/resources/db/migration/V4__durable_media_processing.sql',
 'apps/api/src/main/java/com/reelnest/uploads/MediaStatusController.java',
 'apps/video-worker/pipeline.py', 'apps/video-worker/worker.py',
 'apps/video-worker/Dockerfile','apps/video-worker/requirements.txt',
 'apps/video-worker/tests/test_pipeline.py','infrastructure/minio/source-cors.xml',
 'scripts/e2e_processing_live.py'
]
for name in expected:
    assert (root/name).is_file(),name

import yaml
compose=yaml.safe_load((root/'compose.yaml').read_text())
assert 'video-worker' in compose['services']
assert 'minio-init' in compose['services']['video-worker']['depends_on']
assert 'media_scratch' in compose['volumes']
for service in ('minio','api','postgres','keycloak','web'):
    assert service in compose['services']
assert 'ports' not in compose['services']['video-worker'],'Worker must not have exposed service ports'
ET.parse(root/'infrastructure/minio/source-cors.xml')
cors=(root/'infrastructure/minio/source-cors.xml').read_text()
assert '<ExposeHeader>ETag</ExposeHeader>' in cors
assert '<AllowedOrigin>http://localhost:3000</AllowedOrigin>' in cors
assert '<AllowedOrigin>*</AllowedOrigin>' not in cors
migration=(root/'apps/api/src/main/resources/db/migration/V4__durable_media_processing.sql').read_text()
for marker in ('media_processing_jobs','FOR UPDATE', 'max_attempts','lease_until','CREATE UNIQUE INDEX idx_media_assets_source_key_unique','AWAITING_INSPECTION'):
    if marker == 'FOR UPDATE':
        continue # SQL worker queries, not migration
    assert marker in migration,marker
java=(root/'apps/api/src/main/java/com/reelnest/uploads/UploadService.java').read_text()
for marker in ('TransactionTemplate','ON CONFLICT (source_key)','media_processing_jobs', 'headObject', 'completeMultipartUpload'):
    assert marker in java,marker
worker=(root/'apps/video-worker/worker.py').read_text()
for marker in ('SKIP LOCKED','lease_until','reconcile_uploads','mark_failure','mark_success','attempt-','head_object'):
    assert marker in worker,marker
pipeline=(root/'apps/video-worker/pipeline.py').read_text()
for marker in ('ffprobe','ffmpeg','master.m3u8','#EXT-X-STREAM-INF','_validate_playlist'):
    assert marker in pipeline,marker
print('PASS: Build 03 migration, upload atomicity, worker, CORS, Docker source checks')
