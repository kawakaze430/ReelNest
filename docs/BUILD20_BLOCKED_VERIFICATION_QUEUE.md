# ReelNest Build 20 — External/Local/Codex verification queue

These tasks are **not** verified or completed. A runnable environment, third-party account, professional authorization, or independent review is required. Source development proceeds separately.

**Total blocked:** 57 tasks (40 execution / 17 account or approval).

## Local and Codex — runtime and CI evidence

| ID | Task | Required verification |
|---|---|---|
| A-04 | Regenerate and commit reviewed npm lockfile | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-05 | Compile backend with real Maven dependencies | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-06 | Run backend unit and integration tests | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-07 | Typecheck and production-build Next.js | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-08 | Build Docker images for API, frontend, worker and AI service | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-09 | Validate Compose schema, profiles and dependencies | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-10 | Start PostgreSQL, Keycloak, Redis, RabbitMQ, MinIO, API, frontend and ML services | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| A-11 | Apply Flyway V1–V9 on clean database and test an upgrade database | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-01 | Verify registration, login, email confirmation and account recovery | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-03 | Remove seeded/demo accounts from staging and production | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-05 | Enable MFA for administrators and sensitive accounts | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-07 | Enforce per-resource ownership and IDOR protection | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-14 | Test Keycloak outage, restart and identity recovery procedures | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| B-15 | Review admin break-glass procedure and audit records | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| D-01 | Verify end-to-end direct multipart upload to private MinIO/S3 | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| E-01 | Test initial playback in Chrome, Edge, Firefox and Safari | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| E-02 | Test Android Chrome and iOS Safari native HLS behavior | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| E-24 | Verify stream session limits across browsers/devices | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| F-01 | Verify every playback API checks profile ownership | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| F-06 | Prove direct S3 and unauthorized CDN requests return denial | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| G-01 | Compile and run billing service against local database | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| J-01 | Verify every existing route and error state in the real app | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| L-01 | Verify local Docker integration before AWS deployment | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| L-02 | Validate Terraform fmt/init/validate with real providers | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| L-03 | Review Terraform plan, resource costs and IAM policies | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| L-11 | Deploy frontend, API, ML service and worker to staging | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| L-14 | Configure signed-cookies distribution and verified georestrictions | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| M-15 | Create a device/browser/DRM capability matrix and test lab | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| O-03 | Test BOLA/IDOR against user/creator/admin account boundaries | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| O-09 | Run DAST and external dependency/vulnerability review | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| O-12 | Test CDN signed-cookie expiry, geo bypass and authorization revocation | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| O-15 | Add Playwright happy-path and negative browser E2E flows | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| O-16 | Add visual, keyboard, browser matrix and accessibility regressions | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-01 | Run backup/restore in disposable PostgreSQL environment | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-04 | Run read-only and controlled-write load tests in staging | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-05 | Test playback startup time and rebuffer ratios across networks | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-06 | Load test simultaneous sessions and subscription gate | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-08 | Test disk/storage, DB, broker and upstream outage behavior | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-11 | Test blue-green/canary deploy and safe rollback | Execute prepared procedure in a suitable local/staging environment and retain logs. |
| P-14 | Perform chaos/failure injection in staging (not production) | Execute prepared procedure in a suitable local/staging environment and retain logs. |

## Accounts, vendors, rights holders and professional sign-off

| ID | Task | Required action |
|---|---|---|
| B-04 | Configure production identity persistence, SMTP and secure TLS | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| C-15 | Review content rights and classification with qualified professionals | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| F-19 | Define DRM compliance and security commitments with licensors | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| G-02 | Verify Razorpay TEST plan creation and checkout | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| G-15 | Calculate appropriate local taxes, VAT/GST and refund rules with advisor | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| L-15 | Deploy CloudFront global distribution and verify real regional clients | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| L-25 | Perform canary pilot in one licensed territory before worldwide rollout | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| M-14 | Set up app signing, store listings, review and staged rollouts | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-01 | Review privacy policy, terms of use and applicable data laws | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-07 | Perform child data/age verification risk review | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-08 | Review OTT content classifications, grievance redressal and takedown | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-09 | Validate rights contracts for every country and streaming platform | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-10 | Review international taxes, trade/sanctions and payment availability | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-11 | Review accessibility commitments in target countries | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-13 | Run independent IP/trademark clearance for brand name and artwork | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| N-14 | Define creator payout and intellectual-property dispute process | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |
| O-13 | Add pentest and independent security review before paid launch | Obtain the required account, provider service, deployment authorization or professional approval; then verify. |

## Recommended evidence for each external run

- Exact commands, tool versions and non-secret configuration names
- Exit codes, redacted logs and reproducible failure traces
- Identity/rights negative tests, not only happy-path examples
- Authorized staging URLs or artifact links, no API keys or passwords
- Human reviewer and recorded approval for paid deployment or content rights

## Suggested environment checks

```bash
cd reelnest
python3 scripts/codex_preflight.py
mvn -B -f apps/api/pom.xml verify
cd apps/web && npm ci && npm run build && cd ../..
docker compose config
docker compose up --build -d
bash scripts/validate_local_stack.sh --smoke
# Flyway V1–V16 must run successfully on clean and upgrade databases.
```

**Do not run live customer payments or deploy chargeable infrastructure without explicit approval.**
**CDN revocation after rights takedown remains a launch blocker until expiry, cache and key-rotation behaviors are tested.**
