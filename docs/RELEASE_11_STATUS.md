# ReelNest Consolidated Deployment Package — Build 11

**Decision:** STATIC PREVIEW upload-ready; FULL OTT application remains NO-GO pending independently verifiable runtime/production evidence.

## What this consolidation did

- Started from Build 10 cumulative archive, which already contains Builds 01–10. No earlier project phase has been intentionally removed.
- Added a ready-to-upload `deploy/reelnest-amplify-preview.zip` containing an allowlisted, independent browser storefront, original sample MP4, a 404 page and robots.txt. It excludes the application source, test identities, media origins, billing variables and private credentials.
- Added `scripts/prepare_public_preview.py` (reproducible static export) and `scripts/deployment_preflight.py` (non-destructive local readiness report).
- Added deployment tests and `docs/AWS_DOMAIN_AND_DEPLOYMENT.md`, covering domain registration, DNS, certificates, Amplify and guarded full AWS staging.
- No real cloud resource was created; no AWS account, domain, payment merchant credentials or content licences were available.

## What still prevents a FULL OTT public launch

1. No actual `mvn verify`, Next.js dependency typecheck/production compile, Docker E2E or Terraform-provider validation ran in this authoring environment (Docker, Maven and Terraform unavailable).
2. PostgreSQL schema upgrades V1–V8 and whole browser/auth/MinIO/worker/rights/playback flow have not run end-to-end.
3. Real HTTPS identity provider not deployed. Local Keycloak development passwords are not for public use.
4. Razorpay TEST merchant captures, cancellations, refunds, disputes, renewals and webhooks not externally verified.
5. Terraform AWS staging not applied or security/cost-reviewed; ECS runtime DB role/password provisioning and application images remain operator tasks.
6. CloudFront geofencing needs actual trusted viewer-location enforcement and tests; signed cookies are not DRM.
7. Full cross-system account erasure, backup restoration, monitoring alerts, vulnerability/load/accessibility tests and rights/legal review remain pending.
8. Offline recommender models not connected to production and advanced OTT features are optional, not a reason to bypass P0 release gates.

**Safe next action**: Publish the demo through Amplify manual static deploy; separately use a Docker capable developer machine/GitHub CI to resolve the full-service blockers before inviting paying users. This release package is not a claim of production certification.
