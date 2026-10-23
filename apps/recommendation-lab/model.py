"""Build 06 offline ML lab: TF-IDF metadata retrieval + collaborative item similarity.

Not a live production recommender. Java's rights/age-gated discovery endpoint is the
active service. This lab learns from explicit, authorized catalogs and interactions.
No generated data is claimed to be a real business outcome.
"""
from __future__ import annotations
from dataclasses import dataclass
from collections import defaultdict
from math import log2
from typing import Iterable
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

@dataclass(frozen=True)
class Film:
    id: str
    title: str
    synopsis: str
    genre: str
    language: str

@dataclass(frozen=True)
class Interaction:
    profile_id: str
    film_id: str
    weight: float = 1.0

class HybridRecommender:
    """No persistence of identifiable viewing logs. Everything is held in memory."""
    def __init__(self, content_weight: float = .65):
        if not 0 <= content_weight <= 1:
            raise ValueError('content_weight must be 0..1')
        self.weight = content_weight
        self.films: list[Film] = []
        self.ids: dict[str, int] = {}
        self.history: dict[str, dict[str, float]] = {}
        self.content: np.ndarray | None = None
        self.collaborative: np.ndarray | None = None
        self.popularity: np.ndarray | None = None

    def fit(self, films: Iterable[Film], events: Iterable[Interaction]) -> 'HybridRecommender':
        self.films = list(films)
        self.ids = {f.id: i for i, f in enumerate(self.films)}
        if len(self.ids) != len(self.films):
            raise ValueError('Film IDs must be unique')
        if not self.films:
            raise ValueError('Catalog must contain at least one film')
        text = [f'{f.genre} {f.genre} {f.language} {f.title} {f.synopsis}' for f in self.films]
        features = TfidfVectorizer(stop_words='english',ngram_range=(1,2),max_features=2000).fit_transform(text)
        self.content = cosine_similarity(features)
        n = len(self.films)
        histories: dict[str, dict[str, float]] = defaultdict(dict)
        for event in events:
            if event.film_id not in self.ids or event.weight <= 0:
                continue
            row = histories[event.profile_id]
            row[event.film_id] = min(5.0,row.get(event.film_id,0.0)+event.weight)
        self.history=dict(histories)
        users=list(self.history)
        interaction=np.zeros((len(users),n),dtype=float)
        for u,user in enumerate(users):
            for fid,confidence in self.history[user].items():
                interaction[u,self.ids[fid]]=confidence
        self.popularity=interaction.sum(axis=0)
        if interaction.shape[0]>1:
            # Learn item-item cosine similarity across user behavior; shrink small overlaps.
            self.collaborative=cosine_similarity(interaction.T)
            np.fill_diagonal(self.collaborative, 0)
        else:
            self.collaborative=np.zeros((n,n))
        return self

    def rank(self, profile_id: str, preferred_language: str | None=None,
             limit: int=10, eligible_ids: set[str] | None=None) -> list[tuple[str,float]]:
        if self.content is None or self.collaborative is None or self.popularity is None:
            raise ValueError('fit must be called before rank')
        if limit<1 or limit>100:
            raise ValueError('limit out of range')
        previous=self.history.get(profile_id,{})
        n=len(self.films)
        scores=np.zeros(n,dtype=float)
        interacted=[(self.ids[fid],weight) for fid,weight in previous.items() if fid in self.ids]
        if interacted:
            normalizer=sum(weight for _,weight in interacted)
            for idx,weight in interacted:
                scores+=weight/normalizer*(self.weight*self.content[idx]+(1-self.weight)*self.collaborative[idx])
        # Cold start: weak popularity signal and explicit preferred language.
        peak=max(1.0,float(np.max(self.popularity)))
        scores+=.08*(self.popularity/peak)
        if preferred_language:
            scores+=np.array([.12 if f.language==preferred_language else 0 for f in self.films])
        ranked=[]
        for i,film in enumerate(self.films):
            if film.id in previous: continue  # Recommend unseen films only.
            if eligible_ids is not None and film.id not in eligible_ids: continue
            ranked.append((film.id,round(float(scores[i]),6)))
        return sorted(ranked,key=lambda x:(-x[1],x[0]))[:limit]

def recall_at_k(ranking: list[str],truth: set[str],k: int)->float:
    """Fraction of held-out positives found in top k; 0 when no ground truth."""
    if not truth:return 0.0
    return len(set(ranking[:k])&truth)/len(truth)

def ndcg_at_k(ranking: list[str],truth: set[str],k: int)->float:
    """Binary relevance normalized discounted cumulative gain."""
    if not truth:return 0.0
    dcg=sum(1/log2(i+2) for i,fid in enumerate(ranking[:k]) if fid in truth)
    ideal=sum(1/log2(i+2) for i in range(min(k,len(truth))))
    return dcg/ideal if ideal else 0.0

if __name__=='__main__':
    catalog=[Film('a','Orbit','Space explorers find a signal','Sci-Fi','en'),
             Film('b','Solar Dawn','Scientists travel into space','Sci-Fi','en'),
             Film('c','Monsoon','Family drama in a village','Drama','hi')]
    model=HybridRecommender().fit(catalog,[Interaction('user','a'),Interaction('other','a'),Interaction('other','b')])
    print('Local demonstration recommendation:',model.rank('user','en'))
    print('Results are not production engagement metrics.')
