from datetime import datetime, timezone, timedelta
from pathlib import Path
import sys
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from model import Film
from evaluation import TimedInteraction,evaluate_chronological,curate_ranked

CATALOG=[Film('a','Orbit','space station','Science','en'),
         Film('b','Moon','space mission','Science','en'),
         Film('c','Harvest','farm village','Drama','hi'),
         Film('d','Rainfall','monsoon village','Drama','hi')]
CUT=datetime(2026,10,1,tzinfo=timezone.utc)


def test_global_time_cutoff_and_metrics():
    events=[TimedInteraction('u','a',CUT-timedelta(days=2)),
            TimedInteraction('other','a',CUT-timedelta(days=1)),
            TimedInteraction('other','b',CUT-timedelta(days=1)),
            TimedInteraction('u','b',CUT+timedelta(days=1))]
    report=evaluate_chronological(CATALOG,events,CUT,k=2,eligible_by_profile={'u':{'a','b'}})
    assert report.train_events==3 and report.test_events==1
    assert report.evaluated_profiles==1
    assert report.recall_at_k==1.0
    assert report.ndcg_at_k==1.0
    assert 0<=report.catalog_coverage<=1
    assert set(report.language_coverage)=={'en'}


def test_future_events_never_change_training_scores():
    base=[TimedInteraction('u','a',CUT-timedelta(days=1)),
          TimedInteraction('u','b',CUT+timedelta(days=1))]
    first=evaluate_chronological(CATALOG,base,CUT,k=2)
    second=evaluate_chronological(CATALOG,base+[
        TimedInteraction('u','b',CUT+timedelta(days=2))],CUT,k=2)
    assert first.recall_at_k==second.recall_at_k
    assert first.ndcg_at_k==second.ndcg_at_k
    assert first.train_events==second.train_events


def test_denies_unauthorized_ground_truth():
    report=evaluate_chronological(CATALOG,[TimedInteraction('u','c',CUT+timedelta(days=1))],
                                  CUT,k=2,eligible_by_profile={'u':{'a','b'}})
    assert report.evaluated_profiles==0
    assert report.recall_at_k==0


def test_curation_cannot_pin_blocked_or_unranked_titles():
    ranked=[('a',.9),('b',.8),('c',.7),('d',.6)]
    result=curate_ranked(ranked,CATALOG,eligible_ids={'a','b','c'},blocked_ids={'b'},
                         pinned_ids=['d','b','c'],max_per_genre=1,limit=3)
    assert [id for id,_ in result]==['c','a']
    assert 'd' not in dict(result) and 'b' not in dict(result)


def test_curation_soft_genre_backfill():
    result=curate_ranked([('a',1),('b',.9),('c',.8)],CATALOG,
                         eligible_ids={'a','b','c'},max_per_genre=1,limit=3)
    assert [id for id,_ in result]==['a','c','b']


def test_rejects_naive_times_and_invalid_bounds():
    with pytest.raises(ValueError):evaluate_chronological(CATALOG,[],datetime(2026,10,1))
    with pytest.raises(ValueError):evaluate_chronological(CATALOG,[],CUT,k=0)
    with pytest.raises(ValueError):evaluate_chronological(CATALOG,[TimedInteraction('u','a',datetime(2026,10,1))],CUT)
    with pytest.raises(ValueError):curate_ranked([],CATALOG,eligible_ids=set(),limit=0)
