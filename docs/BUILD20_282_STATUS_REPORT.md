# ReelNest Build 20 — 282-task source status and blocked-verification lane

**No P0/P1 production acceptance tests are implied by source statuses.**

| Status | Tasks | Meaning |
|---|---:|---|
| Source implemented — runtime unverified | 60 | Source work pending or not yet proven |
| Partially implemented | 102 | Source work pending or not yet proven |
| Blocked — external execution / operator verification | 40 | Requires operator/CI/environment evidence before closure |
| Blocked — external approval / account | 17 | Requires operator/CI/environment evidence before closure |
| Not started / no audited implementation evidence | 63 | Source work pending or not yet proven |

**Total: 282/282. Fully acceptance-verified: 0/282.**

## Tasks newly addressed in Build 20
- **C-13 — Partial:** Rights complaints, administrator review, title unpublish and appeal; anonymous rights-owner intake and complete legal process pending.
- **C-14 — Partial:** Admin-only bounded CSV export plus strict 500-row import preview; bulk apply pending.
- **J-11 — Source implemented:** Existing FAQ plus authenticated support tickets, account links and status history.
- **K-04 — Partial:** Admin-only CSV export and dry-run parser; bulk metadata apply/update pending.
- **K-06 — Partial:** Staff support/rights review queue, resolution and appeals; broader moderation still pending.
- **K-09 — Source implemented:** Private support tickets: new customer UI, Java API, case audit events, quota and database constraints.
- **L-20 — Partial:** Global latency and regional disaster recovery strategy documented; operational sign-off pending.
- **N-15 — Source implemented:** Build 20 media licensing, DRM and geoblocking limitations brief.
- **O-01 — Partial:** Threat model draft with trust boundaries and attack scenarios; security owner sign-off pending.

## Workstream breakdown

| Workstream | Source | Partial | Runtime blocked | External blocked | No evidence |
|---|---:|---:|---:|---:|---:|
| Build, repository & local runtime | 4 | 4 | 8 | 0 | 0 |
| Identity, security & user accounts | 0 | 7 | 6 | 1 | 1 |
| Catalog, metadata, rights & moderation | 3 | 11 | 0 | 1 | 0 |
| Creator upload & media processing | 6 | 9 | 1 | 0 | 2 |
| Netflix-style player & playback | 15 | 5 | 3 | 0 | 1 |
| DRM, entitlement & streaming security | 3 | 5 | 2 | 1 | 8 |
| Subscriptions & international payments | 3 | 6 | 1 | 2 | 5 |
| Search, recommendations & AI | 5 | 7 | 0 | 0 | 5 |
| Analytics & quality of experience | 4 | 5 | 0 | 0 | 4 |
| UI, accessibility & localization | 6 | 5 | 1 | 0 | 2 |
| Creator Studio & administration | 1 | 8 | 0 | 0 | 4 |
| AWS & worldwide distribution | 4 | 10 | 5 | 2 | 4 |
| Mobile, TV & offline viewing | 0 | 1 | 1 | 1 | 12 |
| Privacy, legal & compliance | 1 | 6 | 0 | 8 | 0 |
| Security & engineering quality | 4 | 8 | 5 | 1 | 0 |
| Reliability, performance & recovery | 1 | 5 | 7 | 0 | 2 |
| Advanced innovation & monetization | 0 | 0 | 0 | 0 | 13 |

## What remains blocked — not silently counted as done
Maven compilation and tests; Next.js typecheck/build; Docker/Compose; PostgreSQL V1–V16 clean and upgrade migrations; Keycloak/OIDC and session controls; Razorpay test mode; AWS Terraform/provider, deployment and CDN signed-cookie/geo tests; legal rights and security review; device tests; backup restore/load tests.

The historical source-status audit is retained in `docs/BUILD19_282_TASK_STATUS.csv`. The newer `docs/MASTER_TASK_STATUS_282.csv` is now the canonical tracker.

## Next coding priorities (without claiming blocked verification)

- B-10 profile PIN and age restrictions with server-side enforcement
- C-14/K-04 reviewed bulk metadata import with dry-run validation and audit
- K-12 scheduled publications with expiry/revocation safeguards
- H-02 multilingual search and transliteration with rights filtering
- K-10 refund support workflow with dual approval, provider-backed verification
- Privacy retention policy and legal hold integration for support cases.

## Build 20 release caveats

- New V16 migration has **not run** against PostgreSQL. Its static constraints were tested only.
- New support and moderation endpoints have **not been compiled with Maven** or live-tested. The pure Java policy and source contracts passed.
- An admin unpublish immediately revokes application playback sessions, but previously issued CDN cookies may remain usable until expiry. CDN invalidation/key-rotation must be exercised before a paid launch.
- New account-data exports include private support cases; accounts with support history are deliberately held for privacy retention review rather than erased incorrectly.
- Do not accept live support complaints or payments before identity, security and retention policy verification.
