"""Build37 tracker truth and two bounded video source advances."""
from pathlib import Path
from collections import Counter
import csv
ROOT=Path(__file__).resolve().parents[2]
def rows(n):
    with (ROOT/"docs"/n).open(newline="",encoding="utf-8") as f:return list(csv.DictReader(f))
def test_b37_preserves_all_old_task_columns_and_zero_acceptance():
    old,new=rows("BUILD36_282_TASKS.csv"),rows("BUILD37_282_TASKS.csv")
    assert len(old)==len(new)==282
    assert [x["task_id"] for x in old]==[x["task_id"] for x in new]
    assert all(x["acceptance_verified"]=="NO" for x in new)
    for a,b in zip(old,new):
        for key,value in a.items():assert b[key]==value
def test_exactly_two_not_started_source_promotions_and_12_remain():
    old={r["task_id"]:r for r in rows("BUILD36_282_TASKS.csv")}
    new={r["task_id"]:r for r in rows("BUILD37_282_TASKS.csv")}
    changed={id for id in old if old[id]["build36_status"]!=new[id]["build37_status"]}
    assert changed=={"F-17","Q-03"}
    assert all(new[id]["build37_status"]=="Partially implemented" and
        old[id]["build36_status"]=="Not started / no audited implementation evidence" for id in changed)
    assert len(rows("BUILD37_REMAINING_12.csv"))==12
    assert dict(Counter(r["build37_status"] for r in new.values()))=={
        "Component CI verified — production acceptance pending":6,
        "Source implemented — runtime unverified":67,
        "Partially implemented":149,
        "Blocked — external execution / operator verification":31,
        "Blocked — external approval / account":17,
        "Not started / no audited implementation evidence":12
    }
def test_rights_watermark_is_declared_nonforensic_and_server_decided():
    service=(ROOT/"apps/api/src/main/java/com/reelnest/playback/PlaybackService.java").read_text()
    policy=(ROOT/"apps/api/src/main/java/com/reelnest/playback/VisibleWatermarkPolicy.java").read_text()
    player=(ROOT/"apps/web/src/components/protected-player.tsx").read_text()
    assert "r.rights_basis" in service
    assert "VisibleWatermarkPolicy.requiredFor(asset.rightsBasis())" in service
    assert "LICENSED_DISTRIBUTOR" in policy
    assert "session?.visibleWatermarkRequired&&ready" in player
    assert "session.sessionId.slice(0,8)" in player
    assert "forensic" in policy.lower()
def test_synthetic_live_proof_never_accepts_external_ingest():
    s=(ROOT/"apps/video-worker/live_preview.py").read_text()
    assert 'testsrc2=size=640x360:rate=24' in s
    assert "delete_segments+independent_segments+program_date_time+omit_endlist" in s
    assert "not 3<=duration_seconds<=10" in s
    assert "target.exists() or target.is_symlink()" in s
