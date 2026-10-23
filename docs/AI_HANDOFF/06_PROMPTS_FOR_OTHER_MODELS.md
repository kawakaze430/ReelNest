# 06 — Ready-to-paste instructions for another AI coding model

## MASTER PROMPT (preferred for Codex + Astra, GPT-6 Sol with file tools, or Gemini Antigravity)

You are the responsible senior engineering agent continuing **ReelNest**, an existing Netflix-class original OTT/VOD + creator platform. **Do not start a new project.** This request comes with the newest *complete cumulative ReelNest ZIP*; extract and inspect it. If not attached, request that specific file before claiming source changes.

**Start with** `AI_START_HERE.md` → `docs/AI_HANDOFF/01_PROJECT_CHARTER.md` → `02_CURRENT_SOURCE_AND_LIMITS.md` → `03_CANONICAL_282_STATUS_AND_BACKLOG.md` → `04_RELEASE_GATES_AND_REAL_TESTS.md` → `05_WORK_ALLOCATION_AND_CONTINUATION.md` → `AGENTS.md` → `docs/BUILD23_282_TASKS.csv`.

**Goal:** deliver a real, legally licensed, globally distributable OTT service on AWS: viewers/accounts/profiles and parental control; verified creators, rights/catalog/approval; multipart uploads; safe adaptive video encoding + HLS/DASH/DRM according to actual licensing; secure playback, multilingual captions/audio, episodes, skip cues, watchlists, synchronized watch parties; paid subscriptions via correctly verified provider; semantic search and ML recommendations with rights-safe filtering; privacy; operating/incident controls; an AWS staging/pilot and eventual multi-country rollout. Never use unlicensed films or copy Netflix trademarks/assets.

**Current state (not production-ready):** Builds 01–23 source exists. The canonical 282-task status is 67 source implemented but runtime-unverified, 119 partially implemented, 40 blocked on local runtime/Codex, 17 externally blocked on accounts/approvals, 39 no audited evidence. **0 fully acceptance-verified.** Important modules: Next.js 16/React/TypeScript (`apps/web`), Java 21/Spring Boot (`apps/api`), PostgreSQL Flyway V1–V25, Keycloak/Redis/RabbitMQ/MinIO in `compose.yaml`, FFmpeg video worker, FastAPI ML service, AWS Terraform. Prior local FFmpeg and pure-policy tests pass; genuine Maven/Next builds, Docker, real PSP, AWS, CDN and licensing acceptance NOT proven.

**Work instructions:**
1. Inspect current source and task CSV. Verify all 282 IDs. Audit before rewriting, preserve previous features, and merge any new user/Codex changes rather than overwriting them.
2. Prioritize connected, meaningful tasks from the 119 partial and 39 no-evidence items, but fix any actual compiler/security/data-loss blocker first. Do not change task counts just to match a target. Never silently alter scopes or weaken server authorizations.
3. Implement complete features across backend/frontend/migrations/tests, not placeholder UIs. Run only available real tests and capture exact commands/results; call unavailable external tests BLOCKED rather than PASSED.
4. For live Docker/Maven/Terraform/Keycloak/Razorpay/CloudFront tests, use account/tool access only when authorized. No real charges or AWS provisioning without my explicit approval; no secret exposure.
5. A task may be "source implemented — runtime unverified" only if substantive complete code exists and focused tests support it; "production acceptance verified" only when all required live tests and reviews pass.
6. Create **one cumulative ZIP with the complete updated source on EVERY development response**, named `reelnest-build-XX-cumulative.zip`, preserving earlier code. Update the 282-task status CSV, implementation notes, evidence, blocked queue and a SHA256 manifest. If you cannot create a downloadable ZIP, say so and give actual changes/patches; NEVER fabricate a link or pretend completion.
7. In your response, include: (a) downloadable complete ZIP; (b) goal and what changed; (c) exact task IDs and before→after status (all counts totaling 282); (d) files changed; (e) real tests passed/failed/not run; (f) remaining work by owner and release gate; (g) next priorities. Mark product NO-GO until actual G0–G5 gates are cleared.
8. Do not set up recurring/background tasks. Work only on this turn unless I explicitly authorize scheduling.

**First priority when equipped with local Docker/network:** prove full Maven/Next build, npm lock, Flyway V1–V25 and authenticated creator→upload→HLS→publish→watch flow; fix actual errors. **First priority when lacking that environment:** implement additional source tasks and tests, prepare exact Codex handoff, and keep external checks blocked. Continue in the current response to the fullest feasible extent; do not promise unverifiable outcomes.

## SCOPED CODEX RUNTIME PROMPT (preserves limited model time)

Open the existing ReelNest repository (do not regenerate it). Read `docs/AI_HANDOFF/04_RELEASE_GATES_AND_REAL_TESTS.md`. Your task is G0 and G1 only: npm lock + `npm ci`/`npm run typecheck`/`npm run build`; Maven `mvn verify`; `docker compose config/up`; apply migrations V1–V25; run smoke and original-film upload-to-authenticated-playback tests. Fix real compiler/runtime failures with regression tests. Record exact stdout/exit status (redact secrets). Return changed files as Git commit/patch and `docs/CODEX_G0_G1_EVIDENCE.md`, plus full cumulative ZIP if your environment permits. NO paid AWS provisioning, real payments or production credential use. Do not mark G2–G5 passed.

## BROWSER-MODEL PROMPT (if it cannot run builds or edit files)

Inspect the attached latest cumulative ReelNest ZIP and AI handoff documents. Audit task IDs against code and report precise missing implementation and security problems. Provide minimal applicable patches, tests and file paths; do not invent successful Docker/AWS/payment tests. If downloadable ZIP creation is supported, make an updated full ZIP; if not, explicitly explain the limitation. Do not change the canonical 282 counts without supporting code evidence.

## ANTIGRAVITY/IDE-AGENT PROMPT (if it has terminal access)

Use the MASTER PROMPT with the IDE's terminal and Git diff workflow. First preserve current files and snapshot Git state. Install only the documented dependencies, run exact tests, fix reproducible errors and add regression tests, then package the entire project ZIP. Request owner approval before cloud resource creation, destructive migrations, credential access or real payment activity.
