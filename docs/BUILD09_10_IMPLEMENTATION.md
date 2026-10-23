# Build 09 + Build 10 — integration and launch gates

## Verified work

- Reused the complete Build 08 repository; no features intentionally reset.
- Added application-start protection for production (`DeploymentPolicy`, `DeploymentGuard`); `APP_DEPLOY_MODE=production` rejects the developer HLS proxy, missing geofencing review, non-HTTPS CDN URLs, malformed territory codes, and custom MinIO endpoints.
- Production Next.js image now validates HTTPS public origin, API and OIDC URLs before static compilation. Use `scripts/build_production_web.sh` rather than relying on unvalidated/default localhost values.
- CloudFront Terraform has a **single-country whitelist** matching `PLAYBACK_TRUSTED_COUNTRY`; it is India by default. CDN session authorization remains disabled without an independently verified operator review (`reviewed_geo_enforcement=false`). This is an edge IP filter, not a trusted assertion of viewer residence or a guarantee against VPNs.
- Terraform now provisions a separate empty runtime DB secret. ECS API and worker use a `reelnest_app` login with no schema-migration privileges; Spring Flyway is disabled for the deployed API. The actual role/secret must be provisioned and tested outside Terraform (SQL script included). The standalone migration task and rollback strategy remain operator responsibilities.
- Privacy admin API no longer allows marking a deletion request `FULFILLED` when deletion is not implemented (only `IN_REVIEW` and `REJECTED` until a real cross-system cleanup flow exists).
- Release gate script `scripts/release_gate.py` runs 12 offline regression checks and records 12 mandatory evidence items. The `--production` mode refuses to pass while those items remain unverified. It is not a substitute for independent provider/live testing.
- GitHub Actions includes the offline release evidence artifact, alongside Maven/Next/Terraform validation jobs that still need to be run online.

## How to reproduce offline tests

```bash
cd reelnest
python3 scripts/release_gate.py --mode offline --output docs/BUILD10_CHECK_RESULTS.json
bash scripts/test_deployment_policy_java.sh
```

The outcome **does not** mean the full application compiled, migrations ran against PostgreSQL, Docker started, or AWS resources were deployed. `docker`, `mvn`, and `terraform` were unavailable in this environment and npm dependencies could not be downloaded.

## Release checklist and evidence

Run full CI against a connected environment. To inspect the gated release state:

```bash
python3 scripts/release_gate.py --mode production --output docs/build10-production-report.local.json
```

Exit 3 means mandatory external launch evidence is missing. If an operator later provides a `docs/release-attestation.local.json`, the gate displays **ATTESTED** entries, not verified facts; review the actual evidence reports. Keep this file private.

## Migration and runtime database split

1. Use a dedicated elevated migrator to run and test Flyway migrations (V1–V8) before ECS service rollout.
2. Set a fresh random password using `scripts/sql/provision_runtime_role.sql` via a secure one-time invocation on RDS. Verify application permissions independently.
3. Populate Secrets Manager `${project}-${environment}-runtime-db-password` with the exact password, avoiding Terraform state.
4. Deploy API/worker with `DB_USER=reelnest_app`; verify RDS TLS, least privilege and connectivity. App has `SPRING_FLYWAY_ENABLED=false`.
5. For future schema changes, run controlled privileged migrations before rolling out a compatible API.

Caution: the RDS managed master password is **not** used by the ECS app anymore. Staging cannot boot successfully until the runtime role and secret are installed. Existing local Docker Compose continues to use its local development login and Flyway.

## Critical remaining risks

- Full Java/Next/Docker compilation and runtime integration still unverified here. All compile/type/schema errors discovered on a connected runner need fixes.
- Terraform AWS provider validation, actual plan/apply, costs/permissions, private TLS network and CloudFront signed-cookie/geo tests are still required. Terraform remains a staging scaffold, not a deployable one-click production platform.
- CloudFront one-country geofence must be tested against expected viewer origin and bypass attempts; `PLAYBACK_CDN_GEO_REVIEWED` is an operator attestation, not geolocation proof. Multi-territory licensing needs proper per-viewer checks.
- Razorpay sandbox and real provider webhook schemas, refund/dispute lifecycles, proration, cancellations and idempotency must be tested. Live money remains blocked.
- Cross-system account deletion is **not implemented**; data export currently covers the application database subset, not all IdP, media, logs and billing records.
- Production OIDC identity provider, admin MFA, age verification/parental controls, studio-content DRM where needed, live legal rights, content classification and consumer compliance are still external work.
- No production uptime, load-capacity, backup-restore or incident-response proof exists.

See `docs/REMAINING_WORK_DETAILED.md` for prioritized launch blockers and completion criteria.

The AWS .tf files were converted to newline-separated HCL blocks; `scripts/check_hcl_structure.py` only checks balanced tokens and disallows semicolon separators. **Terraform provider validation has NOT run.**
