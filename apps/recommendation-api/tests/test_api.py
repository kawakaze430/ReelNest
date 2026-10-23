import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import pytest
from fastapi.testclient import TestClient
from main import app

client=TestClient(app)
TOKEN='an-internal-long-random-test-secret-1234'
HEADERS={'x-reelnest-internal-token':TOKEN}
EXAMPLES=[
    {'id':'a','title':'Orbital','synopsis':'Space science expedition','genre':'Science fiction','language':'en'},
    {'id':'b','title':'Starbound','synopsis':'Space explorers','genre':'Science fiction','language':'en'},
    {'id':'c','title':'Village','synopsis':'Family drama','genre':'Drama','language':'hi'}]

def test_missing_secret_denied(monkeypatch):
    monkeypatch.delenv('RECOMMENDER_INTERNAL_TOKEN',raising=False)
    assert client.post('/rank',json={'candidates':EXAMPLES},headers=HEADERS).status_code==401

def test_wrong_secret_denied(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN',TOKEN)
    assert client.post('/rank',json={'candidates':EXAMPLES},headers={'x-reelnest-internal-token':'wrong'}).status_code==401

def test_rank_candidates_with_no_identity_leak(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN',TOKEN)
    r=client.post('/rank',json={'candidates':EXAMPLES,'seenIds':['a'],'preferredLanguage':'en','limit':2},headers=HEADERS)
    assert r.status_code==200,r.text
    ids=[x['id'] for x in r.json()['ranked']]
    assert 'a' not in ids and len(ids)==2 and set(ids)=={'b','c'}
    assert all('profile' not in str(x).lower() for x in r.json()['ranked'])

def test_rejects_unlisted_and_duplicate_ids(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN',TOKEN)
    for payload in ({'candidates':EXAMPLES,'seenIds':['unlicensed']},
                    {'candidates':[EXAMPLES[0],EXAMPLES[0]]}):
        assert client.post('/rank',json=payload,headers=HEADERS).status_code==422

def test_health_works_without_secret():
    assert client.get('/health').status_code==200

def test_semantic_rejects_missing_internal_secret(monkeypatch):
    monkeypatch.delenv('RECOMMENDER_INTERNAL_TOKEN', raising=False)
    r=client.post('/semantic-search',json={'candidates':EXAMPLES,'query':'space'},headers=HEADERS)
    assert r.status_code==401

def test_semantic_only_returns_preapproved_candidates(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN', TOKEN)
    limited=[EXAMPLES[0],EXAMPLES[2]]  # Film 'b' intentionally absent (not authorized).
    r=client.post('/semantic-search',json={'candidates':limited,'query':'space science','limit':12},headers=HEADERS)
    assert r.status_code==200,r.text
    assert [v['id'] for v in r.json()['ranked']]==['a']
    assert 'b' not in str(r.json())

def test_semantic_blank_query_rejected(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN', TOKEN)
    r=client.post('/semantic-search',json={'candidates':EXAMPLES,'query':'  '},headers=HEADERS)
    assert r.status_code==422

def test_semantic_duplicate_candidate_rejected(monkeypatch):
    monkeypatch.setenv('RECOMMENDER_INTERNAL_TOKEN', TOKEN)
    r=client.post('/semantic-search',json={'candidates':[EXAMPLES[0],EXAMPLES[0]],'query':'science'},headers=HEADERS)
    assert r.status_code==422
