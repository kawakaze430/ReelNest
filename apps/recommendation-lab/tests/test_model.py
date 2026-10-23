import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from model import Film,Interaction,HybridRecommender,recall_at_k,ndcg_at_k
import pytest

FILMS=[Film('a','Orbit','An astronaut studies the lunar surface','Science','en'),
       Film('b','Moon','The lunar astronaut discovers water','Science','en'),
       Film('c','Harvest','Two farmers prepare for rain','Drama','hi'),
       Film('d','Rainfall','A monsoon love story','Drama','hi')]

def test_semantic_content_similarity():
    model=HybridRecommender(1).fit(FILMS,[Interaction('alice','a')]);
    assert model.rank('alice')[0][0]=='b'

def test_collaborative_boost():
    interactions=[Interaction('alice','a'),Interaction('bob','a'),Interaction('bob','d'),
        Interaction('charlie','c'),Interaction('charlie','d')]
    model=HybridRecommender(.0).fit(FILMS,interactions)
    assert model.rank('alice')[0][0]=='d'

def test_cold_start_language_and_eligible_catalog():
    model=HybridRecommender().fit(FILMS,[])
    ids=[id for id,_ in model.rank('new','hi',eligible_ids={'c','b','d'})]
    assert ids[0] in ('c','d') and 'a' not in ids

def test_never_recommend_seen_titles():
    model=HybridRecommender().fit(FILMS,[Interaction('user','a')]);
    assert 'a' not in dict(model.rank('user'))

def test_metrics_are_bounded():
    assert recall_at_k(['a','b','c'],{'b','d'},2)==.5
    assert 0<ndcg_at_k(['a','b','c'],{'b','d'},3)<1
    assert ndcg_at_k([],set(),5)==0

def test_reject_invalid_arguments():
    with pytest.raises(ValueError):HybridRecommender(2)
    with pytest.raises(ValueError):HybridRecommender().fit([],[])
    with pytest.raises(ValueError):HybridRecommender().fit([FILMS[0],FILMS[0]],[])
    with pytest.raises(ValueError):HybridRecommender().fit(FILMS,[]).rank('u',limit=0)
