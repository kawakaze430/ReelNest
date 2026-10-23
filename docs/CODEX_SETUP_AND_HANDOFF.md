# ReelNest — Codex setup and scarce-usage handoff

## Why this exists
Codex should be reserved for checks requiring the user's real environment. Static design, source auditing and standalone offline tests can be done in ChatGPT. A successful static scan does NOT prove the Java/Next.js backend runs.

## 1. Recommended Windows setup (WSL2)
1. Install [Git](https://git-scm.com/downloads), [VS Code](https://code.visualstudio.com/), [Docker Desktop](https://www.docker.com/products/docker-desktop/) and Ubuntu via WSL2. In elevated PowerShell: `wsl --install -d Ubuntu`; reboot if asked.
2. Open Docker Desktop > Settings > Resources > WSL Integration; enable Ubuntu. Start Docker Desktop. Docker Compose should work from the Ubuntu shell.
3. In an Ubuntu terminal, verify `docker version` and `docker compose version`. If it cannot access the daemon, fix Docker/WSL integration first; do NOT waste a Codex task on basic desktop installation.
4. Prefer storing the project under Ubuntu `~/projects/reelnest`, not `/mnt/c/...`, for better performance and file permissions.
5. Install Java 21 and Maven in Ubuntu (e.g. `sudo apt update && sudo apt install -y openjdk-21-jdk maven git python3 python3-venv`). For Node.js install an actively supported Node version (22+ per project), preferably using the official nvm tool; confirm `node -v`, `npm -v`.
6. Install Codex CLI after npm is working: `npm install -g @openai/codex`; start it in your repo with `codex`, sign in using your **ChatGPT account** (not an API key). If you want Astra through the CLI, use version >=0.153.0 and choose Astra in the model picker if available on your account.
7. In VS Code install the official Codex extension, or run the CLI from the Ubuntu terminal. Open the project from WSL with `code .` when configured.

For users not using WSL: run Codex on Windows with the native terminal; some project validation scripts are Bash and Dockerized Linux, so WSL2 is the simpler reproducible route.

## 2. Import this project and initialize Git
Extract the cumulative ZIP. Copy the **inner `reelnest/`** folder into `~/projects/reelnest`.

```bash
cd ~/projects/reelnest
python3 scripts/codex_preflight.py
# Read warnings before continuing
git init
git add .
git commit -m 'ReelNest Build 12 with Codex handoff'
git switch -c fix/runtime-integration
```

If Git requests your author name/email, configure it locally. Check `git status` and `git ls-files` to ensure .env, production keys, build evidence and backups are NOT tracked. Push to a **private** GitHub repository only after reviewing the file inventory. Do not paste credentials into an AI chat or commit them into Git.

## 3. Use Codex deliberately
In Codex, read `AGENTS.md`, `docs/RELEASE12_PRODUCTION_READINESS.md`, and `docs/CODEX_SETUP_AND_HANDOFF.md`. Use the smallest model/effort that solves the task. Save Astra for nontrivial cross-service or opaque compilation failures. Avoid fast modes unless needed. Check `/status` or Settings > Usage before and after a substantial task.

### The first (high-value) Codex task — only real build/integration blockers
Paste the following as a **single scoped task**:

> Work in this existing ReelNest repository. Read AGENTS.md first. Do not rewrite the application. First run `python3 scripts/codex_preflight.py` and `python3 scripts/release_gate.py --mode offline`. Then perform actual `cd apps/api && mvn -B verify` and `cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build`. Fix the FIRST reproducible compilation error using a minimal patch with regression tests; continue until both builds pass or an external blocker remains. Then run `docker compose config -q` and `bash scripts/validate_local_stack.sh --smoke`. Do not reset any persistent data, contact paid providers, or deploy cloud resources. Write exact commands, exit codes, errors and verified results to `build-evidence/CODEX_SESSION.md`, and give a concise list of changed files and any blockers. Do not claim local integration passed unless the stack actually runs.

On failures, ask Codex to fix the *root cause*, not to edit the expected tests. Do not spend your limited allowance asking it for a list of features that are already documented.

### The second task — after successful runtime builds

> Continue on the same branch. Using a disposable development database and only an original video sample, run `bash scripts/validate_local_stack.sh --full`. Debug creator upload, MinIO multipart processing, FFmpeg HLS output, rights approval, authenticated playback, watch progress and privacy boundaries in that order. Do not bypass auth/rights checks. Commit focused fixes with E2E assertions, and capture failing and passing logs in build-evidence/CODEX_SESSION.md. Stop if credentials or real content permissions are needed.

### Third task — only after you provide a Razorpay TEST account

> Using Razorpay TEST credentials via local secrets only, validate captured/failed/pending payments, renewals, cancellations, refunds, dispute states, duplicate/out-of-order webhooks and server-side premium entitlement. Do not use live keys or charge real users. If test provider paths cannot reproduce a scenario, record it as NOT TESTED and add a mock regression test instead. Supply exact evidence and changed files.

### Fourth task — AWS staging (requires explicit approval)

> Run `terraform fmt -check -recursive`, `terraform init -backend=false` and `terraform validate` in `infrastructure/aws`, with current provider modules available. Review `terraform plan` against a *staging-only* AWS account/role and identify billable resources, secret-handling gaps, geo rights, CDN cookies, Keycloak identity runtime and database access. Make safe config fixes. Never run `terraform apply`, register domains, create chargeable AWS resources, or change DNS without the owner's explicit approval.

## 4. What ChatGPT can do between your Codex sessions
- Analyze the Codex error report, Java/TypeScript trace or failed migration and prepare targeted patches for the next cumulative release.
- Improve test plans, API contracts, security rules, rights/entitlement edge cases, frontend UX and docs without interacting with paid services.
- Add offline Python tests and review source changes against the release gates.
- Keep project state/remaining work synchronized so Codex does not repeat resolved tasks.

Send back **only non-sensitive** Codex output: relevant stack trace, command exit codes, names of modified files, test summary and a sanitized project ZIP or Git diff. Redact passwords, personal data, tokens, session cookies, payment identifiers and provider secrets.

## 5. What cannot be declared complete without you
- Razorpay TEST events: requires valid sandbox merchant setup and webhooks.
- AWS staging: requires authorized IAM role, account/budget, domain and explicit deployment approval.
- Film licensing, consumer/privacy legal advice and content classification: require real rights holders/qualified humans.
- DRM: requires appropriate provider/licence/SDK configuration and target devices.
- Mobile/TV native apps: require SDKs/emulators and eventual signing/store accounts.

## 6. Evidence handback format

```text
Codex task: Build/Integration, original Build 12
Repo commit/branch: ...
Commands executed:
- mvn -B verify: PASS/FAIL (+ final failure lines)
- npm run typecheck && npm run build: PASS/FAIL
- docker compose config -q: PASS/FAIL
- validate_local_stack.sh --smoke: PASS/FAIL/NOT RUN
Files changed: ...
Bugs fixed with regression tests: ...
External blockers: ...
Any secrets removed from report: YES
Next smallest reproducible problem: ...
```
