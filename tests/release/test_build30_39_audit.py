"""Build30: all original not-started IDs audited without pretending completion."""
import csv
from collections import Counter
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]

def rows(file):
    with (ROOT/"docs"/file).open(newline="",encoding="utf-8") as fd:
        return list(csv.DictReader(fd))

def test_39_audit_entries_have_real_acceptance_conditions():
    audit=json.loads((ROOT/"docs/BUILD30_39_ADVANCED_AUDIT.json").read_text())
    old=rows("BUILD29_282_TASKS.csv")
    previous={r["task_id"] for r in old if r["build29_status"]=="Not started / no audited implementation evidence"}
    assert len(previous)==len(audit)==39
    assert previous=={a["id"] for a in audit}
    assert all(a["blocking_dependency"] and a["next_engineering_step"] and a["production_acceptance_evidence"] for a in audit)
    assert sum(a["build30_status"]=="Partially implemented" for a in audit)==6
    assert sum(a["build30_status"]=="Not started / no audited implementation evidence" for a in audit)==33

def test_282_historical_status_fields_and_no_acceptance_overwriting():
    before=rows("BUILD29_282_TASKS.csv")
    after=rows("BUILD30_282_TASKS.csv")
    assert len(before)==len(after)==282
    assert [r["task_id"] for r in before]==[r["task_id"] for r in after]
    assert all(r["acceptance_verified"]=="NO" for r in after)
    for previous,current in zip(before,after):
        for key,val in previous.items():
            assert current[key]==val,(previous["task_id"],key)
    assert dict(Counter(r["build30_status"] for r in after))=={
      "Component CI verified — production acceptance pending":5,
      "Source implemented — runtime unverified":67,
      "Partially implemented":128,
      "Blocked — external execution / operator verification":32,
      "Blocked — external approval / account":17,
      "Not started / no audited implementation evidence":33
    }

def test_only_six_optional_tasks_reclassified_to_partial():
    old={r["task_id"]:r for r in rows("BUILD29_282_TASKS.csv")}
    new={r["task_id"]:r for r in rows("BUILD30_282_TASKS.csv")}
    moved={k for k in old if old[k]["build29_status"]!=new[k]["build30_status"]}
    assert moved=={"F-12","G-14","L-17","L-19","P-02","P-10"}
    assert all(new[k]["build30_evidence_and_blocker"] for k in moved)
