"""Unit and source regression tests for Build 14 browser player controls.

Pure preference helpers compile with real TypeScript before being executed in Node.
Do not mistake these checks for end-to-end playback/device verification.
"""
from __future__ import annotations
import os
import pathlib
import shutil
import subprocess
import tempfile
import textwrap

ROOT = pathlib.Path(__file__).resolve().parents[2]
WEB = ROOT / 'apps/web/src'


def test_player_preferences_compiled_and_exercised():
    assert shutil.which('tsc'), 'Node.js + TypeScript required for Build 14 tests'
    assert shutil.which('node'), 'Node.js required for Build 14 tests'
    with tempfile.TemporaryDirectory(prefix='reelnest-player-') as folder:
        build = subprocess.run([
            'tsc', str(WEB/'lib/player-preferences.ts'), '--module', 'commonjs',
            '--target', 'ES2020', '--lib', 'ES2020,DOM', '--skipLibCheck',
            '--outDir', folder,
        ], capture_output=True, text=True, timeout=30)
        assert build.returncode == 0, build.stdout + build.stderr
        js = pathlib.Path(folder)/'player-preferences.js'
        assert js.exists(), 'Compiled player preference helper missing'
        script = textwrap.dedent("""
          const assert=require('node:assert/strict');
          const u=require(process.env.PLAYER_HELPER);
          assert.equal(u.cappedQualityIndex([360,480,720,1080]),1);
          assert.equal(u.cappedQualityIndex([720,1080]),0);
          assert.equal(u.cappedQualityIndex([1080,360,480]),2);
          assert.equal(u.cappedQualityIndex([]),-1);
          assert.equal(u.clampSeek(1,60,-10),0);
          assert.equal(u.clampSeek(58,60,10),60);
          assert.equal(u.clampSeek(20,Infinity,10),30);
          assert.equal(u.clampSeek(NaN,60,10),0);
          assert.equal(u.normalizeLanguage('en-US'),'en-us');
          assert.equal(u.normalizeLanguage('../../oops'),'');
          assert.equal(u.matchingTrack([{id:0,label:'English',language:'en'},
            {id:1,label:'French',language:'fr-FR'}],'fr'),1);
          assert.equal(u.matchingTrack([{id:0,label:'English',language:'en'}],'ja'),-1);
          let x=u.cleanPreferences({speed:100,captionSize:'giant',captionContrast:'red',preferredAudio:'../../',dataSaver:'true'});
          assert.deepEqual(x,u.DEFAULT_PLAYER_PREFERENCES);
          x=u.cleanPreferences({speed:1.25,captionSize:'large',captionContrast:'high',preferredAudio:'hi',preferredSubtitles:'en-US',dataSaver:true});
          assert.equal(x.speed,1.25); assert.equal(x.captionSize,'large');
          assert.equal(x.captionContrast,'high');assert.equal(x.preferredAudio,'hi');
          assert.equal(x.preferredSubtitles,'en-us');assert.equal(x.dataSaver,true);
          assert.equal(u.prefsKey('C1B4B990-934A-40DA-8AB7-C4A86DD82A1D'),
            'reelnest:player:v1:c1b4b990-934a-40da-8ab7-c4a86dd82a1d');
          assert.throws(()=>u.prefsKey('../../../other'),/Invalid/);
          console.log('12 player helper scenarios passed');
        """)
        run = subprocess.run(['node', '-e',script],capture_output=True,text=True,timeout=20,
                             env={**os.environ,'PLAYER_HELPER':str(js)})
        assert run.returncode==0,run.stdout+run.stderr


def test_protected_player_has_safe_and_accessible_controls():
    src=(WEB/'components/protected-player.tsx').read_text()
    for expression in [
        'onTouchEnd={handleTouchEnd}', 'aria-label="Video playback controls"',
        'aria-label="Rewind ten seconds"', 'aria-label="Forward ten seconds"',
        'dataSaver', 'autoLevelCapping', 'chooseAudio', 'chooseSubtitle',
        'captionContrast', 'onKeyDown', 'toggleFullscreen', 'togglePiP',
        "video?.addEventListener('enterpictureinpicture',onEnterPiP)",
        "video?.addEventListener('leavepictureinpicture',onLeavePiP)",
        "video?.removeEventListener('enterpictureinpicture',onEnterPiP)",
        "video?.removeEventListener('leavepictureinpicture',onLeavePiP)",
        'setRetry(v=>v+1)', 'hlsRef.current?.destroy()',
        'document.pictureInPictureEnabled',
    ]:
        assert expression in src, f'Missing player contract {expression}'
    assert "xhr.setRequestHeader('Authorization',`Bearer ${token}`)" in src
    assert "if (isCdn && video.canPlayType('application/vnd.apple.mpegurl'))" in src
    assert 'controls playsInline' in src
