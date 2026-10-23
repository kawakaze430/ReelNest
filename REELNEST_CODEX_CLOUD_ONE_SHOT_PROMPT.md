# Copy and paste into Codex Cloud — ReelNest narrowly scoped verification

You are the **execution/test assistant**, NOT the lead developer for ReelNest. ChatGPT in the browser owns the rest of the project, all new features, source development and cumulative archives. **Conserve Codex usage. Do not browse, redesign, refactor, implement the backlog or deploy anything.**

Read `AGENTS.md`, `AI_START_HERE.md`, and `docs/AI_HANDOFF/10_BUILD25_CLOUD_TEST_DELEGATION.md`. Work on the extracted **Build 25** repository, not an empty repo or a ZIP sitting unextracted in GitHub.

**Only perform these actions, in order:**

1. Run `python3 scripts/cloud_evidence.py --phase g0`. This checks actual Maven/JUnit compilation; creates and installs a frontend npm lockfile if required; runs `npm ci`, `npm run typecheck`, `npm run build`; and checks Docker Compose configuration. Install project build dependencies only when network access allows. Do not spend time installing system-wide Docker/Maven or changing network security settings; if unavailable, mark BLOCKED.
2. For a concrete code error, make **at most one small targeted fix per failing component**, plus an essential regression test; rerun only the failed command. If blocked after that, stop and report the error. Do not change application architecture or other backlog items. Preserve all Flyway V1–V25 migrations and all 282 task IDs.
3. **Only if G0 is successful and a truly disposable private Docker daemon is available**, run `python3 scripts/cloud_evidence.py --phase g1 --disposable-confirmed`. This covers Keycloak, PostgreSQL V1–V25 migrations, FFmpeg ingestion, creator upload, private HLS playback and negative authorization smoke. Never use shared/production Docker volumes. If unavailable, record BLOCKED and stop; do NOT try to set up the user's Windows laptop.
4. If Terraform is already present, optionally run `python3 scripts/cloud_evidence.py --phase tf`. This is backend-disabled validation only; **never plan/apply AWS resources**.
5. Do not use AWS, Azure, Google Colab, Drive or Razorpay accounts. No real payments, refunds, DNS changes, production migrations or billable cloud resources. Do not print/commit any secrets, tokens or customer data. Do not run `docker compose down -v`.

**Return only:** (a) actual PASS/FAIL/BLOCKED/NOT RUN commands, exit codes and summary of `build-evidence/cloud-*-latest.json`, (b) root cause + exact blocker for failed steps, (c) minimal changed files/patch or GitHub PR, (d) whether all 25 migrations and the full playback path genuinely passed. Commit reviewed frontend `package-lock.json` if generated. Preserve the 282-task tracker without promoting statuses solely from source checks. If artifact export is supported without extra work, provide a complete cumulative ZIP; otherwise hand back the branch and exact changes to ChatGPT to package. **Then stop.**
