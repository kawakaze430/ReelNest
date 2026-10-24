"""Offline Build29 source contracts; never substitute for live multipart E2E."""
from pathlib import Path
import csv
ROOT=Path(__file__).resolve().parents[2]
def read(path):
    return (ROOT/path).read_text()

def test_server_part_listing_is_scoped_and_uncached():
    service=read("apps/api/src/main/java/com/reelnest/uploads/UploadService.java")
    controller=read("apps/api/src/main/java/com/reelnest/uploads/UploadController.java")
    snapshot=read("apps/api/src/main/java/com/reelnest/uploads/UploadPartSnapshot.java")
    assert "Row r=owned(jwt,titleId,id)" in service
    assert "ensureDraft(titleId,r.creatorId())" in service
    assert 'expiresAt' not in snapshot
    assert 'response.isTruncated()' in snapshot
    assert "part.size()" in snapshot
    assert '@GetMapping("/{id}/parts")' in controller
    assert "CacheControl.noStore()" in controller
    assert "UploadPartSnapshot.validate" in service
    assert "expires_at>NOW()" in service

def test_paused_upload_remains_open_and_retries_are_bounded():
    ui=read("apps/web/src/components/creator-studio.tsx")
    assert "attempt<=3" in ui
    assert "Uploading" in ui
    assert "Resume upload" in ui
    assert "Cancel upload" in ui
    assert "skipExisting" in ui
    assert "uploadFileSample" in ui
    assert "window.localStorage.removeItem(key)" in ui
    assert "method:'DELETE'" in ui
    assert "Abort incomplete uploads. A retry starts a fresh" not in ui

def test_original_282_task_ids_and_acceptance_preserved():
    with (ROOT/"docs/BUILD28_282_TASKS.csv").open(newline="") as f:
        previous=list(csv.DictReader(f))
    with (ROOT/"docs/BUILD29_282_TASKS.csv").open(newline="") as f:
        latest=list(csv.DictReader(f))
    assert len(previous)==len(latest)==282
    assert [row["task_id"] for row in previous]==[row["task_id"] for row in latest]
    assert all(row["acceptance_verified"]=="NO" for row in latest)
    for old,new in zip(previous,latest):
        assert all(new[key]==val for key,val in old.items())
    row=next(row for row in latest if row["task_id"]=="D-02")
    assert row["build29_status"]=="Partially implemented"
    assert "multipart" in row["build29_evidence"].lower()
