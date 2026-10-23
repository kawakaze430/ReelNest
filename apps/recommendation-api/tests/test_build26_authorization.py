"""Build 26: malicious recommendation output must never widen catalog eligibility."""
import math
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import main
from fastapi.testclient import TestClient

CLIENT=TestClient(main.app)
SECRET="build26-long-internal-secret-for-tests"
HEADERS={"x-reelnest-internal-token":SECRET}
FILMS=[
  {"id":"safe","title":"Licensed title","language":"en"},
  {"id":"safe2","title":"Another licensed title","language":"hi"},
  {"id":"seen","title":"Previously viewed title","language":"en"},
]

def test_rank_rejects_unapproved_duplicates_seen_and_nonnumeric(monkeypatch):
    monkeypatch.setenv("RECOMMENDER_INTERNAL_TOKEN",SECRET)
    monkeypatch.setattr(main.HybridRecommender,"fit",lambda self,*args:self)
    monkeypatch.setattr(main.HybridRecommender,"rank",
        lambda self,*args:[("unlicensed",1.0),("safe",float("nan")),
                          ("safe",0.9),("safe",0.8),("seen",0.7),
                          ("safe2",float("inf")),("safe2",0.6)])
    resp=CLIENT.post("/rank",json={"candidates":FILMS,"seenIds":["seen"],"limit":3},headers=HEADERS)
    assert resp.status_code==200,resp.text
    assert resp.json()=={"ranked":[{"id":"safe","score":0.9},{"id":"safe2","score":0.6}]}

def test_rank_never_exceeds_client_limit(monkeypatch):
    monkeypatch.setenv("RECOMMENDER_INTERNAL_TOKEN",SECRET)
    monkeypatch.setattr(main.HybridRecommender,"fit",lambda self,*args:self)
    monkeypatch.setattr(main.HybridRecommender,"rank",
        lambda self,*args:[("safe",0.9),("safe2",0.8),("seen",0.7)])
    resp=CLIENT.post("/rank",json={"candidates":FILMS,"limit":1},headers=HEADERS)
    assert resp.status_code==200
    assert [r["id"] for r in resp.json()["ranked"]]==["safe"]

def test_semantic_fail_closed_on_unlisted_ids_and_invalid_scores(monkeypatch):
    monkeypatch.setenv("RECOMMENDER_INTERNAL_TOKEN",SECRET)
    monkeypatch.setattr(main,"semantic_search",
        lambda *args:[("unlicensed",100.0),("safe",float("inf")),
                      ("safe2",0.3),("safe2",0.2),("safe",0.1)])
    response=CLIENT.post("/semantic-search",json={"candidates":FILMS,"query":"licensed"},
                         headers=HEADERS)
    assert response.status_code==200,response.text
    assert [r["id"] for r in response.json()["ranked"]]==["safe2","safe"]

def test_no_secret_disables_hardened_ranker(monkeypatch):
    monkeypatch.delenv("RECOMMENDER_INTERNAL_TOKEN",raising=False)
    response=CLIENT.post("/rank",json={"candidates":FILMS},headers=HEADERS)
    assert response.status_code==401

def test_scoring_barrier_rejects_bool_string_and_nonfinite():
    result=main.authorized_results([
        ("a",True),("a","0.9"),("a",math.inf),("a",0.4),("a",0.3),
        ("b",-math.inf),("b",0.2)],{"a","b"},2)
    assert [(r.id,r.score) for r in result.ranked]==[("a",0.4),("b",0.2)]
