"""Build35 status evidence: preserve all original IDs and old claims."""
import csv
from collections import Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def rows(path):
    with (ROOT/"docs"/path).open(newline="",encoding="utf-8") as f:return list(csv.DictReader(f))
def test_original_282_tasks_never_renamed_or_accepted_without_evidence():
    old=rows("BUILD34_282_TASKS.csv");now=rows("BUILD35_282_TASKS.csv")
    assert len(old)==len(now)==282
    assert [r["task_id"] for r in old]==[r["task_id"] for r in now]
    assert all(r["acceptance_verified"]=="NO" for r in now)
    for a,b in zip(old,now):
        for key,value in a.items():assert b[key]==value,(a["task_id"],key)
def test_142_prior_partial_tasks_audited_and_only_h01_component_ci_promotion():
    old={r["task_id"]:r for r in rows("BUILD34_282_TASKS.csv")}
    new={r["task_id"]:r for r in rows("BUILD35_282_TASKS.csv")}
    partial={id for id in old if old[id]["build34_status"]=="Partially implemented"}
    audit=rows("BUILD35_142_PARTIAL_TASK_AUDIT.csv")
    assert len(partial)==len(audit)==142
    assert {r["task_id"] for r in audit}==partial
    changed={id for id in old if old[id]["build34_status"]!=new[id]["build35_status"]}
    assert changed=={"H-01"}
    assert "40074556828" in new["H-01"]["build35_evidence_and_remaining"]
    assert all(r["remaining_acceptance"] and r["gate"] for r in audit)
    assert dict(Counter(x["build35_status"] for x in new.values()))=={
        "Component CI verified — production acceptance pending":6,
        "Source implemented — runtime unverified":67,
        "Partially implemented":141,
        "Blocked — external execution / operator verification":31,
        "Blocked — external approval / account":17,
        "Not started / no audited implementation evidence":20
    }
