"""Private VOD artwork generation from an already-validated local video.

Never use arbitrary URLs, file names, or user-supplied FFmpeg expressions. All
artwork stays in the processed-media prefix and inherits playback entitlement.
"""
from __future__ import annotations

import math
from pathlib import Path
import re
import subprocess


class InvalidArtwork(ValueError):
    pass


def _stamp(value: float) -> str:
    milliseconds = max(0, round(value * 1000))
    hours, remainder = divmod(milliseconds, 3_600_000)
    minutes, remainder = divmod(remainder, 60_000)
    seconds, millis = divmod(remainder, 1000)
    return f"{hours:02}:{minutes:02}:{seconds:02}.{millis:03}"


def create_poster(source: Path, output_dir: Path, duration: float, *, timeout: int = 45) -> Path:
    """Produce a bounded, private, landscape poster for the playback detail UI."""
    if not math.isfinite(duration) or duration <= 0:
        raise InvalidArtwork("Invalid duration")
    folder = output_dir / "artwork"
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / "poster.jpg"
    timestamp = min(10.0, max(0.0, duration / 3))
    try:
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
            "-protocol_whitelist", "file,pipe,crypto,data", "-ss", f"{timestamp:.3f}",
            "-i", str(source), "-map", "0:v:0", "-an", "-sn", "-dn",
            "-vf", "scale=w='min(1280,iw)':h=-2:flags=lanczos",
            "-frames:v", "1", "-threads", "1", "-q:v", "5", str(target)
        ], check=True, capture_output=True, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as exc:
        raise RuntimeError("Poster extraction failed") from exc
    if not target.is_file() or not 100 < target.stat().st_size < 5_000_000:
        raise RuntimeError("Poster missing or outside size limit")
    return target


def create_sprite_atlas(previews: Path, duration: float, *, interval: int, timeout: int = 90) -> int:
    """Tile existing private seek frames into 10x10 atlases and a #xywh WebVTT.

    Individual thumbnails remain available for older clients. Sprite images are
    optional bandwidth optimizations, not public thumbnails.
    """
    if not math.isfinite(duration) or duration <= 0 or interval < 1:
        raise InvalidArtwork("Invalid sprite metadata")
    frames = sorted(previews.glob("thumb_*.jpg"))
    if not frames or len(frames) > 120 or any(not re.fullmatch(r"thumb_\d{4}\.jpg", p.name) for p in frames):
        raise InvalidArtwork("Unexpected preview frames")
    for index, frame in enumerate(frames, 1):
        if frame.name != f"thumb_{index:04d}.jpg":
            raise InvalidArtwork("Noncontiguous preview frame names")
    # Use a numeric sequence instead of glob expansion to prevent arbitrary files.
    try:
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
            "-framerate", "1", "-start_number", "1", "-i", str(previews / "thumb_%04d.jpg"),
            "-vf", "tile=10x10:padding=0:margin=0", "-frames:v", "2",
            "-q:v", "6", "-threads", "1", str(previews / "sprite_%02d.jpg")
        ], check=True, capture_output=True, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as exc:
        raise RuntimeError("Sprite generation failed") from exc
    sprites = sorted(previews.glob("sprite_*.jpg"))
    expected = math.ceil(len(frames) / 100)
    if len(sprites) != expected or any(p.stat().st_size < 100 or p.stat().st_size > 8_000_000 for p in sprites):
        raise RuntimeError("Invalid sprite atlas output")
    lines = ["WEBVTT", ""]
    for idx, _ in enumerate(frames):
        start = idx * interval
        end = min(duration, (idx + 1) * interval)
        if end <= start:
            break
        atlas = idx // 100 + 1
        local = idx % 100
        x, y = (local % 10) * 320, (local // 10) * 180
        lines.extend([f"{_stamp(start)} --> {_stamp(end)}",
                      f"sprite_{atlas:02d}.jpg#xywh={x},{y},320,180", ""])
    (previews / "sprites.vtt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return expected
