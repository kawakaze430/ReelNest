# Isolated synthetic live HLS package proof (Build 36)

Run unit and FFmpeg integration tests with
`pytest -q apps/video-worker/tests/test_build36_live_preview.py`.
The test generates six seconds of original synthetic color/noise video and
audio with FFmpeg and verifies short live-style HLS segments, bounded retention
and a manifest without VOD `ENDLIST`. It creates no network listeners or
public video endpoint and cannot take arbitrary RTMP/SRT/external inputs.

**It is NOT a live events feature or LL-HLS with partial segments.**
Still required: authenticated SRT/RTMP ingest, producer rights review, encoder
autoscaling, real LL-HLS/CMAF partials, multi-bitrate playlists, DVR, stream key
rotation, moderation/abuse control, playback entitlements, capacity monitoring,
failover and legal distribution approval.
