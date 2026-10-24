"""Build 34 source contracts; production signoff is NOT granted by component tests."""
from pathlib import Path
from collections import Counter
import csv
import subprocess

ROOT=Path(__file__).resolve().parents[2]
def load(name):
    with (ROOT/"docs"/name).open(newline="",encoding="utf-8") as file:
        return list(csv.DictReader(file))

def test_all_prior_evidence_preserved_and_no_production_acceptance():
    before=load("BUILD33_282_TASKS.csv")
    after=load("BUILD34_282_TASKS.csv")
    assert len(before)==len(after)==282
    assert [r["task_id"] for r in before]==[r["task_id"] for r in after]
    assert len({r["task_id"] for r in after})==282
    assert all(r["acceptance_verified"]=="NO" for r in after)
    for old,new in zip(before,after):
        for k,v in old.items():
            assert new[k]==v,(old["task_id"],k)

def test_only_eight_new_tasks_have_partial_implementations():
    old={r["task_id"]:r for r in load("BUILD33_282_TASKS.csv")}
    new={r["task_id"]:r for r in load("BUILD34_282_TASKS.csv")}
    promoted={key for key in old if old[key]["build33_status"]!=new[key]["build34_status"]}
    assert promoted=={"D-13","E-17","G-10","M-02","M-03","M-09","M-10","M-12"}
    for key in promoted:
        assert old[key]["build33_status"]=="Not started / no audited implementation evidence"
        assert new[key]["build34_status"]=="Partially implemented"
        assert new[key]["build34_evidence_and_remaining"]
    assert dict(Counter(r["build34_status"] for r in new.values()))=={
        "Component CI verified — production acceptance pending":5,
        "Source implemented — runtime unverified":67,
        "Partially implemented":142,
        "Blocked — external execution / operator verification":31,
        "Blocked — external approval / account":17,
        "Not started / no audited implementation evidence":20
    }

def test_secure_one_use_handoff_has_no_session_transfer():
    base=ROOT/"apps/api/src/main/java/com/reelnest"
    source=(base/"devices/DeviceHandoffController.java").read_text()
    throttle=(base/"security/AdmissionRateLimiter.java").read_text()
    client=(ROOT/"apps/web/src/components/device-handoff.tsx").read_text()
    for anchor in ("DefaultRedisScript", "redis.call('DEL',KEYS[1])",
                   "DeviceHandoffPolicy.digest", "CacheControl.noStore()",
                   "profiles.findByIdAndUserId", 'Duration.ofMinutes(5)'):
        assert anchor in source
    assert '/api/v1/device-handoff/redeem' in throttle
    assert "PROFILE_EVENT" in client
    assert "access_token" in client  # OIDC auth header for both authenticated devices
    assert "signedCookie" not in source and "manifestUrl" not in source

def test_4k_gate_and_airplay_only_after_authorized_native_session():
    worker=(ROOT/"apps/video-worker/pipeline.py").read_text()
    player=(ROOT/"apps/web/src/components/protected-player.tsx").read_text()
    for snippet in ("REELNEST_ENABLE_4K_SDR_H264","LADDER_4K_SDR",
                    'raise InvalidMedia("HDR','info.height >= 2160'):
        assert snippet in worker
    for snippet in ("webkitShowPlaybackTargetPicker","airplayAvailable",
                    "!session||!ready||!nativeHls", "mode==='Authorized native HLS'"):
        assert snippet in player

def test_tv_public_catalog_does_not_request_private_sessions():
    script=(ROOT/"apps/tv/shared/tv.js").read_text()
    assert "/api/v1/catalog?page=0&size=24" in script
    assert "credentials:'omit'" in script
    assert "textContent" in script
    assert "webk" not in script.lower()
    assert "/api/v1/playback" not in script
