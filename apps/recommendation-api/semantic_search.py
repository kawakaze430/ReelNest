"""Private, request-scoped semantic/lexical retrieval over ALREADY RIGHTS-FILTERED catalog.

TF-IDF is a lexical embedding baseline, not neural semantic understanding or a licensed-content judge.
Never request access to the complete catalog or retain query/user information.
"""
from __future__ import annotations
from dataclasses import dataclass
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

@dataclass(frozen=True)
class SearchFilm:
    id: str
    title: str
    synopsis: str = ''
    genre: str = ''
    language: str = ''

def search(query: str, films: list[SearchFilm], limit: int = 12) -> list[tuple[str, float]]:
    query = query.strip()
    if not 1 <= len(query) <= 200 or not 1 <= limit <= 24:
        raise ValueError('Invalid search query or limit')
    if len(films) > 300 or len({f.id for f in films}) != len(films):
        raise ValueError('Catalog too large or duplicate candidate ID')
    if not films:
        return []
    text = [f'{f.title} {f.title} {f.genre} {f.genre} {f.synopsis} {f.language}' for f in films]
    try:
        vectors = TfidfVectorizer(ngram_range=(1, 2), strip_accents='unicode',
                                  max_features=4000, lowercase=True).fit_transform([query, *text])
    except ValueError:  # e.g. only punctuation or empty vocabulary
        return []
    scores = cosine_similarity(vectors[0], vectors[1:]).ravel()
    ranked = [(f.id, round(float(s), 6)) for f, s in zip(films, scores) if s > 0.000001]
    return sorted(ranked, key=lambda x: (-x[1], x[0]))[:limit]
