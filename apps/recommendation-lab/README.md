# ReelNest Build 06 — Offline hybrid ML lab

This is a real scikit-learn TF-IDF + co-interaction cosine recommender with
cold-start language handling, allowed-catalog filtering, Recall@K and NDCG@K.
It does **not** receive customer data or unlock content. The production-facing
Spring Boot discovery endpoint currently implements deterministic explainable
ranking with server-enforced rights and age filters. Connecting trained models
to that gateway is a later task and requires offline dataset evaluation and
monitoring. Never train on customer data without an appropriate legal basis.

Run: `python3 -m pytest apps/recommendation-lab/tests -q`

Requires NumPy, scikit-learn, and pytest; see requirements.txt.

### Build 18: chronological offline evaluation and rights-first curation

`evaluation.py` adds `TimedInteraction`, `evaluate_chronological`, and
`curate_ranked`. The evaluator uses one timezone-aware **global** cutoff, trains
only on earlier interactions, holds out later interactions, and reports aggregate
Recall@K, NDCG@K, eligible catalog coverage, genre diversity, and coverage by
language. Per-profile eligibility filters prevent unavailable titles from being
counted as valid recommendations or relevant ground truth. The curator may pin or
suppress candidates and impose a soft per-genre cap, but cannot inject an item
that was absent from the already authorized candidate ranking.

Use only authorized, consented, pseudonymous event samples. This is an offline
research tool, **not** a production experiment or a claim of increased retention.
No real user events are bundled in this repository. The live API remains responsible
for filtering catalog rights, age and subscription before ranking.
