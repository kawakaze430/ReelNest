# Operator deployment and launch checklist (Build 08)

## 0. Important

**Do not start Terraform on a production AWS account or accept money based solely on this repository.** It is an unvalidated STAGING scaffold; many resources cost money even with `enable_services=false`. A registered domain, Route53 zone, two ACM certificates (ALB region and us-east-1), OIDC identity provider, prebuilt container images and reviewed CDN keypair are required. Terraform has not been provider-validated here.

## 1. Local fresh install — disposable development data

Prerequisites: Docker Compose v2, Docker daemon, free local ports 3000/8080/8180/9000/9001, internet to retrieve images.

```bash
cd reelnest
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs --tail=120 api web keycloak video-worker
bash scripts/smoke.sh
```

Local dev: `http://localhost:3000`, `http://localhost:8080/actuator/health`, `http://localhost:8180` (Keycloak). Sample video is an **original generated test clip**; seed catalog descriptions have no streamable licensed media. Run `python3 scripts/e2e_creator_live.py`, `e2e_processing_live.py`, `e2e_playback_live.py`, `e2e_billing_discovery_live.py` only after reading each script's setup and role assumptions.

## 2. Required build verification

```bash
(cd apps/api && mvn --batch-mode verify)
(cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build)
python3 -m pytest apps/video-worker/tests apps/recommendation-lab/tests -q
python3 scripts/check_build08.py
(cd infrastructure/aws && terraform init -backend=false && terraform validate)
```

Do **not** assume these pass; none of Maven/Node production build/Terraform validation ran during authoring. Correct failures before deployment.

## 3. AWS staging checklist

1. Choose AWS account/region; set budgets and alerts. Apply organization/security guardrails; deploy resources with non-root IAM and remote encrypted Terraform state + locking. NEVER store CDN private keys or payment secrets in Terraform state.
2. Review every file in `infrastructure/aws` including rates/costs/SQL permissions; create domain, Route53 zone, ACM certificates, separate OIDC IdP with registered app origins.
3. Generate a CloudFront RSA PKCS#8 private key and corresponding public key **outside** this repository; put only public key in Terraform variables; import private PEM to the separately created Secrets Manager entry, never commit it. The `aws_cloudfront_public_key` ID is used as the signing key ID.
4. Prebuild and push immutable API, worker and production web images to ECR. For Next.js `docker build`, pass `--target production` and `--build-arg NEXT_PUBLIC_API_BASE_URL=https://api.example.com`, `NEXT_PUBLIC_AUTHORITY=https://identity.example.com/realms/reelnest`, `NEXT_PUBLIC_OIDC_CLIENT_ID=reelnest-web`, `NEXT_PUBLIC_APP_ORIGIN=https://app.example.com`. Use **your actual domain**.
5. Review `terraform.tfvars.example`, fill actual values in a secure uncommitted `.tfvars`, run `terraform plan -out=reviewed.plan`, peer-review the plan and costs. The default `enable_services=false` still creates supporting infrastructure. Services remain off until all prerequisites are provisioned.
6. Create a least-privileged runtime PostgreSQL role; update ECS task auth to use it rather than the managed master user. Apply Flyway to a clean staging database and verify migration V1–V8. Test recovery from a restored backup.
7. Review S3 presigned upload behavior from real browser origin, CORS, multipart resume and media worker IAM. Process an original short video fully, approve rights and verify CDN requests cannot access S3 without CloudFront.
8. **Implement trusted geography** via a spoof-resistant edge-to-backend channel and country enforcement. Default CDN playback blocks session issuance until reviewed. Setting `PLAYBACK_CDN_GEO_REVIEWED=true` alone is **not** sufficient; it is an operator attestation, not geo implementation.
9. Verify signed CloudFront cookies against an actual distribution/key group, cross-origin credentials, expiration, refresh, session revocation lag, concurrency, iOS/Safari behavior, privacy/analytics and DRM requirements.
10. Use Razorpay TEST with verified provider values for captured charges, refund/dispute payload shapes, idempotency, pending payments, chargeback settlements and charge reconciliation. Disable live checkout until sandbox evidence and customer refund terms pass.
11. Run E2E, k6 load tests, OWASP checks, vulnerability scan, accessibility testing, admin MFA review, content-rights review, and a documented security approval. Then stage rollout to invited testers only.

## 4. Explicit configuration safety

- `PLAYBACK_DEV_PROXY_ENABLED=false` in cloud. This API proxy is ONLY for local developer HLS.
- `PLAYBACK_CDN_BASE_URL=https://stream.yourdomain.com`, custom-key pair ID/public key group, CDN private key in Secrets Manager. Cookies are short-lived, scoped to approved rendition prefix, and are **not DRM**.
- `PLAYBACK_CDN_GEO_REVIEWED=false` until real trusted geo is integrated; `PLAYBACK_TRUSTED_COUNTRY=IN` currently is just a local/operator config constant.
- Razorpay values are empty by default; checkout will fail. Enable only verified TEST merchant credentials first.
- `BILLING_RECONCILIATION_ENABLED=false` by default; enable after TEST merchant reconciliation and scheduling/distributed locking review.
- `DB_PASSWORD`, CloudFront private signing key, OIDC private material and payment secrets must never be checked into Git or placed in browser-accessible variables.
- Do not rely on `PREMIUM` vs `STANDARD` price differences until meaningful features are actually implemented. Existing sample product offers the same catalog to both tiers.

## 5. Migrations/backups

`V8` deliberately clears previously recorded `subscriptions.paid_through` because historic transactions lack verified `entitled_until`. This is fail-closed: **upgrading an existing environment can temporarily remove premium access**. Reconcile historic charges using the provider and a customer-support-approved migration, not an invented database timestamp. Test schema upgrades on a clone/backup and design a separate verified provider history backfill before affecting real customers.

Cloud RDS `deletion_protection=true`, encrypted storage, non-public network, seven-day automated backups and final snapshot are configured. **Restore has not been tested**. Document actual RPO/RTO through disaster-recovery rehearsal before launch.

## 6. Pass criteria

A fully authorized user can: register, create profiles, a verified creator can upload an original licensed film, media worker outputs validated HLS, admin approves publisher+rights, user purchases TEST plan, verified charge activates subscription, CDN permits valid segment requests, playback resumes and saved privacy data is scoped; expired rights, fake payments, refunds, disputes, child restrictions, expired cookies, unauthenticated users and cross-account IDs are denied.

A release is **not** accepted based on static checks or the offline interactive preview alone.
