# ReelNest media worker — Build 03

A separate, least-privileged Docker container polls a **PostgreSQL durable queue**. Upload completion atomically inserts a `media_assets` row and a unique `media_processing_jobs` row. No RabbitMQ publication is required for the first version (RabbitMQ is reserved for later event fan-out); the DB row is the durable event.

Worker claims jobs with `FOR UPDATE SKIP LOCKED`, renews a 90-second lease every 20 seconds, and retries transient failures with exponential backoff, max 3 attempts. Exhausted/crashed jobs transition to FAILED; corrupt media is REJECTED without retries. The source bucket and output bucket are private. Each attempt uploads to its **own prefix**, and `media_assets.manifest_key` is populated only after uploading and validating the full set of files.

## Local no-Docker test

```bash
cd apps/video-worker
python3 -m unittest discover -s tests -v
```

Requires the FFmpeg/FFprobe executables, Python 3.12+; `boto3` and `psycopg` aren't needed for this isolated pipeline test.

## Run the complete worker

```bash
docker compose up --build -d
# Watch encoding and retries
docker compose logs -f video-worker
```

Run worker's PostgreSQL/S3 tests only against disposable developer infrastructure. The worker includes periodic recovery of S3-assembled/DB-uncommitted upload intents and expiry cleanup for incomplete multipart uploads. A mid-upload browser refresh/resume UI, S3 lifecycle policies, production DRM, subscription entitlement, and full-stack Docker verification remain later work.

## Safe handling

The worker always downloads source privately to a temporary directory, uses FFprobe to validate video duration (<= 4h), dimensions, and tracks, strips subtitles/data streams and metadata, and builds 360/480/720 HLS renditions **without upscaling**. A small input produces only a matching-quality rendition. FFmpeg has a time budget; Docker has CPU/RAM/PID limits and `no-new-privileges`. Additional defense-in-depth is still required for public untrusted video submissions: container isolation policies, disk quotas, malware screening, rights verification, and infrastructure resource limits.


### Build 16 private seek previews and captions

`pipeline.encode` optionally generates `previews/preview.vtt` and bounded JPEG thumbnails. They are uploaded to the **same private processed bucket** and protected by the same signed-CDN/session policy as the movie. Preview failures do not make valid HLS unavailable. `validate_package` verifies playlist-referenced segment existence and decodes representative segments before READY. The optional 1080p ladder is used only for sufficiently large source video. `timed_text.convert_srt_to_vtt` is a strict offline converter **not yet wired to the creator subtitle upload API**.

### Build 18: multilingual embedded audio (source-only, device tests pending)

When an owned source video contains **two to eight embedded audio streams**, the worker
extracts each into a separate private stereo-AAC HLS rendition (`audio/a00/`, etc.)
and links them through a strict `AUDIO="audio"` HLS group. The original video
variants keep their first muxed audio track for backwards compatibility. A source
with zero or one audio track keeps the old package layout. Untrusted language tags
are normalized to `und`, and all media paths use fixed numeric indices rather than
source-controlled filenames. Media validation rejects unknown playlist directives,
missing segments, and cross-origin audio URIs. `REELNEST_FULL_SEGMENT_DECODE=true`
also decodes every alternate-audio playlist.

The development playback proxy allowlists these audio paths only after session
reauthorization. On supported browsers hls.js exposes audio track selection;
Safari native track selection is feature-detected. **This does not support
separately uploaded dubbed audio, commercial DRM, live CDN validation or device
certification.** Rights to every embedded audio language must be reviewed by the
creator/rights-holder before publishing.
