"""Build 15 offline behavior tests. Never represents full Spring/Next/Docker integration."""
from __future__ import annotations
import os
from pathlib import Path
import subprocess
import tempfile
import textwrap

ROOT=Path(__file__).resolve().parents[2]
WEB=ROOT/'apps/web/src'
API=ROOT/'apps/api/src/main/java/com/reelnest'
MIG=ROOT/'apps/api/src/main/resources/db/migration/V10__player_cues_preferences_and_qoe.sql'


def test_playback_java_policies_compile_and_execute():
    with tempfile.TemporaryDirectory(prefix='reelnest-b15-java-') as folder:
        args=['javac','-d',folder,
              str(API/'playback/PlaybackCues.java'),str(API/'playback/ResumePolicy.java'),
              str(ROOT/'tests/release/PlaybackEnhancementsSmoke.java')]
        p=subprocess.run(args,capture_output=True,text=True,timeout=30)
        assert p.returncode==0,p.stdout+p.stderr
        p=subprocess.run(['java','-cp',folder,'PlaybackEnhancementsSmoke'],capture_output=True,text=True,timeout=30)
        assert p.returncode==0,p.stdout+p.stderr
        assert '20 playback enhancement policy assertions passed' in p.stdout


def test_playback_typescript_policies_compile_and_execute():
    with tempfile.TemporaryDirectory(prefix='reelnest-b15-ts-') as folder:
        p=subprocess.run(['tsc',str(WEB/'lib/playback-enhancements.ts'),str(WEB/'lib/player-preferences.ts'),
                          str(WEB/'lib/site-origin.ts'),
                          '--module','commonjs','--target','ES2020','--lib','ES2020,DOM',
                          '--skipLibCheck','--outDir',folder],capture_output=True,text=True,timeout=30)
        assert p.returncode==0,p.stdout+p.stderr
        script=textwrap.dedent("""
            const assert=require('node:assert/strict');
            const path=require('node:path');
            const lib=process.env.TS_BUILD;
            const x=require(path.join(lib,'playback-enhancements.js'));
            const p=require(path.join(lib,'player-preferences.js'));
            const site=require(path.join(lib,'site-origin.js'));
            const cues={introStartSeconds:0,introEndSeconds:55,recapStartSeconds:60,
              recapEndSeconds:80,creditsStartSeconds:500};
            assert.deepEqual(x.skipAction(cues,20,600),{label:'Skip Intro',targetSeconds:55});
            assert.deepEqual(x.skipAction(cues,70,600),{label:'Skip Recap',targetSeconds:80});
            assert.deepEqual(x.skipAction(cues,550,600),{label:'Skip Credits',targetSeconds:599.5});
            assert.equal(x.skipAction(cues,200,600),null);
            assert.equal(x.skipAction(cues,-1,600),null);
            assert.equal(x.skipAction({...cues,introEndSeconds:999},10,600),null);
            assert.equal(x.skipAction(cues,10,NaN),null);
            assert.equal(x.hlsRecoveryAction('networkError',undefined,0),'network');
            assert.equal(x.hlsRecoveryAction('mediaError',undefined,1),'media');
            assert.equal(x.hlsRecoveryAction('networkError',401,0),'stop');
            assert.equal(x.hlsRecoveryAction('networkError',403,0),'stop');
            assert.equal(x.hlsRecoveryAction('networkError',undefined,2),'stop');
            assert.equal(x.hlsRecoveryAction('keySystemError',undefined,0),'stop');
            assert.equal(x.boundedElapsedMs(10,110),100);
            assert.equal(x.boundedElapsedMs(100,10),null);
            assert.equal(x.boundedElapsedMs(0,4000000),3600000);
            assert.equal(x.telemetryErrorCode('frag-load:error 403'),'FRAG_LOAD_ERROR_403');
            assert.equal(p.cleanPreferences({qoeConsent:'true'}).qoeConsent,false);
            assert.equal(p.cleanPreferences({qoeConsent:true}).qoeConsent,true);
            assert.equal(p.DEFAULT_PLAYER_PREFERENCES.qoeConsent,false);
            assert.equal(site.publicSiteOrigin('https://stream.example.org'),'https://stream.example.org');
            assert.equal(site.publicSiteOrigin('https://user:pass@host.com'),'http://localhost:3000');
            assert.equal(site.publicSiteOrigin('http://example.com'),'http://localhost:3000');
            console.log('23 TypeScript playback/SEO policy assertions passed');
        """)
        p=subprocess.run(['node','-e',script],capture_output=True,text=True,timeout=30,
                         env={**os.environ,'TS_BUILD':folder})
        assert p.returncode==0,p.stdout+p.stderr


def test_server_ownership_privacy_and_idempotency_contracts():
    migration=MIG.read_text()
    assert 'REFERENCES profiles(id) ON DELETE CASCADE' in migration
    assert 'REFERENCES playback_sessions(id) ON DELETE CASCADE' in migration
    assert "qoe_consent BOOLEAN NOT NULL DEFAULT FALSE" in migration
    assert 'CREATE TABLE media_cues' in migration
    assert 'intro_start_seconds IS NOT NULL AND intro_end_seconds IS NOT NULL' in migration
    assert 'recap_start_seconds IS NOT NULL AND recap_end_seconds IS NOT NULL' in migration
    assert 'CREATE TABLE playback_qoe_events' in migration
    service=(API/'playback/PlaybackService.java').read_text()
    assert 'ResumePolicy.resumePosition' in service
    assert 'Processed media duration unavailable' in service
    assert 'ResumePolicy.boundedProgress' in service
    assert 'A newer playback session owns progress' in service
    assert 'requireActiveSession(user,sessionId)' in service
    assert 'if(!Boolean.TRUE.equals(consent))' in service
    assert 'FOR SHARE' in service
    assert 'if(existing!=null)' in service and 'return; // exact retry is idempotent' in service
    assert 'ON CONFLICT(id) DO NOTHING' in service
    prefs=(API/'playback/PlayerPreferencesController.java').read_text()
    assert 'requireOwner(jwt,profileId)' in prefs
    assert 'DELETE FROM playback_qoe_events WHERE profile_id=?' in prefs
    assert 'Maximum' not in prefs  # no unsafe expansion of profile creation rules
    qoe=(API/'playback/PlaybackQoeController.java').read_text()
    assert 'UUID eventId' in qoe and 'String errorCode' in qoe
    assert 'String url' not in qoe and 'String ip' not in qoe
    admin=(API/'admin/AdminController.java').read_text()
    assert '@PreAuthorize("hasRole(\'ADMIN\')")' in admin
    assert 'cues.validFor(duration)' in admin
    assert 'COUNT(DISTINCT profile_id)' in admin
    assert 'sessions<5' in admin
    assert 'new QoeSummary(0,0,0,null,null,false)' in admin
    assert 'getBigDecimal(4)' in admin


def test_player_has_network_recovery_opt_in_and_skip_ui():
    src=(WEB/'components/protected-player.tsx').read_text()
    for expected in ['skipAction(session?.cues','hlsRecoveryAction(data.type,status,recoveryAttempts)',
                     'player.recoverMediaError()','player.startLoad()',
                     "emitQoe('STARTUP'", "emitQoe('STALL'", "emitQoe('QUALITY_SWITCH'",
                     'qoeConsent','viewer-skip-cue','maxBufferLength:30','backBufferLength:30',
                     'prefsVersionRef.current','player-preferences']:
        assert expected in src,expected
    assert 'Share optional playback quality metrics linked to this viewer profile' in src
    assert 'MediaCueEditor' in (WEB/'components/review-console.tsx').read_text()
    assert 'navigator.sendBeacon(' not in src and 'Never send a bearer token' in src


def test_public_sitemap_avoids_private_and_unlicensed_routes():
    robots=(WEB/'app/robots.ts').read_text()
    sitemap=(WEB/'app/sitemap.ts').read_text()
    for path in ['/admin/','/watch/','/account/','/creator/']:
        assert path in robots
    # Build 17: eligible title URLs can be crawled to read rights-gated metadata;
    # all other title URLs carry a server-rendered noindex directive.
    assert "'/title/'" not in robots
    assert 'privateRobots' in (WEB/'app/title/[id]/page.tsx').read_text()
    assert "['','/browse','/pricing']" in sitemap
    assert 'seo-index' in sitemap and 'if(!response.ok)break' in sitemap
    for private in ['watch','admin','account','title']:
        assert (WEB/f'app/{private}/layout.tsx').exists()
