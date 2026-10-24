# ReelNest Build 37 — visible licence watermark and synthetic live HLS foundation

Date: 24 October 2026. Source: GitHub main. Continues Build36, preserving all original 282 task IDs/status/evidence and 0 production acceptance claims.

## Source advances
- **F-17:** Extended actual signed-in playback session response with `visibleWatermarkRequired` computed from the current approved content-rights basis, not user input. Licensed distributor titles show a rotating-position, pseudonymous session marker overlay in the browser player. This is a **best-effort client-rendered visible watermark**. It is removable in a modified client; casting, PiP and media segments need vendor-supplied forensic watermarking and legal rights policy before professional security signoff. Java `VisibleWatermarkPolicyTest` covers rights basis.
- **Q-03:** Added `apps/video-worker/live_preview.py` as a sandbox-only FFmpeg generator of <=10 seconds of original, synthetic H.264/AAC low-delay HLS segments. `apps/video-worker/tests/test_build36_live_preview.py` runs the real encoder and validates the manifest. No arbitrary input ingest or live distribution servers. It is **not full low-latency HLS partial segments** or OTT live events.
- Build 36 AVOD/default-consent work also received disposable PostgreSQL constraint verification: `scripts/test_avod_schema_live.py` checks default OFF consent, second independent reviewer, non-billable event dedup and cascade deletion.
- Builds36/37 all source edits pushed to `main`. Latest standard CI and migration actions provide actual results. No native APK/iOS builds or commercial vendor acceptance have been run.

## 282 tasks as of Build37
| Bucket | Count |
|---|---:|
| Component CI verified (narrow, not signoff) | 6 |
| Source implemented/runtime unverified | 67 |
| Partially implemented | 149 |
| Execution or operator blocked | 31 |
| External accounts/approvals blocked | 17 |
| **Not started** | **12** |
| **Total** | **282** |

Across Builds36–37, 8 of the original 20 unstarted tasks advanced to **partial**, not production complete. Exactly 12 are still unstarted; see `docs/BUILD37_REMAINING_12.csv`.
No live paid ad, native device playback, vendor DRM, multi-region AWS release, live customer streaming, creator payouts or payment transactions were performed.

G1 creator→licensed full playback E2E, G2 sandbox billing, G3 AWS staging, G4 security/recovery, and G5 commercial rights are still NOT ACCEPTED.
**Worldwide production verdict: NO-GO**.
