"""Build26 historical task ID and evidence consistency, not production acceptance."""
import csv
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
OLD=ROOT/'docs/BUILD25_282_TASKS.csv'
NEW=ROOT/'docs/BUILD26_282_TASKS.csv'
COUNT_EXPECTED={
    'Component CI verified — production acceptance pending':4,
    'Source implemented — runtime unverified':67,
    'Partially implemented':121,
    'Blocked — external execution / operator verification':34,
    'Blocked — external approval / account':17,
    'Not started / no audited implementation evidence':39,
}

def read(path):
    with path.open(newline='',encoding='utf-8') as file:
        return list(csv.DictReader(file))

def test_build26_retains_all_original_ids_and_history():
    previous=read(OLD)
    current=read(NEW)
    assert len(previous)==len(current)==282
    assert [x['task_id'] for x in previous]==[x['task_id'] for x in current]
    assert len({x['task_id'] for x in current})==282
    assert all(x['acceptance_verified']=='NO' for x in current)
    for before,after in zip(previous,current):
        for key,value in before.items():
            assert after[key]==value,(key,before['task_id'])

def test_build26_exactly_twenty_progress_items():
    rows=read(NEW)
    updated=[r for r in rows if r['build26_batch20']=='YES']
    assert len(updated)==20
    assert all(r['build26_evidence_and_remaining'].strip() for r in updated)
    assert all(r['build26_batch20']=='NO' for r in rows if r not in updated)
    assert {r['task_id'] for r in updated}=={
      'A-01','A-02','A-04','A-05','A-06','A-07','A-09','A-12','A-13',
      'B-10','C-14','D-05','D-16','E-16','F-09','H-04','H-06',
      'L-02','O-18','P-15'}

def test_build26_status_totals_and_no_premature_acceptance():
    rows=read(NEW)
    status=Counter(row['build26_status'] for row in rows)
    assert dict(status)==COUNT_EXPECTED
    assert sum(status.values())==282
    verified={r['task_id'] for r in rows
              if r['build26_status']=='Component CI verified — production acceptance pending'}
    assert verified=={'A-04','A-05','A-07','A-09'}
