"""Ephemeral, internal-only candidate re-ranking.

Only pre-authorized catalog items may be submitted. This service never queries the
catalog, stores user identity or grants playback access. Java MUST rights-filter
and validate the returned candidate IDs again.
"""
from __future__ import annotations
import hmac
import math
import os
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, ConfigDict, Field, field_validator
from model import Film, Interaction, HybridRecommender
from semantic_search import SearchFilm, search as semantic_search

app = FastAPI(title="ReelNest internal recommendation ranker", docs_url=None, redoc_url=None)

class Candidate(BaseModel):
    model_config = ConfigDict(extra='forbid')
    id: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=250)
    synopsis: str = Field(default='', max_length=2000)
    genre: str = Field(default='', max_length=100)
    language: str = Field(default='', max_length=16)

class RankRequest(BaseModel):
    model_config = ConfigDict(extra='forbid')
    candidates: list[Candidate] = Field(min_length=1, max_length=300)
    seenIds: list[str] = Field(default_factory=list, max_length=300)
    preferredLanguage: str | None = Field(default=None, max_length=16)
    limit: int = Field(default=24, ge=1, le=48)

    @field_validator('candidates')
    @classmethod
    def distinct(cls, entries):
        if len({v.id for v in entries}) != len(entries):
            raise ValueError('duplicate candidates')
        return entries

class RankedItem(BaseModel):
    id: str
    score: float

class RankResponse(BaseModel):
    ranked: list[RankedItem]

def authorized_results(items, allowed: set[str], limit: int, excluded=()):
    """Fail closed if a model returns an unknown, duplicate or non-finite result.

    Inputs have already passed through the Java API's rights-filtered candidate
    list. This second barrier does not replace server-side entitlement checks.
    """
    seen = set(excluded)
    ranked = []
    for item_id, score in items:
        if item_id not in allowed or item_id in seen:
            continue
        if isinstance(score, bool) or not isinstance(score, (int, float)) or not math.isfinite(score):
            continue
        seen.add(item_id)
        ranked.append(RankedItem(id=item_id, score=float(score)))
        if len(ranked) >= limit:
            break
    return RankResponse(ranked=ranked)

@app.get('/health')
def health():
    return {'status':'ok'}

@app.post('/rank', response_model=RankResponse)
def rank(payload: RankRequest, x_reelnest_internal_token: str | None = Header(default=None)):
    secret = os.getenv('RECOMMENDER_INTERNAL_TOKEN', '')
    # Refuse to run unsecured: never allow empty or unset shared secret.
    if len(secret) < 24 or not x_reelnest_internal_token or not hmac.compare_digest(secret, x_reelnest_internal_token):
        raise HTTPException(status_code=401, detail='unauthorized')
    ids = {c.id for c in payload.candidates}
    if any(seen not in ids for seen in payload.seenIds):
        raise HTTPException(status_code=422, detail='seen ID not in eligible candidate set')
    try:
        films = [Film(c.id,c.title,c.synopsis,c.genre,c.language) for c in payload.candidates]
        interactions = [Interaction('request',id) for id in set(payload.seenIds)]
        # Per-request, in-memory fit. No identifying profile ID crosses this boundary.
        model = HybridRecommender().fit(films,interactions)
        ranked = model.rank('request',payload.preferredLanguage,payload.limit,ids)
        return authorized_results(ranked, ids, payload.limit, payload.seenIds)
    except (ValueError, IndexError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


class SearchRequest(BaseModel):
    model_config = ConfigDict(extra='forbid')
    candidates: list[Candidate] = Field(min_length=1, max_length=300)
    query: str = Field(min_length=1, max_length=200)
    limit: int = Field(default=12, ge=1, le=24)

    @field_validator('candidates')
    @classmethod
    def unique_candidates(cls, items):
        if len({v.id for v in items}) != len(items):
            raise ValueError('duplicate candidates')
        return items

@app.post('/semantic-search', response_model=RankResponse)
def semantic(payload: SearchRequest, x_reelnest_internal_token: str | None = Header(default=None)):
    secret=os.getenv('RECOMMENDER_INTERNAL_TOKEN','')
    if len(secret)<24 or not x_reelnest_internal_token or not hmac.compare_digest(secret,x_reelnest_internal_token):
        raise HTTPException(status_code=401,detail='unauthorized')
    if not payload.query.strip():
        raise HTTPException(status_code=422,detail='query cannot be blank')
    allowed={c.id for c in payload.candidates}
    found=semantic_search(payload.query,[SearchFilm(c.id,c.title,c.synopsis,c.genre,c.language)
                                       for c in payload.candidates],payload.limit)
    return authorized_results(found, allowed, payload.limit)
