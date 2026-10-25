# Owner inputs and remaining acceptance work

GitHub `kawakaze430/ReelNest`, branch `main`, is canonical. Work directly on main, reconcile newer commits and push normally. Do not create branches, worktrees or PRs, force-push, or treat component checks as complete acceptance.

## Access restored: 26 October 2026

Runtime revision **7** now contains the prepared download rules. Actual MinIO server/client pulls and GitHub log/artifact downloads succeeded. Normal `git push origin main` succeeded, publishing all three pending commits through `a534c48c2cb8fcf366af334c835be088421c0dc6`. The earlier write-authentication and download-host blockers are resolved; no additional access-settings action is currently required.

The downloaded old G1 evidence confirmed that unsupported `mc cors set` caused initialization failure. Its removal is already published. At `a534c48`, all nine component jobs, PostgreSQL checks and Android compilation/lint passed. Full integration advanced through startup, V1–V27, browser identity, account security, real CORS, captions, multipart upload, worker READY and rights approval, then failed at publication confirmation.

The concrete cause was an empty HTTP 200 publication response: the frontend parsed it as JSON and reported an error after the backend published the title. The focused repair returns HTTP 204 for publication and three identical admin actions, preserving their existing guards. Regression tests and full Maven verification pass; the next automatic CI run must verify the repaired complete workflow, playback and restore. Inspect current main and actual run results before any further dispatch.

## Technical scope and deferred legal acceptance

The owner has explicitly deferred **legal acceptance**. Keep it pending; do not invent approval or weaken rights controls. Continue technical checks using only original synthetic media, disposable databases and test accounts.

A separate real Keycloak/PostgreSQL free-viewer erasure probe passed 17 checks with complete fixture cleanup. That component result does not establish billing/creator retention, all external processors, production MFA/email, backup retention, or legal acceptance.

The technical inputs below remain necessary for external verification. Existing instructions still prohibit financial transactions, billable cloud deployment and production data operations in this task. Complete reviewable preparation before seeking any later authorization. Current commands and criteria are in [the main-only testing prompt](CODEX_NEXT_TESTING_PROMPT.md).

## External release gates require named prerequisites

| Gate | Required owner input or external evidence | Work that can then proceed |
| --- | --- | --- |
| G2 payment/provider | Authorized Razorpay TEST account/configuration and explicit TEST-transaction scope, entered securely | Provider captures/failures, genuine signed webhooks, renewal/cancellation/refund/reconciliation and entitlement transitions |
| G3 AWS staging | Authorized cloud identity, reviewed region/domain scope and spending budget | Reviewable staging plan, owner-approved provisioning, HTTPS OIDC and real S3/CloudFront delivery checks |
| Native clients | Hosted native build results; macOS/Xcode and Android runners, then supported physical devices and signing accounts | Unsigned compilation/lint first; installation, focus/accessibility, protected media and store/device acceptance separately |
| Required DRM/offline playback | Widevine/FairPlay/PlayReady provider agreements, certificates/keys, approved packaging policy and supported hardware | Encrypted packaging, licensing/renewal/revocation and certified online/offline playback |
| G4 security/recovery/legal | Qualified independent review, defined load/recovery targets, approved original/licensed content and jurisdiction-specific policies | Security remediation, measured recovery/load evidence and signed rights/privacy/consumer approvals |
| G5 territorial launch | G0–G4 accepted, licensed country/content matrix and operating approval | Controlled pilot and separately approved country expansion |

The standing instructions prohibit financial transactions, paid cloud deployments and production database operations in the current task. A broad request to complete the project does not provide merchant credentials, DRM contracts, device hardware, legal sign-off or a spending budget. Those prerequisites must exist before their checks can honestly pass.

The twelve remaining advanced ledger items also include multi-region data design, native protected TV/offline clients, tenant isolation, HDR formats and creator royalties. Their exact scopes are retained in `BUILD37_REMAINING_12.csv`; adding a placeholder or altering tracker labels does not complete them. Preserve all 282 original task IDs and historical evidence.
