"""Regression checks for the reconciled master backlog (source-only != accepted)."""
from pathlib import Path
import csv
import re

ROOT=Path(__file__).resolve().parents[2]
DOCS=ROOT/'docs'

def test_original_master_preserves_282_unchecked_identifiers():
    s=(DOCS/'MASTER_IMPLEMENTATION_CHECKLIST.md').read_text()
    ids=re.findall(r'^- \[([ x])\] \*\*([A-Q]-\d\d)\*\*',s,re.M)
    assert len(ids)==282
    assert len({id for _,id in ids})==282
    assert all(check==' ' for check,_ in ids)

def test_priority_view_selects_117_existing_distinct_ids():
    master=(DOCS/'MASTER_IMPLEMENTATION_CHECKLIST.md').read_text()
    master_ids=set(re.findall(r'^- \[[ x]\] \*\*([A-Q]-\d\d)\*\*',master,re.M))
    rows=list(csv.DictReader((DOCS/'MASTER_TASK_STATUS_282.csv').open()))
    assert len(rows)==282
    assert all(row['task_id'] in master_ids for row in rows)
    selected=[r['task_id'] for r in rows if r['priority_117']=='yes']
    assert len(selected)==117
    assert len(set(selected))==117
    assert all(row['acceptance_status']=='NOT_VERIFIED' for row in rows)
    assert 'not a claim to recover' in (DOCS/'PRIORITY_117_TASKS.md').read_text()
