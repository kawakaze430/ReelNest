# ReelNest Build 20 — Implementation and acceptance evidence

This release continues the Build 19 audited cumulative source, without pretending that previously unavailable external tests ran.

## New or extended source features

- Authenticated customer ticket creation, personal case history, resolution and a single appeal per resolved case.
- Admin/moderator case queue with locked state transitions and immutable audit event entries.
- Administrator-only rights-complaint unpublish, clearing title SEO approval and revoking app-level playback sessions transactionally; CDN cookie expiry/invalidation still requires operator verification.
- V16 support-case and event tables with foreign-key constraints, quota indexes and status checks.
- 5-per-24h database-backed case limit and Redis-based burst limits; ownership checks on personal case routes.
- Customer support page and moderator administration page.
- User data export includes private case records; erasure is conservatively held when support/audit retention requires review.
- Administrator-only read-only catalog CSV export with spreadsheet formula injection defenses.
- **New strict admin-only catalog CSV dry-run preview**: bounded to 100 KB/500 rows, duplicate UUID and metadata validation, no writes or actual bulk import.
- Threat model, territory/DRM/media rights limitation report and single-region-global-CDN resilience strategy.
- Complete 282-row canonical source status tracker and 57-item external verification queue.

## Locally executed verification

- `bash scripts/test_build20_case_policy_java.sh`: **17 policy assertions passed**.
- `bash scripts/test_build20_catalog_csv_java.sh`: **9 formula-safety assertions passed**.
- `bash scripts/test_build20_import_preview_java.sh`: **8 CSV parser assertions passed**.
- `PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest tests/release/test_build20_cases.py -q`: **6 tests passed**.
- `node scripts/check_ts_syntax.mjs`: **67 TS/TSX files parse, 0 syntax errors** (not semantic compilation).
- Existing offline release evidence file records **21/21 check groups passing before the last CSV-preview addition**. Its new isolated Java test passed; no new complete-suite claim is made.
- 282/282 task IDs unique; counts match the website dashboard data and status JSON.

## NOT EXECUTED or NOT VALIDATED

Full Maven compile/test, Next.js typecheck/production compilation, Docker builds, PostgreSQL V16 migration, real Keycloak sessions, Razorpay sandbox transactions, Terraform provider validation, AWS, CloudFront, account deletion integration, device testing and human security/legal approval.

## Priority follow-up for implementation here

- B-10: server-enforced adult profile PIN/protected actions.
- K-04/C-14: operator-reviewed catalog import/apply with validation, preview/version checks, rollback and audit (do not apply dry-run metadata yet).
- K-12: safe scheduled publishing and expiry.
- H-02: multilingual search, rights and age-safe result filtering.
- K-10: refund-support handling with dual approvals and provider validation.

## Release rule

**No live paid OTT launch until the 12 external production release gates have independent evidence.**
