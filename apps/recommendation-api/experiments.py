"""Offline assignment and evaluation utilities; NOT a live experiment switch."""
from __future__ import annotations
import hashlib
import hmac
from math import isfinite

def bucket(subject: str, experiment: str, secret: str, variants: tuple[str, ...] = ('control','treatment')) -> str:
    if not subject or len(subject) > 128 or not experiment or len(experiment) > 128:
        raise ValueError('invalid assignment key')
    if len(secret) < 24 or not variants or len(set(variants)) != len(variants):
        raise ValueError('invalid experiment secret or variants')
    payload = f'{experiment}:{subject}'.encode()
    digest = hmac.new(secret.encode(), payload, hashlib.sha256).digest()
    return variants[int.from_bytes(digest[:8], 'big') % len(variants)]

def summarize_binary(successes: int, trials: int) -> dict[str, float | int]:
    if trials < 0 or successes < 0 or successes > trials: raise ValueError('invalid counts')
    # Wilson interval gives a bounded approximate confidence interval, not causal proof.
    if not trials: return {'trials': 0, 'rate': 0., 'lower95': 0., 'upper95': 1.}
    import math
    p = successes/trials; z=1.96; denom=1+z*z/trials
    center=(p+z*z/(2*trials))/denom
    margin=z*math.sqrt((p*(1-p)+z*z/(4*trials))/trials)/denom
    return {'trials': trials, 'rate': p, 'lower95': max(0.,center-margin), 'upper95': min(1.,center+margin)}
