# 05 — Work allocation, escalation and project continuity rules

This project owner has limited Codex model usage and wants the assistant to do the majority of source work while preserving verified human/Codex changes. **Never launch an hourly automation unless owner explicitly requests it; prior recurring ReelNest build automation was disabled.**

## Division of responsibility

**Main coding assistant (browser ChatGPT, Codex, Gemini/Antigravity with file/editor tools):** audit source; implement backend/frontend/media/AI; add Flyway migrations; write security validations, unit tests, docs and explicit manifests; update cumulative ZIP. If it can actually run local tools, it should run real builds too.

**Codex Astra or other editor-agent (when user allows):** use local terminal for `mvn verify`, `npm ci`/typecheck/build, Docker, DB migration, browser Playwright, integration debugging, Terraform init/plan, Keycloak and native device testing. Preserve time allowance by providing scoped failing commands/logs, not a vague request to remake the whole app.

**Owner/qualified parties:** purchase domain, create/secure AWS and payment accounts, provide test-only credentials via proper secret manager, approve chargeable deployment, negotiate media licences/DRM, supply content metadata, and sign off tax/privacy/consumer/IP/security decisions. No model may make these decisions unilaterally.

## Development strategy

1. Start each turn by discovering newest cumulative project ZIP/branch from the **current chat**. This handoff archive is a deterministic baseline; if newer user/Codex changes exist, **diff and merge**, never silently overwrite.
2. Read `AI_START_HERE.md`, `AGENTS.md`, the docs inside `docs/AI_HANDOFF`, and the current task CSV before modifying source.
3. Pick a meaningful connected batch of tasks. Prefer finishing 119 partials, then 39 no-evidence tasks. Respect dependencies. Do not artificially change status to meet a requested total.
4. For each task, include task ID, source files, acceptance criteria, tests and remaining risks. Add regression tests whenever fixing a bug. Never weaken entitlement, country, age, payment or content-rights checks to make tests pass.
5. Run whatever real tests the environment supports. A static source scan is NOT a typecheck; cryptographic helper tests are NOT full CDN proof; synthetic-video encoding is NOT successful authenticated streaming.
6. Do not run/claim paid AWS deployment, real payments, prod database erasure or CDN worldwide rollout without explicit user authorization and applicable real accounts.
7. Create one cumulative ZIP **on every substantive development reply**. Preserve all prior application source and changes. Add `docs/BUILDxx_IMPLEMENTATION.md`, `docs/BUILDxx_282_TASKS.csv`, `docs/BUILDxx_TEST_EVIDENCE.md`, updated `AI_START_HERE.md`, and a checksum manifest. Link exact created file.
8. For any assistant without file-generation access, do NOT invent a ZIP link. Supply a Git patch or directly edited files and clearly explain limitation; next capable tool must package them.
9. Handoff report must list: new features/files; tasks started/completed/partial; old → new 282-task counts (must sum to 282); test commands/pass/fail/not run; what remains and who owns it; release decision; download link.
10. Never confuse "implemented code" and "fully accepted". Record external-blocked items as **blocked, not complete** until evidence is received.

## Source controls

- The canonical status remains `docs/BUILD23_282_TASKS.csv` for this baseline; later valid builds supersede it with a new tracker but MUST preserve all IDs and categories.
- Application code is organized under `apps/web`, `apps/api`, `apps/video-worker`, `apps/recommendation-api`, `apps/recommendation-lab`; AWS under `infrastructure/aws`.
- Use Git branches/PRs and reviewed commits for independent work. Never commit secrets, credentials, generated customer data, or unsafe public media. Do not overwrite a user's edits from another agent.
- Avoid speculative "file count = completion" metrics.
