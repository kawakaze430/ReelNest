# ReelNest Build 36 — Safe contextual-ad preview and native client foundations

Date: 24 October 2026. Source of truth: GitHub `kawakaze430/ReelNest`, branch `main`.

## New implemented source (six original tasks move to partial)
- **Q-01 AVOD preview:** Additive PostgreSQL V27 stores per-profile opt-in, admin-reviewed contextual ad-copy previews and nonbillable event IDs. Admin proposal requires a second distinct administrator for review. No ad media, tracking pixel, third-party click-through or audiovisual insertion. Preview uses `PlaybackService.availability` to enforce current approved content rights, server-configured territory, permitted adult profiles, free title and subscribed users not being targeted. All ad previews are **OFF by default**.
- **Q-02 hybrid offers:** `/api/v1/avod/access-options` reports currently playable/current subscription/preview availability. Actual advertising insertion and TVOD purchases explicitly remain **false**, with no entitlements minted. This is a capability preview, not a billing integration.
- **Q-12 privacy/measurement:** Separate consent for nonbillable client-supplied signals, server event-ID deduplication, Redis request admission limiting, seven-day scheduled retention, opt-out deletion and `Cache-Control: no-store`. Not a certified invalid-traffic detector or billable measurement system.
- **M-04 mobile deep links:** Android/iOS read-only custom-scheme catalog navigation rejects unsafe parameters and never treats links as authentication, money or media-entitlement callbacks. Full native OAuth PKCE not implemented or device-tested.
- **M-06 offline expiry/cleanup:** Android app-private preview-file cleanup and expiry boundary; importantly, **offline playback always returns false** until certified persistent DRM licenses exist. No offline downloaded media or licensed key renewal.
- **M-13 notification consent:** Android 13+ permission prompt appears only on user action. FCM/APNs push delivery, tokens and crash SDK still not integrated.

## Database / code verification
- `V27__contextual_avod_previews.sql` additive migration; legacy V1–V26 untouched.
- Flyway V1–V27 clean and V9→V27 upgrade assertions updated.
- Java `ContextualAdPolicyTest` and `ContextualAdControllerTest` enforce fail-closed paid/child/consent/invalid territory cases.
- Existing GitHub Actions verify Maven, TypeScript/Next production build, worker container, worker tests, Terraform schema and non-production PostgreSQL preflight. Latest results in GitHub Actions; these are **not** live advertising, finance or native-device acceptance.
- New `tests/release/test_build36_contracts.py` checks opt-out rules, migrated schema, native source constraints and absence of payment/DRM grant paths.

## Original 282-task ledger
| Classification | Count |
|---|---:|
| Component CI verified, production acceptance pending | 6 |
| Source implemented, runtime unverified | 67 |
| Partially implemented | 147 |
| Blocked: external execution/verification | 31 |
| Blocked: external account/approvals | 17 |
| **Still not started** | **14** |
| **Total** | **282** |

**No original task has commercial production acceptance (0/282).** Six of the prior 20 unstarted tasks received genuine but bounded source work; 14 remain. The exact 14 and their blocking requirements are in `docs/BUILD36_REMAINING_14.csv`.

## Why 14 cannot be declared complete from CI
Widevine/FairPlay/PlayReady encryption and offline licenses need external key-provider accounts and certified device tests. Multi-region databases require RPO/RTO, consistency and tested failover. Android TV/tvOS protected player needs physical/hardware-based signoff. Live streaming, tenant isolation, HDR/Dolby mastering, and creator financial marketplace require significant new subsystems and commercial governance.

## Product release gates
G1 creator-to-licensed-player integration open. G2 payment refunds/renewal acceptance open. G3 approved AWS staging open. G4 security/restore/privacy/device certification open. G5 licensed countries commercial pilot open.

**Release: NO-GO.** No live charges, AWS resources, paid ads, downloaded licensed media or key-provider integrations were created.
