# ReelNest Build 35 — Licensed catalog parity and 142-task audit

**24 October 2026** — source of truth `kawakaze430/ReelNest`, branch `main`.
Build 35 tackles critical public catalog authorization and search; it does **NOT** complete the other 141 partially implemented advanced tasks.

## Implemented and tested

1. All public list/ID/slug catalog routes now require `PUBLISHED` title, `APPROVED` creator, `APPROVED` and currently valid content rights, configured server-side territory in `rights_territories`, plus a `READY` media asset with private HLS manifest. The previous browse query checked only `PUBLISHED` and could expose revoked or unlicensed metadata.
2. One consistent repository-level SQL visibility predicate covers list, by-slug and by-ID; the search query escapes SQL LIKE wildcards, matches reviewer-approved regional aliases, and validates a MOVIE/SERIES type facet.
3. The Next.js catalog browser now queries the backend for live keyword, genre, language, type and alias search, paginates beyond the first 48 titles, and fails closed (does not show cached potentially revoked results) on a search failure. An offline-only fictional demo preview remains clearly labeled.
4. **Real disposable API + PostgreSQL:** the new scoped `scripts/test_public_catalog_revocation_live.py` creates one synthetic licensed title and verifies that suspending its creator, revoking rights, removing country rights or marking processed media not READY immediately removes it from public browse and individual routes; it also verifies approved aliases and facet narrowing. The entire synthetic fixture is deleted with scoped UUIDs.
5. CI: `PostgreSQL Flyway G1 preflight` run [40074556828](https://github.com/kawakaze430/ReelNest/actions/runs/40074556828) PASSED; regular build checks [40074556772](https://github.com/kawakaze430/ReelNest/actions/runs/40074556772) include Java source tests, Next.js typecheck/build, security and offline tests.

## Status claims
**H-01** moves from partially implemented to **component CI verified, production acceptance pending**. Other 141 previously partial tasks remain partial. Improved source for **C-12, H-02, H-06, L-16** does not discharge their distinct live media/CDN/ML/geo acceptance. All 282 IDs and earlier evidence retained unchanged.

| Classification | Count |
|---|---:|
| Component CI verified — still no release signoff | 6 |
| Source implemented — runtime unverified | 67 |
| Partially implemented | 141 |
| Blocked — operator execution | 31 |
| Blocked — accounts/approvals | 17 |
| Not started | 20 |
| **Total** | **282** |

## What remains
- Real **G1** entire creator → rights → multipart → FFmpeg HLS → review → signed-in viewer with wrong-profile/expired-rights/expired-session denials.
- **G2** test-only charges, renewal, refunds, disputes, verified entitlement changes.
- **G3** approved AWS staging/VPC/CDN/Redis/postgres, monitoring/cost and geolocation boundary.
- **G4** end-to-end security, accessibility, rights revocation across caches, privacy and disaster recovery.
- **G5** licensed commercial territories, DRM vendor/device matrices, TV/mobile app stores, advertising, royalties and live broadcasts.

The public catalog uses a configured **server-side territory**, *not independently verified user geolocation*. This remains a deployment blocker. No AWS deploy, charges, production data mutation, purchased DRM licenses or live commercial streaming occurred in Build 35.

**Release: NO-GO — production accepted: 0/282.**

See `docs/BUILD35_142_PARTIAL_TASK_AUDIT.csv` for a complete item-by-item audit with prior evidence, acceptance gap and suggested testing gate.
