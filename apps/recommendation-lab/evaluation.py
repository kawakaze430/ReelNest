"""Chronological, eligibility-aware *offline* recommendation evaluation.

Use authorized, consented, pseudonymous events only. This never trains on future
interactions and does not claim live engagement improvements. A global cutoff
avoids leaking other users' future behavior into the evaluation model.
"""
from __future__ import annotations
from dataclasses import dataclass
from datetime import datetime, timezone
from statistics import mean
from typing import Iterable
from model import Film, Interaction, HybridRecommender, recall_at_k, ndcg_at_k


@dataclass(frozen=True)
class TimedInteraction:
    profile_id: str
    film_id: str
    occurred_at: datetime
    weight: float = 1.0


@dataclass(frozen=True)
class EvaluationReport:
    evaluated_profiles: int
    train_events: int
    test_events: int
    recall_at_k: float
    ndcg_at_k: float
    catalog_coverage: float
    genre_diversity: float
    language_coverage: dict[str, float]


def curate_ranked(ranked: list[tuple[str,float]], films: Iterable[Film], *,
                  eligible_ids: set[str], blocked_ids: set[str] | None = None,
                  pinned_ids: Iterable[str] = (), max_per_genre: int = 2,
                  limit: int = 10) -> list[tuple[str,float]]:
    """Rights-first curation: pins and diversity can NEVER override eligibility.

    The candidate ranking itself is the source of unseen/authorized candidates;
    pinning an arbitrary ID cannot inject a film absent from that ranking.
    """
    if limit < 1 or limit > 100 or max_per_genre < 1:
        raise ValueError('Invalid curation limits')
    metadata = {film.id:film for film in films}
    blocked = blocked_ids or set()
    candidates = {fid:score for fid,score in ranked if fid in eligible_ids and fid in metadata and fid not in blocked}
    order = list(dict.fromkeys([*pinned_ids, *(fid for fid,_ in ranked)]))
    selected: list[str] = []
    counts: dict[str,int] = {}
    for fid in order:
        if fid not in candidates or fid in selected:
            continue
        genre = metadata[fid].genre
        if counts.get(genre,0) >= max_per_genre:
            continue
        selected.append(fid)
        counts[genre] = counts.get(genre,0) + 1
        if len(selected) >= limit:
            break
    # Soft genre cap: backfill only with authorized candidates.
    for fid,_ in ranked:
        if len(selected) >= limit:break
        if fid in candidates and fid not in selected:selected.append(fid)
    return [(fid,candidates[fid]) for fid in selected]


def evaluate_chronological(films: Iterable[Film], events: Iterable[TimedInteraction],
                           cutoff: datetime, *, k: int = 10,
                           eligible_by_profile: dict[str,set[str]] | None = None) -> EvaluationReport:
    if cutoff.tzinfo is None or cutoff.utcoffset() is None:
        raise ValueError('Cutoff must be timezone-aware')
    if not 1 <= k <= 100:
        raise ValueError('Invalid evaluation cutoff k')
    catalog = list(films)
    ids = {f.id for f in catalog}
    if len(ids) != len(catalog) or not catalog:
        raise ValueError('Catalog must be nonempty with unique IDs')
    rows = list(events)
    for row in rows:
        if row.occurred_at.tzinfo is None or row.occurred_at.utcoffset() is None:
            raise ValueError('Every event timestamp must have a timezone')
        if not row.profile_id or not row.film_id or not 0 < row.weight <= 5:
            raise ValueError('Invalid interaction')
    train = [e for e in rows if e.occurred_at < cutoff and e.film_id in ids]
    test = [e for e in rows if e.occurred_at >= cutoff and e.film_id in ids]
    model = HybridRecommender().fit(catalog,[Interaction(e.profile_id,e.film_id,e.weight) for e in train])
    truth:dict[str,set[str]] = {}
    for event in test:
        truth.setdefault(event.profile_id,set()).add(event.film_id)
    recalls, ndcgs, diversities = [], [], []
    recommended:set[str] = set()
    language_recs:dict[str,set[str]] = {}
    film_lookup = {f.id:f for f in catalog}
    eligible_union:set[str] = set()
    for profile, positives in sorted(truth.items()):
        eligible = ids if eligible_by_profile is None else eligible_by_profile.get(profile,set()) & ids
        eligible_union.update(eligible)
        # Exclude previously watched films and unlicensed/age-ineligible titles.
        valid_truth = positives & eligible - set(model.history.get(profile,{}))
        if not valid_truth:continue
        ranked = model.rank(profile,limit=k,eligible_ids=eligible)
        picks = [fid for fid,_ in ranked]
        recalls.append(recall_at_k(picks,valid_truth,k))
        ndcgs.append(ndcg_at_k(picks,valid_truth,k))
        recommended.update(picks)
        genres = {film_lookup[fid].genre for fid in picks}
        diversities.append(len(genres)/len(picks) if picks else 0.0)
        for fid in picks:
            language = film_lookup[fid].language
            language_recs.setdefault(language,set()).add(fid)
    # Denominator is the authorized evaluation catalog, not unavailable titles.
    if eligible_by_profile is None:eligible_union = ids
    language_catalog:dict[str,set[str]] = {}
    for film in catalog:
        if film.id in eligible_union:language_catalog.setdefault(film.language,set()).add(film.id)
    return EvaluationReport(
        evaluated_profiles=len(recalls),train_events=len(train),test_events=len(test),
        recall_at_k=mean(recalls) if recalls else 0.0,
        ndcg_at_k=mean(ndcgs) if ndcgs else 0.0,
        catalog_coverage=len(recommended & eligible_union)/len(eligible_union) if eligible_union else 0.0,
        genre_diversity=mean(diversities) if diversities else 0.0,
        language_coverage={language:len(language_recs.get(language,set()))/len(group)
                           for language,group in sorted(language_catalog.items())}
    )
