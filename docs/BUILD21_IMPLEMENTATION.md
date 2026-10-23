# ReelNest Build 21 — Evidence-Based Source Development

**Date:** 2026-10-22. All prior Build 20 files preserved.

## Status, 282 tasks

| Classification | Build 20 | Build 21 |
|---|---:|---:|
| Source implemented — runtime unverified | 60 | 65 |
| Partially implemented | 102 | 103 |
| Blocked — external execution / operator verification | 40 | 40 |
| Blocked — external approval / account | 17 | 17 |
| Not started / no audited implementation evidence | 63 | 57 |
| **Total** | **282** | **282** |

**Production-acceptance verified: 0 of 282.** Source implementation is not runtime validation.

## The 20 source tasks addressed

- **B-09** Verify lockout, abuse throttling and stolen-token handling: **Partially implemented**. profiles/ProfilePinPolicy.java: backoff/5-fail lockout for PIN challenges; API auth abuse testing pending.
- **B-10** Add independent viewer profiles and PIN-protected adult profile actions: **Partially implemented**. V17,ProfilePinController/ProfileController,ProfileManager: adult PIN set/check and protected deletion; playback/profile switch PIN gate not implemented.
- **C-04** Add unique content identifiers, slugs and canonical URLs: **Partially implemented**. CatalogController by-slug endpoint; canonical URL redirects/metadata not completed.
- **C-06** Add editorial categories, collections, featured rails and scheduling: **Partially implemented**. PublicationScheduleController/PublicationScheduler: approved scheduled release; editorial collections not completed.
- **C-12** Enforce unpublish and rights revocation across search, CDN authorization and recommendations: **Partially implemented**. PublicationScheduler: auto-unpublish expired rights and revoke app sessions; signed CDN cookie lifetime edge unverified.
- **C-14** Implement metadata import/export and bulk operations: **Source implemented — runtime unverified**. CatalogBatchController: 2-admin staging, snapshot fingerprint, atomic DRAFT-only apply, audit; DB runtime unverified.
- **H-01** Verify baseline keyword search, facets and synonyms: **Partially implemented**. TitleAliasController: alias autocomplete suggestions, LIKE escaping; full-text facets/synonyms pending.
- **H-02** Improve multilingual and transliterated title discovery: **Partially implemented**. V18 aliases, TitleAliasController, CatalogBrowser: editorial transliteration aliases and rights-filtered suggestions; full multilingual relevance pending.
- **H-12** Add per-user control for personalization and privacy: **Source implemented — runtime unverified**. V20 PersonalizationController,DiscoveryService,ProfileManager: explicit opt-in/out, removal of event history, no ML when off; integration pending.
- **J-08** Design RTL layout support for applicable languages: **Partially implemented**. globals.css [dir=rtl] style for search/filter; end-to-end RTL design not yet implemented.
- **K-03** Admin role separation, two-person approval for sensitive actions: **Partially implemented**. CatalogBatchPolicy and RefundReviewPolicy require different requester/reviewer; other sensitive workflows still need dual approval.
- **K-04** Bulk catalog updates and metadata import/export: **Source implemented — runtime unverified**. CatalogBatchController + admin-catalog-operations UI: draft-only staged, reviewed bulk apply, no import of rights/published titles.
- **K-05** Rights calendar, expiration alerts and takedown controls: **Partially implemented**. RightsComplianceReportController expiry CSV and PublicationScheduler expiry cleanup; notifications/calendar UI pending.
- **K-10** Refund-support tools with auditable approval workflow: **Partially implemented**. RefundReviewController + V19: 2-admin manual refund-review queue; payment provider refund NOT executed.
- **K-12** Content publishing schedules, embargoes and staged rollouts: **Source implemented — runtime unverified**. PublicationScheduleController, PublicationScheduler, V18 and admin UI: future scheduled release, block on invalid rights; database/E2E pending.
- **K-13** Rights-holder reports and platform audit exports: **Source implemented — runtime unverified**. RightsComplianceReportController: private bounds-checked rights CSV with expiry days/territories and formula escaping.
- **N-05** Define data retention schedule and backup deletion/expiry procedure: **Partially implemented**. RetentionController preview, retention_legal_holds, EventRetentionJob holds-aware cleanup; backup retention procedure pending.
- **N-06** Add consent, cookie tracking preferences and policy versioning: **Partially implemented**. PersonalizationController opt-in/out and ProfileManager control; cookie policy/versioning still pending.
- **N-12** Maintain security incident, legal hold and deletion audit logs: **Partially implemented**. Legal hold add/release audit, system_publication_events, refund review audit; complete incident audit pending.
- **P-15** Create reproducible launch audit/evidence package: **Partially implemented**. Build21 test reports, status changes and checksum manifest; external release evidence pending.

## Implemented this batch

- Server-side adult-profile PIN set/check and deletion protection, PBKDF2 and persistent lockout; DOES NOT YET gate all playback or profile switching.
- Atomic, staged admin CSV metadata edits for existing DRAFT titles, requiring different reviewer, short expiry, checksum, row-fingerprint and audit; no automatic published edits.
- Scheduled titles with approval checks, expiry unpublishing, playback-session revocation and system event records; CDN cookie expiry remains to test live.
- Rights-restricted catalog suggestions using manually reviewed multilingual aliases; no claim of automatic transliteration.
- Profile personalization opt-in/out with consent-controlled ranking and immediate viewer-event cleanup.
- Admin rights-expiry CSV register, dual approval for INTERNAL refund review, legal holds respected during retention, retention report. Refunds are NOT sent to payment provider.
- Next.js editor screens for PIN, personalization, catalog imports and scheduling.

## Verified locally

- 33 compiled standalone Java policy assertions: PIN hashing, failure lockouts, two-person decisions, schedule validation, refund review, SQL search escaping.
- 69 TS/TSX syntax parses without parsing errors. No Next.js production typecheck/build claimed.
- 15 focused Python checks passed (9 Build 21 contracts and 6 Build 20 support policies).
- Previous full offline runner exceeded execution time; results must NOT be marked as passed for Build 21.

## Blocked without local/provider environment

- Maven and Spring component compilation, npm dependency resolution, Next.js typecheck, Docker and Flyway V17–V20 migrations, real account OIDC/Keycloak, payments/credit events, AWS and CloudFront.
- New controllers and migrations are SOURCE IMPLEMENTED only; full integration and business-security tests remain required.
- Each task retains its task ID, source evidence and the operator/Codex queue; no task was automatically marked acceptance-verified.
