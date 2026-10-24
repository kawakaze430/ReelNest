# ReelNest Build 29 — Resumable creator video uploads

**Date:** 24 Oct 2026. **Source:** GitHub `kawakaze430/ReelNest`, branch `main`.

## Feature delivered in source
A creator uploading an original 5 GiB-or-smaller MP4/MOV/MKV can resume a failed multipart upload, rather than losing already uploaded parts. This advances original backlog **D-02**.

1. Server adds `GET /api/v1/creator/titles/{titleId}/uploads/{id}/parts`, **owner scoped** through existing approved-creator and title ownership checks; only drafts with unexpired, `INITIATED` uploads may request a snapshot. The response is no-store and contains only part numbers, exact lengths and object-store ETags.
2. Server validates remote S3/MinIO part length, ordering, count, ETags and non-truncated listing; S3 remains the source of truth. Upload completion continues to re-fetch S3 metadata and validate **every** part before queuing an asset.
3. Creator Studio detects an unfinished same-name/same-size upload, asks the backend for actual received parts, resumes matching parts, retries newly uploaded chunks up to three times, and preserves progress on transient errors. After reload the creator must reselect the **same original file**. If browser has no matching saved sample fingerprint, it **re-uploads all chunks** to avoid cross-file mixing. No API bearer token is sent to S3 presigned URLs.
4. Creator Studio offers an explicit **Cancel upload** action; it no longer automatically destroys the multipart session on a transient error.
5. Begin-upload guard now ignores already expired `INITIATED` sessions; those no longer indefinitely block a new upload. The existing worker cleanup handles expired orphan sessions.
6. Added Java JUnit tests for invalid/duplicate/truncated part snapshots, an uncached endpoint, and source-contract checks preserving task IDs.

**Security limitation:** The browser's stored SHA-256 covers bounded first/last file samples, **not the entire multi-gigabyte video**. A malicious or changed file that shares sampled bytes could potentially mix unmodified parts. Server checks ETags and sizes, but real full-file checksum attestation before READY remains a future hardening task. Only upload media you own.

## Test truth
Last verified baseline before Build 29: [Build28 six/seven-job GitHub CI success](https://github.com/kawakaze430/ReelNest/actions/runs/40012915448). No new green build is claimed for Build 29.

Build 29 push GitHub Actions runs ended **before any steps executed** (all jobs with zero steps/no log blob); this is an **external CI runner/account issue**, not evidence that source compilation failed. The code contains new Java JUnit regression tests but these have not yet executed in CI. Do not upgrade D-02 to source-complete/accepted until Java Maven and Next.js typecheck run.

## Status summary
All 282 original backlog task IDs preserved. No task classified as production accepted. Build 29 retains Build 28 categories: 5 component verified (prior builds), 67 source implemented (runtime unverified), 122 partially implemented, 32 execution blocked, 17 external approvals blocked, 39 not started. These count backlog classifications, **not percentage of release readiness**.

## Next G1 gate
After GitHub Actions runners become available, rerun `mvn -B verify`, `npm ci && npm run typecheck && npm run build`, and the new API tests. In disposable GitHub/manual cloud CI, run: original upload, interrupt at chunk 2, reopen same draft, resume only uploaded parts, publish and test authorized private HLS. Test wrong-owner/expired upload endpoints and mismatched-file cancellation. Keycloak, MinIO, streaming, payments and AWS staging remain unverified together. No production launch approval.
