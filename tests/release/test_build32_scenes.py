"""Build32 source and migration guard; real reviewer and playback still need G1."""
from pathlib import Path
import csv
ROOT=Path(__file__).resolve().parents[2]

def test_editor_review_and_entitlement_are_both_required():
    ctrl=(ROOT/"apps/api/src/main/java/com/reelnest/playback/SceneMarkerController.java").read_text()
    assert '@PreAuthorize("hasRole(\'ADMIN\')")' in ctrl
    assert "playback.availability(jwt,profileId,titleId)" in ctrl
    assert "if(!allowed.playable())" in ctrl
    assert "CacheControl.noStore()" in ctrl
    assert "READ" in ctrl and "processing_status='READY'" in ctrl
    assert "SceneMarkerPolicy.validate" in ctrl

def test_migration_is_v26_only_and_old_schema_not_rewritten():
    m=ROOT/"apps/api/src/main/resources/db/migration"
    versions=sorted(int(p.name.split("__")[0][1:]) for p in m.glob("V*__*.sql"))
    assert versions==list(range(1,28))
    sql=(m/"V26__editor_reviewed_scene_markers.sql").read_text()
    assert "CREATE TABLE media_scene_markers" in sql
    assert "REFERENCES media_assets(id) ON DELETE CASCADE" in sql

def test_player_scene_seek_stays_behind_authenticated_session():
    c=(ROOT/"apps/web/src/components/protected-player.tsx").read_text()
    assert "session&&token&&profileId&&<SceneExplorer" in c
    assert "if(!auth.isAuthenticated || !token)" in c
    assert "function AuthorizedPlayer({titleId,profileId,token" in c
    assert "canSeek={ready}" in c
    assert "Number.isFinite(v.duration)" in c
    a=(ROOT/"apps/web/src/components/review-console.tsx").read_text()
    assert "<SceneMarkerEditor" in a
