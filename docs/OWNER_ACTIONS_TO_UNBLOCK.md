# Actions that unblock the remaining release work

GitHub `kawakaze430/ReelNest`, branch `main`, is canonical. Continue without new branches, worktrees or PRs; reconcile newer commits and push normally. Do not force-push or treat a passing component check as complete commercial acceptance.

## Current Git write-access blocker: 26 October 2026

The current environment can fetch private `main` and read GitHub API metadata through platform-injected authentication, but normal `git push origin main` now receives **HTTP 401** before credential prompting. This is an authentication failure, not an observed branch-protection rejection. The last verified remote SHA is `d4ae145a5b3c79104e623957ee2856ce2ab87265`. Reviewed CI diagnostic repairs are committed locally at `aa886918a07f12d50acb9a22342f23bd111b0e93`; they are **not pushed**, so their remote integration result is NOT RUN.

Restore the environment's repository write authentication through its GitHub/repository connection settings. Do not paste a token into chat or replace platform-held credentials. After the connection is restored, fetch `main`, reconcile any newer commits safely and push normally; then inspect the resulting CI annotations. An exact patch is retained under ignored `build-evidence/reelnest-main-verification.patch` for integration if this cloud checkout cannot push.

## Current action: save the additional download destinations

The first settings update succeeded: runtime revision 2 allows `cgr.dev` and `api.github.com`. GitHub API access works, and exact-commit Android, PostgreSQL and component job results have been verified. The registry index works too. Actual downloads redirect to additional storage hosts, which remain denied. The prepared follow-up draft retains the two working rules and package-manager presets and adds the observed download destinations. Agent tools can save this draft but cannot apply or publish it.

Open this ReelNest cloud environment's settings, review its prepared configuration and save these hostname rules:

| Hostname | Operation/status |
| --- | --- |
| `cgr.dev` | Working registry manifests/authentication; retain this rule |
| `api.github.com` | Working GitHub run/job metadata; retain this rule |
| `9236a389bd48b984df91adc1bc924620.r2.cloudflarestorage.com` | Actual MinIO server/client image blobs; registry redirects were observed for both images |
| `results-receiver.actions.githubusercontent.com` | Download the failing integration job's logs to diagnose its actual error |
| `*.blob.core.windows.net` | GitHub Actions artifact archives use rotating Azure storage accounts; accounts 11, 13 and 16 were observed |

Do not paste tokens, passwords, signed download links or secret files into chat. Git fetch and normal GitHub API reads work; Git push has the separate HTTP 401 blocker above. Additional credentials cannot fix destination-policy denials.

User-facing configuration save can request a runtime update. Publication is separate: it creates an active reusable environment version/snapshot. Neither an agent draft save nor a general instruction to continue applies network rules. After saving, continue this task so the agent can inspect the observed configuration and retry the affected operations. Publish the prepared environment through the product when it is ready for reuse; do not confuse publication with a verified integration test.

## Then finish the actual full media gate

The pushed workflows already automate G0 and disposable G1 for relevant changes. Once API access works, inspect the existing run for the current `main` SHA before starting another. A browser can also open [repository Actions](https://github.com/kawakaze430/ReelNest/actions). An unobserved job is not a passed or failed job.

G1 must prove original upload, captions, worker encoding, rights review, publication, authorized private media, actual decoded browser frames/time, two-account isolation, revocation, traversal rejection and logical restore. All these steps must use a unique disposable project and synthetic accounts/media. The [testing prompt](CODEX_NEXT_TESTING_PROMPT.md) contains the commands and acceptance criteria.

No replacement storage implementation, proxy bypass, manual production-data change or skipped assertion can substitute for this test.

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
