#!/usr/bin/env python3
"""Regression contracts for Build 09 + 10 (offline, not a compiler)."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
def read(path):return (ROOT/path).read_text()
def require(cond, msg):
    if not cond: raise AssertionError(msg)

media=read('infrastructure/aws/media.tf')
service=read('infrastructure/aws/services.tf')
variables=read('infrastructure/aws/variables.tf')
webdocker=read('apps/web/Dockerfile')
webvalidator=read('apps/web/scripts/validate-public-env.mjs')
guard=read('apps/api/src/main/java/com/reelnest/security/DeploymentGuard.java')
policy=read('apps/api/src/main/java/com/reelnest/security/DeploymentPolicy.java')
privacy=read('apps/api/src/main/java/com/reelnest/privacy/PrivacyController.java')
require(re.search(r'restriction_type\s*=\s*"whitelist"', media)
        and re.search(r'locations\s*=\s*\[\s*var\.playback_country\s*\]', media),
        'CloudFront geo whitelist missing')
require(re.search(r'variable\s+"reviewed_geo_enforcement"\s*\{[^}]*\bdefault\s*=\s*false\b', variables),
        'Geo attestation must be default-deny')
require('PLAYBACK_CDN_GEO_REVIEWED' in service and 'PLAYBACK_TRUSTED_COUNTRY' in service,'API/CDN territory setting missing')
require(re.search(r'\bvalue\s*=\s*var\.playback_country\b', service),'Configured country mismatch')
require('reelnest_app' in service and 'SPRING_FLYWAY_ENABLED' in service and 'runtime_db' in service,
        'Production must run with separate, non-migrating database role')
require('master_user_secret[0].secret_arn' not in service, 'ECS secret reading must exclude DB master secret')
require('APP_DEPLOY_MODE' in service and re.search(r'\bvalue\s*=\s*var\.environment\b', service),'Deployment guard not wired')
require('node scripts/validate-public-env.mjs' in webdocker,'Frontend production env check not wired')
require('new URL(raw)' in webvalidator and 'localhost' in webvalidator,'Web production env validator missing')
require('DeploymentPolicy.validate(' in guard and 'geoReviewed' in guard,'Backend startup guard not wired')
require('if (devProxy)' in policy and 'if (!geoReviewed)' in policy,'Prod playback must fail closed')
require('IN_REVIEW|REJECTED' in privacy and 'IN_REVIEW|FULFILLED|REJECTED' not in privacy,
        'Privacy workflow must not falsely claim data erased')
require((ROOT/'scripts/live_smoke.py').is_file() and (ROOT/'scripts/release_gate.py').is_file(),
        'Read-only live smoke and release gate required')
require('web' in read('compose.yaml') and 'api' in read('compose.yaml'), 'Local stack lost')
print('PASS: Build 09-10 offline production guard, CDN, frontend, privacy and Docker contracts')
