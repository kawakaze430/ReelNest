# ReelNest Build 23 — Source development and acceptance evidence

## Status: SOURCE IMPLEMENTATION, **NOT PRODUCTION READY**

**Baseline**: the exact cumulative Build 22 archive. Builds 01–22 remain in the tree. This is the next **cumulative** source release; no user/Codex external state was assumed verified.

## Features implemented in this release

1. **Editorial collections (C-06, J-10)**: Flyway V23, admin-owned draft/publish/archive lifecycle, bounded/orderable catalog rails, original title rights/media checks, catalog UI `/collections`, admin UI `/admin/editorial`. Metadata-only; playback remains independently gated. Slug/date policy assertions.
2. **Scheduled channels / EPG (Q-04 partial)**: Flyway V24, admin draft programming, schedule overlap detection under channel-row lock, 48-hour public guide `/channels`, rights-gated program metadata. **Not a live FAST broadcast**: no live ingest, timed channel playback, ad breaks, monetization or SSAI.
3. **Invite-only watch parties (Q-05 partial)**: Flyway V25, cryptographically random 192-bit invitation, SHA-256 digest at rest, per-user/profile party membership, independent playback eligibility for create/join/poll/sync, 10-person room limit, host-only optimistic-version playhead edits, auto-expiry. Viewer UI `/watch-party`, link from eligible title. **Five-second polling, not frame-accurate realtime sync**; no moderation/chat or playback URLs shared.
4. **Optional DASH/fMP4 VOD packager (D-09 partial)**: standalone `dash_packaging.py`, rejects unsafe external MPD references, validates local media segments. Actual FFmpeg synthetic encoding tested. **Not wired into production worker/CDN/player**. No DRM encryption.
5. **Privacy-safe installable PWA shell (M-01 source)**: service worker plus public offline fallback, static-only caching. Private, auth, API, account, creator, party and media routes are not cached. Runtime installation across real devices still pending.
6. **Localization groundwork (J-07/J-08 partial)**: English/Hindi/Arabic navigation dictionaries, local preference selection, basic `lang` and `dir` changes and RTL layout. Other routes, SEO language, server-side translated catalog data and screen-reader verification still pending.
7. **Canonical title alias (C-04 partial)**: public slug route redirects to the canonical title UUID after bounded server resolution. Full SEO/country checks remain at canonical details and playback.
8. **Keyboard accessibility (J-06 partial)**: focus-visible outlines, skip-to-content link and reduced-motion support; full WCAG audit and screen-reader testing pending.
9. **DRM browser diagnostic (F-16 partial)**: manual EME key-system checks for Widevine, FairPlay and PlayReady. These do not establish licensing or encrypted playback support.
10. **Accessible route controls**: typed client pages for collections, channel guide, admin editor and watch parties; loading/errors and protected admin requests.

## Tests executed, with evidence

- Video-worker test suite: 30 passing tests, including 2 new FFmpeg DASH tests using synthetic video.
- Recommendation/ML API tests: 8 passing tests.
- Java standard-library policy assertions: compiled and run with `javac`/`java` (editorial slug/date, schedule overlap and duration, invitation digests, expiry and position bounds).
- TypeScript/TSX syntax parse: 82 source files, no parse errors (not a full `tsc --noEmit` with resolved dependencies).
- New source-contract tests: run with `python -m unittest discover -s tests/release -p test_build23_contracts.py -v`.
- Full Maven compilation/Next production compilation, Flyway V23–V25 execution against PostgreSQL, actual OAuth2 + entitlement + invite-room E2E, Safari/TV, deployment/DRM/billing NOT RUN.

## Operator and Codex acceptance queue (not completed)

| Priority | Task | Acceptance gate |
|---|---|---|
| P0 | `mvn -B verify` and `npm ci && npm run typecheck && npm run build` | Successful clean compile with reviewed dependencies |
| P0 | `docker compose up --build -d` and fresh+upgrade Flyway V1–V25 | No schema/startup errors |
| P0 | Two-account rights test | Unentitled member cannot join/poll room, guessed UUID denied, guest cannot change host state |
| P0 | Adversarial EPG tests | Overlaps rejected; rights-expired/unpublished titles excluded, one-country visibility aligned to edge trust |
| P0 | Watch-party browser test | 10 participants max, 4h expiry, host version conflict 409, personal player authorization required |
| P1 | DASH CDN/player integration | Only after valid media access + proper support on target devices |
| P1 | Offline PWA real-device test | No authenticated payload/media cached; offline shell works without leaking private information |
| P1 | Localization QA | Actual Hindi/Arabic layout, keyboard controls, full route translation and screen readers |
| P0 | Existing 57 blocked operator/provider tasks | See `docs/BUILD23_OPERATOR_CODEX_BLOCKERS.md` |

## Explicit scope limits

This release does **not** complete all 159 previously partial/unstarted tasks. Each feature has source code, but many acceptance requirements are still unverified. No live AWS resources were provisioned; no payments were attempted; no copyrighted content was used. Real licenced content, geolocation and commercial DRM remain required for a paid worldwide launch.
