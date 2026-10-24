"""Build31 audit: isolated real MinIO test is NOT full G1 acceptance."""
from collections import Counter
import csv
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
def read(name):
    with (ROOT/"docs"/name).open(newline="",encoding="utf-8") as f:
        return list(csv.DictReader(f))

def test_all_282_ids_unchanged_and_acceptance_not_promoted():
    prev,now=read("BUILD30_282_TASKS.csv"),read("BUILD31_282_TASKS.csv")
    assert len(prev)==len(now)==282
    assert [x["task_id"] for x in prev]==[x["task_id"] for x in now]
    assert len({x["task_id"] for x in now})==282
    assert all(x["acceptance_verified"]=="NO" for x in now)
    for before,after in zip(prev,now):
        for k,value in before.items():
            assert after[k]==value,(before["task_id"],k)

def test_only_direct_storage_evidence_advances_D01():
    prev={x["task_id"]:x for x in read("BUILD30_282_TASKS.csv")}
    curr={x["task_id"]:x for x in read("BUILD31_282_TASKS.csv")}
    changed={k for k in prev if prev[k]["build30_status"]!=curr[k]["build31_status"]}
    assert changed=={"D-01"}
    assert curr["D-01"]["build31_status"]=="Partially implemented"
    assert curr["D-02"]["build31_status"]=="Partially implemented"
    assert "40064689818" in curr["D-01"]["build31_evidence"]
    assert dict(Counter(x["build31_status"] for x in curr.values()))=={
      "Component CI verified — production acceptance pending":5,
      "Source implemented — runtime unverified":67,
      "Partially implemented":129,
      "Blocked — external execution / operator verification":31,
      "Blocked — external approval / account":17,
      "Not started / no audited implementation evidence":33,
    }

def test_development_s3_images_not_withdrawn_quay_registry():
    compose=(ROOT/"compose.yaml").read_text()
    workflow=(ROOT/".github/workflows/minio-multipart.yml").read_text()
    assert "quay.io/minio/" not in compose+workflow
    assert "cgr.dev/chainguard/minio:latest" in compose
    assert "cgr.dev/chainguard/minio-client:latest-dev" in compose
    assert "cgr.dev/chainguard/minio:latest" in workflow
    assert "REELNEST_DISPOSABLE_MINIO" in workflow
    assert "-p 127.0.0.1:9000:9000" in workflow
    assert "if: always()" in workflow
