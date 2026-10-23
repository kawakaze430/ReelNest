# 04 — Mandatory acceptance/test plan before any worldwide AWS production launch

**Current verdict: NO-GO.** "Source implemented" and offline tests never substitute for actual service integration, payment/provider activity, legal rights or operational sign-off. Maintain explicit PASS/FAIL/BLOCKED outcomes and evidence links in `docs/AI_HANDOFF/TEST_EVIDENCE.md` or CI artifacts.

## Gate G0: clean source/repository build (P0, operator/Codex)
- [ ] Inspect Git baseline for uncommitted work/secrets; establish a private remote, protected PR checks and a reviewed lockfile (Node + Java dependencies).
- [ ] Run `python3 scripts/codex_preflight.py` and `python3 scripts/release_gate.py --mode offline` from repo root.
- [ ] `cd apps/api && mvn -B --no-transfer-progress verify` with network-installed Maven dependencies; fix compile/unit failures and add regressions.
- [ ] `cd apps/web && npm ci && npm run typecheck && npm run lint && npm run build` using an actual reviewed `package-lock.json`. If absent, generate, audit and commit it first.
- [ ] `cd infrastructure/aws && terraform init -backend=false && terraform fmt -check && terraform validate`; never treat HCL lexical checks as provider validation.
- [ ] Run vulnerability/license scans with actionable findings; prove critical issues resolved or explicitly waived by reviewer.
- **Acceptance:** clean reproducible install/build from a fresh checkout; CI produces immutable logs. Until then code is not certified runnable.

## Gate G1: local cross-service and database (P0)
- [ ] `docker compose config`, `docker compose up --build -d`, `docker compose ps`; ensure volumes, health checks, secrets and dev-only ports work.
- [ ] Fresh PostgreSQL migrations **V1–V25** plus upgrade from an older application schema; verify constraints, idempotence and rollback plan.
- [ ] Verify Keycloak issuer/audience, login, MFA for admin, email/recovery, token expiry, profile isolation, logout and revoked-token behavior.
- [ ] Verify creator application→rights-approved title→multipart upload→worker→private HLS/captions/audio/sprites→admin publication→legal viewer playback→resume.
- [ ] Test malicious inputs (unsafe paths, playlist traversal, SRT malformed text), failed jobs, expiration, retries, interrupted uploads and unpublish access revocation.
- [ ] Exercise two accounts, several profiles, children, and a separate creator/admin; negative tests must receive 401/403/404 as appropriate.
- [ ] Verify PWA never caches protected data, and watch-party/EPG endpoints recheck entitlements per viewer.
- **Acceptance:** real browser E2E and server logs show successful authorized path and correctly denied attacks, no manual DB edits.

## Gate G2: money/identity/provider (P0)
- [ ] Configure Razorpay **TEST** credentials via a secret store. Validate subscription creation, successful/failed/pending captures, renewals, cancellation, expiry and grace periods.
- [ ] Deliver genuine signed test webhooks; verify signature, replay/deduplication, out-of-order events, refunds, disputes and reconciliation against provider API.
- [ ] Verify **no premium entitlement** is granted without independently verified captured payment; correct access revocation and refund/consumer policies.
- [ ] Test account export/erasure across Keycloak, PostgreSQL, storage and analytics with correctly retained financial/audit/legal records.
- **Acceptance:** sandbox test evidence with scrubbed IDs, no keys, merchant policy approval.

## Gate G3: AWS staging, secure delivery and domain (P0)
- [ ] Owner approves monthly cost/budget, region(s), purchased domain and staging scope before provisioning paid services.
- [ ] Review Terraform plan, IAM least-privilege, VPC/subnets, SG/firewall, RDS private networking and backups, ECS images, queue/Redis, secrets and logging.
- [ ] Deploy **staging only** with HTTPS, public app/API and private S3 processed/original assets. Production Keycloak/OIDC must have TLS, persistence, SMTP and admin MFA.
- [ ] Deploy CloudFront with OAC and verified signed cookies/URLs. Confirm no anonymous origin access; test expired tokens, revoked sessions, MIME/content paths and cookie policy.
- [ ] Geolocation/rights must be enforced using trusted edge/origin signals, not request body or client-selected country. Test per-title rules and regional bypass cases using multiple verified regions.
- [ ] Domain: registrar or Route 53 purchase/transfer (owner action), app/API/stream DNS, ACM TLS certificates in correct AWS region (CloudFront cert generally us-east-1), renewals and HSTS.
- [ ] Run static preview separately if desired; do not confuse it with production OTT.
- **Acceptance:** actual staging URL and evidence across 2+ devices and authorized region(s); Terraform apply log and reviewed IAM policies.

## Gate G4: security, reliability, operating & legal (P0)
- [ ] Threat model: OAuth, privilege escalation, IDOR/BOLA, content rights, SSRF, file-upload malicious media, playlist injection, webhook forgery and privacy leaks.
- [ ] Perform independent security review/pen test and dependency scan; remediate critical/high findings with regression tests.
- [ ] Load test API plus real concurrent playback/CDN delivery; measure p95/p99 latencies, startup, stalls, QoE and cost. Rebuffering and rights/entitlement errors are release metrics.
- [ ] Execute restore of a disposable PostgreSQL backup; test RPO/RTO, worker failure, queue backlog, notification/reconciliation recovery, deployments and rollback.
- [ ] Add real alert drill, incident response, cost/billing alarms, customer support, copyright complaint and takedown process.
- [ ] Rights-holder agreements for every movie, artwork, translation/audio/subtitle, territory, platform, period and rental/download permission; content classification, privacy/tax/consumer regulation reviewed per launch country.
- [ ] DRM provider and encrypted playback where **specific licences** require it. Signed CDN cookies alone are not DRM.
- **Acceptance:** signed release checklist, documented risk decisions and qualified legal/IP/security review.

## Gate G5: controlled paid pilot then global rollout
- [ ] Launch **one licensed country** to a small canary cohort with genuine content and payments only after gates G0–G4 pass.
- [ ] Measure playback availability, search/index hygiene, retention, payments/chargebacks, support tickets, device compatibility and spend; conduct rollback drill.
- [ ] Expand country by country: rights matrix, consumer tax/refunds, privacy/data residency, sanctioned territories, payment options, subtitles/locale and regional device coverage. A global CDN does not imply global rights.
- [ ] Use CloudFront edge distribution for video; add extra app/database regions only when measured latency, resilience or local data rules warrant. Cross-region data replication needs consistency and recovery design.
- [ ] Add mobile native and TV apps, paid DRM, secure downloads and advanced advertising/FAST only with test devices and licensed rights.
- **Acceptance:** separate country-based GO/NO-GO decision, monitoring/financial health and permitted territorial catalog.

## Tests already run in THIS handoff packaging session

- `python3 -m pytest tests/release/test_build23_contracts.py -q` → **7 passed**.
- `node scripts/check_ts_syntax.mjs` → **83 source files parsed; zero syntax errors**. This is NOT `tsc --noEmit`.
- `python3 -m pytest apps/video-worker/tests apps/recommendation-api/tests apps/recommendation-lab/tests -q` → **59 passed**, including local FFmpeg tests.
- Full Maven, npm production build, Docker, Flyway live PostgreSQL, Razorpay, Terraform provider, CloudFront, security pen test and AWS launch → **NOT RUN here**.

## Exact release status

| Gate | Current status | Owner |
|---|---|---|
| G0 compiled clean project | BLOCKED / not verified | Local + Codex for actual errors; assistant for source patches |
| G1 real end-to-end OTT | BLOCKED / not verified | Local + Codex; assistant for source fixes |
| G2 provider money/identity | BLOCKED / not verified | Merchant owner + Codex + review |
| G3 AWS staging/edge/DNS | BLOCKED / not verified | Owner + Codex/cloud operator |
| G4 security/rights/recovery | BLOCKED / not verified | Assistant + independent reviewers + operators |
| G5 international production | NOT STARTED / NO-GO | Owner, licensed catalog, country-specific approval |
