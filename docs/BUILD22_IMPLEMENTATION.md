# ReelNest Build 22 — Advanced discovery, analytics & creator operations

**Date:** October 22, 2026. Built from the exact cumulative Build 21 archive; prior source files preserved. All 282 task IDs remain unique.

## Updated 282-task status

| Category | Build 21 | Build 22 |
|---|---:|---:|
| Source implemented (runtime unverified) | 65 | 66 |
| Partial source implementation | 103 | 116 |
| Blocked: local/Codex execution | 40 | 40 |
| Blocked: account/approval | 17 | 17 |
| Not started / no evidence | 57 | 43 |
| **Total** | **282** | **282** |

The proposed 80 / 108 / 40 / 17 / 47 totals add to **292**, not 282. We deliberately did not alter the task count or relabel partial work as complete. Source progress is not production acceptance: **0/282 items have full acceptance sign-off**.

## 14 previously unstarted tasks advanced

- **I-06 — Source implemented, integration unverified:** hourly per-title and per-episode aggregate analytics with minimum five distinct consenting profiles.
- **F-11 — Partial:** read-only session anomaly aggregates; no surveillance data or automatic punitive action.
- **G-11 — Partial:** preview-only promotion code policies; **checkout still charges full provider-configured price**.
- **H-09 — Partial:** request-scoped, rights-filtered TF-IDF search; not transformer embeddings.
- **H-13 — Partial:** offline drift measurement only.
- **H-14 — Partial:** deterministic assignment and Wilson intervals; no live randomized experiment platform.
- **H-16 — Partial:** natural-language phrase search of eligible titles; not scene-level search.
- **I-11 — Partial:** consent-gated weekly signup cohorts; no acquisition attribution.
- **I-12 — Partial:** scheduled data-quality checks; no alert integration or event schema registry.
- **I-13 — Partial:** privacy-thresholded country QoE, but not ISP/device benchmarking.
- **J-14 — Partial:** in-app notification inbox, creator expiry alerts and preferences; **no outbound notifications**.
- **K-08 — Partial:** creator-private performance summary with suppressed small cohorts; **no royalty calculations or payouts**.
- **M-11 — Partial:** opt-in D-pad catalog navigation, no native TV apps/device testing.
- **Q-08 — Partial:** grounded title search; **not** an LLM-powered chat assistant.

Additional improvement **K-05**: scheduled rights-expiry inbox alerts for approved creators; calendar and live worker tests outstanding.

## What was implemented in source

1. V21 notification and data-quality tables; V22 promotion quote table. New owner-scoped inbox, read/dismiss, opt-in preferences, and idempotent creator-rights-expiry job. No email or push sending.
2. Admin-only engagement/hourly heatmaps, signup cohorts, episode aggregates, country QoE, quality snapshot history and k-anonymity suppression. No raw viewer IDs in outputs.
3. Admin-only aggregate active-session risk signal that does **not** automatically accuse users or revoke access.
4. Approved creator-only engagement summary with strict owner checks and suppression. No fake or estimated earnings are presented as payable.
5. Private TF-IDF query endpoint in existing FastAPI recommender, called only with already-eligible Java candidates. Returned IDs are re-checked against this allowlist; lexical fallback if recommender unavailable. The eligible candidate window is limited to 48 until a dedicated rights-scoped search endpoint is added.
6. Offline drift and deterministic experiment helpers with tests. No experimental metrics fabricated.
7. Authenticated semantic-discovery UI, private notifications UI, creator performance page, opt-in D-pad mode on browse.
8. Promotion quote API with capped 50% discount, strict code syntax and expiry; display-only, not activated in checkout.

## Tests actually run

- **29** Python recommendation/ML tests passed, including private API search authorization and invalid candidate rejection.
- **28** Python video-worker tests passed with real local FFmpeg packaging.
- **15** standalone Java policy assertions passed (notification, privacy, royalty arithmetic, preview discounts).
- **33** inherited Build 21 Java policy assertions passed.
- **74** frontend TS/TSX files syntactically parsed; 0 syntax errors. This is **not** Next.js typecheck.
- The inherited monolithic `release_gate.py --mode offline` **timed out**. Do **not** treat this as passing.
- Full Maven, Next.js production compilation, Flyway V21–V22 live execution, Docker integration and identity/payment/AWS tests were not available here.

## Important security/operations conditions

- Country restrictions still use the prior configuration; trusted geo enforcement remains a separate P0 external verification blocker.
- Payment promotions are PREVIEW ONLY. Premium rights are still based exclusively on verified captured charges; do not promise quoted discounts.
- Aggregates are suppressed for cohorts with fewer than five distinct consenting profiles. This is a privacy baseline, not an anonymity guarantee.
- The private ML endpoint requires its strong internal token. No model output may introduce a title absent from Java's eligible set.
- Recommended staging verification: compile services, apply V21 & V22, test own-versus-other inbox access, verify D-pad navigation on supported browsers, verify private semantic search and territory restrictions, test all admin analytics under 5 and 5+ consenting profiles, and confirm promotions never alter checkout/payment entitlements.

## Source responsibility vs operator handoff

**ChatGPT can continue:** finish semantic search relevance, language support, creator dashboard, notifications, QoE anomaly dashboards, multi-provider plan abstractions, rights calendar, online experiments and tests.

**Local/Codex must verify:** Maven build; actual Next.js build; SQL migrations V1–V22; Docker boot; Keycloak and profile ownership; live uploads/playback; Razorpay sandbox; CDN geolocation; AWS staging; backups and load testing. **Account/approvals:** payment-provider merchant access, DNS/domain, rights-holder contracts, licensing/DRM and legal clearance.

The 40 local/Codex and 17 external/approval blocked tasks are unchanged, not marked complete.
