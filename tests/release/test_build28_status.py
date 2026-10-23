"""Build 28 historical 282-task tracker audit; never counts component CI as launch."""
from pathlib import Path
from collections import Counter
import csv

ROOT=Path(__file__).resolve().parents[2]

def read(name):
    with (ROOT / "docs" / name).open(newline="",encoding="utf-8") as f:
        return list(csv.DictReader(f))

def test_282_ids_preserved_and_no_production_acceptance():
    before=read("BUILD27_282_TASKS.csv")
    after=read("BUILD28_282_TASKS.csv")
    assert len(before)==len(after)==282
    assert [row["task_id"] for row in before]==[row["task_id"] for row in after]
    assert len({row["task_id"] for row in after})==282
    assert all(row["acceptance_verified"]=="NO" for row in after)
    for old,new in zip(before,after):
        for field,value in old.items():
            assert new[field]==value, (old["task_id"],field)

def test_only_two_task_classifications_advance():
    before={row["task_id"]:row for row in read("BUILD27_282_TASKS.csv")}
    after={row["task_id"]:row for row in read("BUILD28_282_TASKS.csv")}
    changed={id for id in before if before[id]["build27_status"]!=after[id]["build28_status"]}
    assert changed=={"A-08","A-11"}
    assert after["A-08"]["build28_status"]=="Partially implemented"
    assert after["A-11"]["build28_status"]=="Component CI verified — production acceptance pending"
    assert all(after[id]["build28_evidence"] for id in changed)

def test_exact_build28_counts():
    counts=Counter(x["build28_status"] for x in read("BUILD28_282_TASKS.csv"))
    assert dict(counts)=={
      "Component CI verified — production acceptance pending":5,
      "Source implemented — runtime unverified":67,
      "Partially implemented":122,
      "Blocked — external execution / operator verification":32,
      "Blocked — external approval / account":17,
      "Not started / no audited implementation evidence":39,
    }
