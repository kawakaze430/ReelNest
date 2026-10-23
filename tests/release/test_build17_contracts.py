"""Build 17 integration contracts where a live Spring/PostgreSQL stack is unavailable.
These are source-level guards, not proof of live authentication or Flyway success.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
API=ROOT/'apps/api/src/main'
WEB=ROOT/'apps/web/src'
WORKER=ROOT/'apps/video-worker'


def test_caption_ingest_is_owner_scoped_and_draft_only():
    src=(API/'java/com/reelnest/creators/CreatorCaptionController.java').read_text()
    assert 'creators.requireApproved(jwt)' in src
    assert 'findByIdAndCreatorId(titleId,creator.getId())' in src
    assert '"DRAFT".equals(status)' in src
    assert 'FOR UPDATE' in src
    assert 'title_caption_sources' in src
    assert 'media_assets' in src
    assert 'getBytes(java.nio.charset.StandardCharsets.UTF_8).length>200000' in src
    assert 'public record CaptionView(String language,java.time.Instant updatedAt)' in src
    assert 'public record CaptionView(String language,String srt)' not in src


def test_caption_packaging_is_protected_and_validated():
    src=(WORKER/'worker.py').read_text()
    assert 'package_captions(output,info.duration,caption_sources)' in src
    assert 'validate_package(output,decode_samples=False)' in src
    policy=(API/'java/com/reelnest/playback/PlaybackPolicy.java').read_text()
    assert 'captions/[a-z]{2,3}' in policy
    assert 'safeRelativeMediaPath' in policy
    assert 'relative.endsWith(".m3u8")' not in policy
    assert 'relative.endsWith(".ts")' not in policy


def test_seo_is_explicitly_opt_in_and_rights_filtered():
    migration=(API/'resources/db/migration/V13__explicit_title_seo_approval.sql').read_text()
    assert 'seo_index_approved BOOLEAN NOT NULL DEFAULT FALSE' in migration
    src=(API/'java/com/reelnest/catalog/PublicSeoController.java').read_text()
    for invariant in ("t.status='PUBLISHED'","t.seo_index_approved=TRUE",
                      "r.status='APPROVED'","c.status='APPROVED'",
                      "m.processing_status='READY'",'rt.country_code=?',
                      'r.valid_until>=CURRENT_DATE'):
        assert invariant in src
    admin=(API/'java/com/reelnest/admin/AdminController.java').read_text()
    assert 'SEO_APPROVED' in admin and 'SEO_REVOKED' in admin
    assert 'hasRole(\'ADMIN\')' in admin


def test_unapproved_titles_are_noindex_even_if_api_fails():
    page=(WEB/'app/title/[id]/page.tsx').read_text()
    sitemap=(WEB/'app/sitemap.ts').read_text()
    robots=(WEB/'app/robots.ts').read_text()
    assert 'privateRobots={index:false,follow:false}' in page
    assert 'if(!response.ok)return' in page
    assert 'catch{return' in page
    assert 'seo-index' in sitemap
    assert 'if(!response.ok)break' in sitemap
    assert "'/title/'" not in robots  # robots must allow the noindex response to be crawled


def test_progress_never_claims_ready_before_success():
    migration=(API/'resources/db/migration/V12__media_processing_progress.sql').read_text()
    worker=(WORKER/'worker.py').read_text()
    assert 'progress_percent BETWEEN 0 AND 100' in migration
    assert "progress_stage='READY',progress_percent=100" in worker
    assert "progress_stage='PROBING',progress_percent=1" in worker
    assert 'AND lease_until>NOW()' in worker
    assert 'progress_percent<=%s' in worker
    assert 'update_progress(job, "VERIFYING", 98)' in worker
    assert 'mark_success(job, manifest, duration, variants)' in worker


def test_qoe_replay_is_memory_only_opt_in():
    src=(WEB/'lib/qoe-replay.ts').read_text()
    assert 'private enabled=false' in src
    assert 'this.pending.length>=40' in src
    assert 'head.attempts>=4' in src
    assert 'this.pending=[]' in src
    assert 'localStorage' not in src and 'indexedDB' not in src and 'sendBeacon' not in src
    player=(WEB/'components/protected-player.tsx').read_text()
    assert 'qoeQueue.dispose()' in player
    assert 'qoeQueueRef.current?.setEnabled(prefs.qoeConsent)' in player
