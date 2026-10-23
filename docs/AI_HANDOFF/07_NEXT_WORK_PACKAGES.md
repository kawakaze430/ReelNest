# 07 — Prioritized work batches and completion criteria

**P0 foundation, only local/Codex/operator can complete end-to-end**
1. Build and dependency review: A-03–A-11, A-14. Acceptance: Maven/npm lock+typecheck/build, clean Docker and both Flyway fresh/upgrade runs.
2. Authentication: B-01–B-15, especially B-07/B-10/B-13. Acceptance: two-account isolation, PKCE, admin MFA, token/logout revocation, PIN everywhere needed and email/recovery with real Keycloak.
3. Media: D-01 and D-16/17, E-01/02/24, F-01/06. Acceptance: original video upload→FFmpeg→rights review→private CDN→real multi-device playback; unauthorized origin/media rejected.
4. Money: G-01–G-09. Acceptance: real Razorpay TEST event correctness + rights/age/geo/premium gate, not a mock success callback.
5. Infrastructure and law: L-01–L-15, N and O/P critical tasks. Acceptance: reviewed Terraform staging, tested CloudFront georights, backups, security and licensed catalog.

**Next source-development batches for a coding agent without external credentials**
- Finish profile PIN enforcement, parental controls, token/session security on EVERY profile-scoped endpoint; add tests including malicious actors.
- Finish subtitle/caption/alternate audio lifecycle management including creator revisions, admin review and media packaging.
- Complete production-grade rights-aware multilingual search, suggestions and personalized recommendation experiments/telemetry with consent.
- Complete creator editorial/collections CMS, scheduled channels, rights-expiry notifications, refund-support workflows and royalty statement calculations (without executing real payments).
- Complete watch-party moderation/privacy and live state sync, cross-device preferences, TV keyboard accessibility and PWA leak tests.
- Add Apple/Android/TV client scaffolds only as meaningful native projects with reproducible build/test instructions; real app-store approval and DRM device tests remain blocked.
- Add battle-tested operations: testable runbooks, SLO/error budgets, backup scripts, incident alert definitions, load-test manifests and rollback procedures.

**Long-horizon global premium extensions (do not imply they exist yet)**
- Native Android/iOS/tvOS/Android TV/Tizen/webOS; secure offline DRM if licensed.
- Truly live ingest/FAST channels and SSAI, not merely EPG metadata.
- Commercial licensed Widevine/FairPlay/PlayReady, watermarking and device-specific tests.
- Global localization, subtitles/voice-over, multi-currency billing/tax, rights matrices, expanded support and data retention per territory.
- Multi-region active/passive or active/active architectures ONLY if budget, consistency, legal and measured global performance support it.

**Task ownership:** assistant implements source and offline tests; local/Codex proves integrated runtime; owner approves/accounts/licensing; qualified reviewers handle independent legal/privacy/security sign-off.
