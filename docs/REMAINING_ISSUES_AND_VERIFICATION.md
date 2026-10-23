# ReelNest — Current Remaining Work and Evidence Gates

**As of 2026-10-22: NOT PRODUCTION READY.** This is a corrected view based on Build 18 cumulative code, not an assertion that all source features work together.

## The 12 external release gates still unverified

- [ ] **maven_verify** — Full Maven compile, tests, Flyway/JPA boot integration. Evidence: not provided / not run in this environment.
- [ ] **nextjs_build** — Production Next.js dependency install, typecheck and build. Evidence: not provided / not run in this environment.
- [ ] **docker_e2e** — Live creator upload, HLS, admin approval, viewer playback. Evidence: not provided / not run in this environment.
- [ ] **razorpay_test** — Razorpay sandbox charge, refund, dispute, duplicate/out-of-order webhook. Evidence: not provided / not run in this environment.
- [ ] **cdn_geo** — CloudFront distribution, cookie and country/territory bypass test. Evidence: not provided / not run in this environment.
- [ ] **terraform_validate** — Real Terraform init/validate/plan with pinned provider and review. Evidence: not provided / not run in this environment.
- [ ] **security_review** — Dependency scan, access control, session & upload threat-model review. Evidence: not provided / not run in this environment.
- [ ] **content_rights** — Owned/licensed films, classification, consent and rights documents. Evidence: not provided / not run in this environment.
- [ ] **privacy_deletion** — Identity-provider+storage+legal retention deletion workflow proven. Evidence: not provided / not run in this environment.
- [ ] **backup_restore** — Staging RDS restore and recovery target test. Evidence: not provided / not run in this environment.
- [ ] **load_test** — Representative paid traffic and video QoE load tests. Evidence: not provided / not run in this environment.
- [ ] **alerts_runbook** — Paging routes and alert exercises with incident ownership. Evidence: not provided / not run in this environment.

## Partially implemented or offline-only tasks explicitly identified in Builds 14–18

- [ ] **D-10** — Create thumbnails, posters, preview strips and sprite/VTT metadata (`PARTIAL_SOURCE`; Build 16 (partial); Build 17 (partial)).
- [ ] **D-11** — Support alternate audio tracks and subtitle ingest (`PARTIAL_SOURCE`; Build 17 (partial); Build 18 (partial)).
- [ ] **D-14** — Record processing progress, ETA, warnings and failure diagnosis (`PARTIAL_SOURCE`; Build 17 (partial)).
- [ ] **D-16** — Validate package decoding, manifest correctness and all segments (`PARTIAL_SOURCE`; Build 16 (partial); Build 17 (partial); Build 18 (partial)).
- [ ] **E-06** — Implement audio track and subtitle/closed-caption selectors (`PARTIAL_SOURCE`; Build 14 (implemented); Build 17 (partial); Build 18 (partial)).
- [ ] **E-10** — Add Next Episode, episode picker and series season navigation (`PARTIAL_SOURCE`; Build 16 (partial)).
- [ ] **H-07** — Create offline chronological evaluation and reproducible datasets (`OFFLINE_ONLY`; Build 18 (offline_only)).
- [ ] **H-08** — Measure Recall@K, NDCG@K, coverage, diversity and fairness (`OFFLINE_ONLY`; Build 18 (offline_only)).
- [ ] **H-15** — Build editorial overrides, safety exclusion and diversity constraints (`OFFLINE_ONLY`; Build 18 (offline_only)).
- [ ] **J-11** — Implement support/contact, FAQ and account settings pages (`PARTIAL_SOURCE`; Build 16 (partial)).
- [ ] **K-07** — Creator processing dashboard, subtitles and poster editing (`PARTIAL_SOURCE`; Build 18 (partial)).

## High-priority operational problems to fix in order

1. Compile actual Spring Boot and Next.js dependency trees; fix discovered compiler/type errors.
2. Run database migrations V1–V14, keycloak, RabbitMQ, MinIO, Redis and worker under Docker.
3. Test the full authorized creator upload → transcode → approval → session → playback and subtitle/alternate audio workflow.
4. Validate paid entitlements with actual Razorpay TEST events, refunds and disputes.
5. Validate Terraform with providers and deploy AWS staging with reviewed IAM, secret handling and identity configuration.
6. Test CloudFront signing, geographic rights, player renewal, Safari behavior and fail-closed authorization.
7. Exercise account deletion/retention, security checks, staging backup restoration and load testing.
8. Obtain licensed media distribution contracts and relevant legal/consumer/security review prior to public paid launch.

## Rules for future updates

- Do not resume the canceled automated hourly task unless the owner explicitly requests it.
- Do not overwrite Git branches with older or separate-instance generated ZIPs.
- Attach actual command exits, CI logs, environment details and reviewer evidence before marking a checklist box complete.
- Use `PRIORITY_117_TASKS.md` for a focused view and `MASTER_IMPLEMENTATION_CHECKLIST.md` for the original full backlog.
