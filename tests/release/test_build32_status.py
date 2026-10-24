"""Build 32: preserve all original 282 task ids and acceptance evidence."""
from collections import Counter
from pathlib import Path
import csv
ROOT=Path(__file__).resolve().parents[2]
def read(name):
    with (ROOT/"docs"/name).open(newline="",encoding="utf-8") as f:
        return list(csv.DictReader(f))
def test_no_mutation_of_older_backlog_evidence():
    before=read("BUILD31_282_TASKS.csv")
    after=read("BUILD32_282_TASKS.csv")
    assert len(before)==len(after)==282
    assert [x["task_id"] for x in before]==[x["task_id"] for x in after]
    assert all(x["acceptance_verified"]=="NO" for x in after)
    for old,new in zip(before,after):
        for k,v in old.items():
            assert new[k]==v,(old["task_id"],k)
def test_only_Q09_Q10_promoted_from_not_started_to_partial():
    old={x["task_id"]:x for x in read("BUILD31_282_TASKS.csv")}
    now={x["task_id"]:x for x in read("BUILD32_282_TASKS.csv")}
    changed={k for k in old if old[k]["build31_status"]!=now[k]["build32_status"]}
    assert changed=={"Q-09","Q-10"}
    assert now["Q-09"]["build32_status"]=="Partially implemented"
    assert now["Q-10"]["build32_status"]=="Partially implemented"
    assert dict(Counter(v["build32_status"] for v in now.values()))=={
      "Component CI verified — production acceptance pending":5,
      "Source implemented — runtime unverified":67,
      "Partially implemented":131,
      "Blocked — external execution / operator verification":31,
      "Blocked — external approval / account":17,
      "Not started / no audited implementation evidence":31
    }
