"""Build36 isolated low-latency HLS package generation from synthetic inputs ONLY.

No public ingest URL, arbitrary file/network source, upload authentication,
commercial live-event transport, DRM, recording or billing is provided.
"""
from __future__ import annotations
import os
from pathlib import Path
import subprocess

def generate_synthetic_live_preview(destination: Path, duration_seconds: int=6) -> Path:
    """Create at most 10 seconds of video in a NEW local, non-symlink directory."""
    if not isinstance(duration_seconds,int) or not 3<=duration_seconds<=10:
        raise ValueError("Synthetic preview must be between 3 and 10 seconds")
    target=Path(destination)
    if target.exists() or target.is_symlink():
        raise ValueError("Refusing existing or symbolic-link output directory")
    target.mkdir(parents=True,exist_ok=False)
    playlist=target/"demo-live.m3u8"
    cmd=[
      "ffmpeg","-hide_banner","-nostdin","-loglevel","error",
      "-f","lavfi","-i","testsrc2=size=640x360:rate=24",
      "-f","lavfi","-i","sine=frequency=440:sample_rate=44100",
      "-t",str(duration_seconds),
      "-map","0:v:0","-map","1:a:0",
      "-c:v","libx264","-preset","ultrafast","-tune","zerolatency",
      "-pix_fmt","yuv420p","-g","48","-keyint_min","48","-sc_threshold","0",
      "-c:a","aac","-b:a","96k",
      "-f","hls","-hls_time","2","-hls_list_size","3",
      "-hls_flags","delete_segments+independent_segments+program_date_time+omit_endlist",
      "-hls_segment_filename",str(target/"part-%04d.ts"),
      str(playlist)
    ]
    try:
        subprocess.run(cmd,check=True,timeout=30,stdin=subprocess.DEVNULL,
                       stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    except (subprocess.SubprocessError,OSError) as exc:
        raise RuntimeError("Synthetic FFmpeg packaging failed; no live service was started") from exc
    text=playlist.read_text(encoding="utf-8")
    if ("#EXTM3U" not in text or "#EXT-X-TARGETDURATION" not in text or
        "#EXT-X-ENDLIST" in text or not list(target.glob("part-*.ts"))):
        raise RuntimeError("Generated synthetic live HLS did not pass manifest guard")
    return playlist
