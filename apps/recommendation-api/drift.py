"""Offline monitoring for bounded categorical feature drift with smoothing."""
from collections import Counter
from math import log

def population_stability_index(reference: list[str], current: list[str], smoothing: float=1e-4) -> float:
    if not reference or not current or smoothing <= 0:
        raise ValueError('nonempty populations and positive smoothing required')
    ref=Counter(reference); cur=Counter(current); categories=ref.keys()|cur.keys()
    n1=len(reference)+len(categories)*smoothing
    n2=len(current)+len(categories)*smoothing
    return sum(((cur[k]+smoothing)/n2-(ref[k]+smoothing)/n1)*
               log(((cur[k]+smoothing)/n2)/((ref[k]+smoothing)/n1)) for k in categories)
