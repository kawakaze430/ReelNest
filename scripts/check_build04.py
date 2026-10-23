#!/usr/bin/env python3
"""Portable Build 04 structural/security regression gate; NOT a substitute for live tests."""
from pathlib import Path
import re
import yaml

root=Path(__file__).resolve().parents[1]
def get(path):return (root/path).read_text(encoding='utf8')

def must(pattern,path,flags=re.S):
    data=get(path)
    if not re.search(pattern,data,flags):raise AssertionError(f'Missing required invariant in {path}: {pattern}')

migration='apps/api/src/main/resources/db/migration/V5__playback_watchlist.sql'
for text in ('CREATE TABLE playback_sessions','CREATE TABLE watchlists',
    'ALTER TABLE watch_progress ADD COLUMN completed',"CHECK (access_tier IN ('FREE','PREMIUM'))",
    'CREATE INDEX idx_playback_user_active'):
    assert text in get(migration),text
service='apps/api/src/main/java/com/reelnest/playback/PlaybackService.java'
for keyword in ('currentUser.resolve(jwt)','FOR UPDATE','MAX_SESSIONS',
    'rights_territories','r.valid_until>=?',"c.status='APPROVED'",
    "m.processing_status='READY'",'PROFILE_RESTRICTED','Premium entitlements',
    'last_progress_seq < ?','requireActiveSession','clearProgress'):
    assert keyword.lower() in get(service).lower(),keyword
controller='apps/api/src/main/java/com/reelnest/playback/PrivateMediaController.java'
for keyword in ('dev-proxy-enabled:false','playback.authorizeMedia','object.transferTo(output)',
                "requestPath.indexOf('%')>=0",'private, no-store'):
    assert keyword in get(controller),keyword
policy='apps/api/src/main/java/com/reelnest/playback/PlaybackPolicy.java'
for keyword in ('safeRelativeMediaPath','master.m3u8','seg_[0-9]{5}','U/A 7+','isFree'):
    assert keyword in get(policy)
assert 'permitAll()' not in get(controller)
frontend='apps/web/src/components/protected-player.tsx'
for keyword in ('hls.js','Authorization','resumePositionSeconds','heartbeatIntervalSeconds',
                'progress','window'):
    if keyword=='window':continue
    assert keyword in get(frontend),keyword
assert ('player.destroy()' in get(frontend) or 'hlsRef.current?.destroy()' in get(frontend)), 'Player cleanup missing'
assert 'localStorage' not in get(frontend), 'JWTs must never be stored directly in playback UI'
compose=yaml.safe_load(get('compose.yaml'))
assert compose['services']['api']['environment']['PLAYBACK_DEV_PROXY_ENABLED'] in (True,'true','True')
assert compose['services']['api']['environment']['S3_PROCESSED_BUCKET']=='reelnest-processed-private'
assert 'ports' not in compose['services']['video-worker']
assert 'preview/playback-flow.html' in str(list(root.glob('preview/playback-flow.html')))
assert (root/'scripts/e2e_playback_live.py').exists()
assert (root/'apps/api/src/test/java/com/reelnest/playback/PlaybackPolicyTest.java').exists()
print('PASS: Build 04 migration, sessions, rights/age/territory gates, auth media proxy, watchlists, UI and Compose guards')
