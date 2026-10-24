# ReelNest — CI tests passed vs. work that remains to be tested

**Basis:** Build37 `eefb90bae0c699b3afc915d3f7ae873e1338af42`, 24 October 2026. This file is a companion to [all 282 task rows](REELNEST_MASTER_282_CHECKLIST.md).

## Verified, completed CI jobs
[GitHub Actions Build37](https://github.com/kawakaze430/ReelNest/actions/runs/40077997900) completed with all **7/7 jobs successful**:

| Job | What actually ran | Not established |
|---|---|---|
| API | Java 21 Maven verify, compiled backend and unit tests | Full Keycloak, Redis, RabbitMQ, MinIO→HLS user E2E |
| Web | Node 22 npm ci, audit, TypeScript typecheck, Next.js production build | Real customer paths, browser cross-device login, payments |
| Worker | Python media and ML regression suites, FFmpeg installed | Complete creator→review→protected stream journey |
| Worker image | Real Docker image build, restricted non-root Python and FFmpeg sanity checks | Full Compose stack, long-running distributed queues |
| Terraform | `init -backend=false` and `validate` | AWS plan/apply, deployed regional systems, cost and threat review |
| Release-offline | Source/security/offline tests and packaging contracts | Actual G1–G5 acceptance |
| AWS preview package | Static preview packaging and deployment tests | Actual AWS Amplify deployment or commercial product |

## Other narrow passed tests

- [Disposable Flyway V27 and live schema testing](https://github.com/kawakaze430/ReelNest/actions/runs/40077691106): clean/upgrade migrations in isolated PostgreSQL and API preflight (not a rollback/restore or production DB).
- [Catalog rights and search integration](https://github.com/kawakaze430/ReelNest/actions/runs/40074556828): ephemeral creator/title/media/rights record, search facets, reviewed aliases, immediate revocation and clean-up.
- [Build31 actual MinIO multipart](https://github.com/kawakaze430/ReelNest/actions/runs/40064689818): resumable S3-compatible multipart requests; not OAuth-backed browser creator upload.
- Prior [Codex Cloud configuration checks](https://github.com/kawakaze430/ReelNest): Docker Compose `config` and lockfile; actual services not started by that run.
- Work in Builds 36/37 adds nonbillable preview and synthetic media components; repository CI passing is not professional ad, DRM, device or payment acceptance.

## Which test statuses are meaningful?
- **COMPONENT_CHECK_PASSED:** 6 *explicitly classified* task IDs; acceptance still `NO`.
- **LIMITED_TEST_PASSED:** for another audited task with evidence of a narrower test, e.g. MinIO multipart or FFmpeg synthetic HLS. Still `NO`.
- **SOURCE PRESENT / PARTIAL:** no implication that the whole task was executed.
- **BLOCKED / NOT STARTED:** not tested as a deliverable.
- **Production accepted:** **0 of 282.**

For the exact next integration test/implementation action, account/vendor dependencies, responsible executor and detailed historical source evidence, use [CSV — one row per task](REELNEST_282_TASK_TEST_AND_RELEASE_AUDIT.csv).

## Remaining release gates
- **G0:** Seven green GitHub CI jobs do not imply services ran together.
- **G1:** Creator identity→license→upload→FFmpeg→review→authorized private HLS play+revocation not accepted.
- **G2:** Sandbox full money lifecycle, disputes/renewal/refund and access transitions not accepted.
- **G3:** Terraform validation is not approved AWS plan/apply or running staging; no actual worldwide deployment.
- **G4:** Security, privacy, DRM device E2E, accessibility, backup/recovery still open.
- **G5:** Country-specific film/music rights, payment terms, tax, app stores, DRM and legal signoff still open.

**Do not switch commercial payment, DRM, offline streaming, or geo rights on because G0 source CI passes.**
