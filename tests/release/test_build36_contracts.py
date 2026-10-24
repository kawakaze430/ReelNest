"""Build36 contract and backlog guards; non-billable preview does not equal production AVOD."""
from collections import Counter
from pathlib import Path
import csv
ROOT=Path(__file__).resolve().parents[2]
def load(n):
    with (ROOT/"docs"/n).open(newline="",encoding="utf-8") as f:return list(csv.DictReader(f))
def test_every_original_id_and_status_preserved_and_none_accepted():
    old,new=load("BUILD35_282_TASKS.csv"),load("BUILD36_282_TASKS.csv")
    assert len(old)==len(new)==282
    assert [x["task_id"] for x in old]==[x["task_id"] for x in new]
    assert all(x["acceptance_verified"]=="NO" for x in new)
    for before,after in zip(old,new):
        for k,v in before.items():assert after[k]==v,(before["task_id"],k)
def test_only_six_previously_unstarted_tasks_advanced_to_partial():
    old={x["task_id"]:x for x in load("BUILD35_282_TASKS.csv")}
    now={x["task_id"]:x for x in load("BUILD36_282_TASKS.csv")}
    changed={x for x in old if old[x]["build35_status"]!=now[x]["build36_status"]}
    assert changed=={"Q-01","Q-02","Q-12","M-04","M-06","M-13"}
    assert all(old[x]["build35_status"]=="Not started / no audited implementation evidence" for x in changed)
    assert all(now[x]["build36_status"]=="Partially implemented" and
               now[x]["build36_evidence_and_remaining"] for x in changed)
    assert len(load("BUILD36_REMAINING_14.csv"))==14
    assert dict(Counter(x["build36_status"] for x in now.values()))=={
      "Component CI verified — production acceptance pending":6,
      "Source implemented — runtime unverified":67,
      "Partially implemented":147,
      "Blocked — external execution / operator verification":31,
      "Blocked — external approval / account":17,
      "Not started / no audited implementation evidence":14
    }
def test_avod_ensures_opt_in_rights_review_no_money_and_limited_retention():
    migration=(ROOT/"apps/api/src/main/resources/db/migration/V27__contextual_avod_previews.sql").read_text()
    ctl=(ROOT/"apps/api/src/main/java/com/reelnest/avod/ContextualAdController.java").read_text()
    job=(ROOT/"apps/api/src/main/java/com/reelnest/avod/AvodPreviewRetentionJob.java").read_text()
    rate=(ROOT/"apps/api/src/main/java/com/reelnest/security/AdmissionRateLimiter.java").read_text()
    for needle in ["DEFAULT FALSE","reviewed_by<>submitted_by","status='REVIEWED'"]:
        assert needle in (migration+ctl)
    for needle in ["playback.availability(jwt,profileId,titleId)","billing.hasPremium(owner)",
                   "CacheControl.noStore()","false,false","ContextualAdPolicy.measureAllowed"]:
        assert needle in ctl,needle
    assert "created_at<NOW()-interval '7 days'" in job
    assert '"/api/v1/avod/preview-events"' in rate
    assert "billing_transactions" not in ctl and "createCheckout" not in ctl
def test_v27_migration_and_mobile_sources_guarded():
    migrations=ROOT/"apps/api/src/main/resources/db/migration"
    nums=sorted(int(p.name.split("__")[0][1:]) for p in migrations.glob("V*__*.sql"))
    assert nums==list(range(1,28))
    android=(ROOT/"apps/mobile/android/app/src/main/java/com/reelnest/catalogpreview/OfflineRights.kt").read_text()
    ios=(ROOT/"apps/mobile/ios/Shared/SafeCatalogLink.swift").read_text()
    assert "return false" in android and "isSymbolicLink" in android
    assert 'url.scheme == "reelnest-preview"' in ios
