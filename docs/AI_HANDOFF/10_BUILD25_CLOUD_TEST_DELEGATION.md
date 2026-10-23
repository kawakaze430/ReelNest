# Build 25 — Minimal Codex Cloud delegation, ChatGPT ownership

This is an execution and evidence handoff, **not** a request for Codex to own ReelNest development or add features. The ChatGPT/browser engineer retains source development, backlog maintenance, complete ZIP delivery, and integration of returned fixes. Codex Cloud is used only for environment-dependent tests and unavoidable small unblock patches.

## Codex Cloud's bounded work

1. Extract or check out the **whole Build 25 monorepo**, read `AGENTS.md` and `AI_START_HERE.md`. If a GitHub repository only contains the ZIP, extract into the checkout so `reelnest/apps/api/pom.xml` and `reelnest/apps/web/package.json` are present, and work from the `reelnest/` root. Do **not** assume Codex can see a ZIP attached to a different ChatGPT conversation.
2. Execute exactly `python3 scripts/cloud_evidence.py --phase g0`. It runs actual Java/Maven tests; creates an npm lockfile if absent; runs npm ci, typecheck, Next.js build; and validates Docker Compose YAML if Docker exists. No network setup advice is a substitute for execution.
3. Fix only the **first concrete compile/runtime blocker** when its cause is clear and repair is small. Add a targeted regression if modifying production code. Rerun the failed command, not every test suite. Do not tackle old partial tasks, unrelated refactors, stylistic cleanup or business feature backlog.
4. If G0 passes **and** the environment has access to isolated Docker, run `python3 scripts/cloud_evidence.py --phase g1 --disposable-confirmed` only with an empty, expendable development stack and no real customer data. This reuses the existing smoke, Keycloak, all-25-Flyway, original-content FFmpeg upload-to-HLS and player E2E validator. The script intentionally does not tear down volumes.
5. Optionally run `python3 scripts/cloud_evidence.py --phase tf` if Terraform is present; it only does `fmt`, `init -backend=false`, `validate`. **No** provider `plan` against AWS accounts, no `apply`, no DNS, no billing, no real Razorpay interactions.
6. Return machine-readable evidence files and the exact PASS/FAIL/BLOCKED results, one concise root-cause explanation for failures, patch/branch or changed files, and a handoff for browser ChatGPT. Do not self-declare a release gate complete unless the actual test passed. If there are blockers, stop and hand back.

Codex Cloud may not have a Docker daemon or outbound dependency network; those are genuine **BLOCKED — Environment** results. In such a case, use GitHub Actions (existing workflows) or a disposable AWS EC2 build host only **with separate explicit spend authorization**. Do not demand the user's Windows laptop.

## Ownership matrix

| Scope | Owner | Condition |
|---|---|---|
| Feature code, implementation, static/portable tests, design, 282-task audit, every cumulative ZIP | ChatGPT in browser | By user request, current-turn only |
| G0 real Maven/Next.js build, npm lockfile, G1 disposable Docker/Flyway/Keycloak/HLS/Playwright, Terraform init/validate | Codex Cloud | Only if available; narrow one-shot handoff |
| Overflow hard IDE/browser/device E2E or a compile diagnostic when Codex Cloud blocked | Gemini Antigravity | Specific same failing command/branch only; avoid duplicate ownership |
| Recommendation ML experiments / model train/eval / embeddings | Google Colab | Optional later; 200 units are limited; no AWS secrets in notebooks |
| Validated staging infrastructure, RDS, ECS, media storage/CDN, HTTPS, backup restore, pilot load testing | AWS | Explicit user approval, billing monitoring, once local G0/G1 pass |
| Extra testing credits, optional secondary cloud | Azure | Only if AWS cannot satisfy approved test requirement |
| Source archives, test assets and artifact history | Google Drive | Not a substitute for Git commits; avoid storing unencrypted secrets |
| OS/device-specific checks impossible in cloud | Local Codex / Windows | Last resort, no unnecessary local Docker installs |
| Legal rights, payment sandbox registration, production approvals, domain, spend | User | Human account-holder decision required |

## Evidence handling

- Raw logs stay in ignored `build-evidence/` and must be checked for secrets before sharing. Commit only reviewed source diffs and sanitized evidence summaries.
- No GitHub repository should contain only `reelnest-build-25-cumulative.zip`: commit **extracted source files** so Codex sees code. GitHub's web UI does not automatically extract an uploaded ZIP.
- Production launch remains `NO-GO` until gates G0–G5 receive real signed evidence.
