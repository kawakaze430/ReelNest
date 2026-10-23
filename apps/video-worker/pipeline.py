"""Untrusted input -> bounded probe/transcode -> local validated HLS tree.

Pure standard-library orchestration; does not need S3/Postgres for unit tests.
Transcoded renditions are generated separately to align each variant's segments.
"""
from __future__ import annotations
import json
import math
import shutil
import os
from pathlib import Path
import re
import subprocess
from dataclasses import dataclass
from preview_artwork import create_poster, create_sprite_atlas
from caption_packaging import validate_captions
from audio_packaging import package_audio, validate_audio


class InvalidMedia(ValueError):
    """Permanent, non-retriable invalid/corrupt input."""


@dataclass(frozen=True)
class MediaInfo:
    width: int
    height: int
    duration: float
    has_audio: bool
    video_codec: str


# Conservative VOD preset, no upscale. Target rates in bits/s.
LADDER = ((360, 640, 650_000, 96_000), (480, 854, 1_100_000, 128_000),
          (720, 1280, 2_500_000, 128_000), (1080, 1920, 5_000_000, 192_000))


def probe(path: Path, *, max_duration: int = 14_400, timeout: int = 30) -> MediaInfo:
    try:
        result = subprocess.run([
            "ffprobe", "-v", "error", "-protocol_whitelist", "file,pipe,crypto,data", "-show_entries",
            "format=duration,format_name:stream=index,codec_type,codec_name,width,height",
            "-of", "json", str(path)
        ], check=True, capture_output=True, text=True, timeout=timeout)
        data = json.loads(result.stdout)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, json.JSONDecodeError,
            OSError, KeyError) as exc:
        raise InvalidMedia("Input cannot be inspected with ffprobe") from exc

    container = data.get("format", {}).get("format_name", "")
    # Do not let crafted playlists or network-fetching containers masquerade as uploads.
    allowed_containers = ("mov", "matroska", "mpegts")
    if not any(container.startswith(prefix) for prefix in allowed_containers):
        raise InvalidMedia("Unsupported media container or playlist")
    streams = data.get("streams", [])
    video = next((s for s in streams if s.get("codec_type") == "video"), None)
    if not video:
        raise InvalidMedia("A video track is required")
    try:
        duration = float(data["format"]["duration"])
        width = int(video["width"])
        height = int(video["height"])
    except (ValueError, TypeError, KeyError, OverflowError) as exc:
        raise InvalidMedia("Invalid duration or dimensions") from exc
    if not math.isfinite(duration) or not 0.5 <= duration <= max_duration:
        raise InvalidMedia("Video duration exceeds policy limits")
    if width < 160 or height < 144 or width > 3840 or height > 2160:
        raise InvalidMedia("Video dimensions exceed policy limits")
    if len(streams) > 24:
        raise InvalidMedia("Too many tracks")
    return MediaInfo(width, height, duration,
                     any(s.get("codec_type") == "audio" for s in streams),
                     video.get("codec_name", "unknown"))


def _renditions(info: MediaInfo) -> list[tuple[int, int, int, int]]:
    variants = [r for r in LADDER if info.height >= r[0] and info.width >= int(r[1] * .85)]
    # Small sources are never upscaled; encode a dimension-preserving 360p-like track.
    if not variants:
        even_height = max(2, info.height // 2 * 2)
        even_width = max(2, info.width // 2 * 2)
        variants = [(even_height, even_width, 650_000, 96_000)]
    return variants


def _validate_playlist(folder: Path) -> list[Path]:
    """Parse the exact VOD subset our encoder emits; reject URI/tag injection."""
    playlist = folder / "index.m3u8"
    if folder.is_symlink() or playlist.is_symlink() or not playlist.is_file() or playlist.stat().st_size > 256_000:
        raise RuntimeError("Missing or oversized HLS variant playlist")
    lines = [line.strip() for line in playlist.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not lines or lines[0] != "#EXTM3U" or lines[-1] != "#EXT-X-ENDLIST":
        raise RuntimeError("Incomplete HLS VOD playlist")
    for required in ("#EXTM3U", "#EXT-X-VERSION:3", "#EXT-X-PLAYLIST-TYPE:VOD",
                     "#EXT-X-MEDIA-SEQUENCE:0", "#EXT-X-ENDLIST"):
        if lines.count(required) != 1:
            raise RuntimeError("Missing or repeated HLS directive")
    targets = [x for x in lines if x.startswith("#EXT-X-TARGETDURATION:")]
    if len(targets) != 1 or not re.fullmatch(r"#EXT-X-TARGETDURATION:[1-9][0-9]{0,3}", targets[0]):
        raise RuntimeError("Invalid target duration")
    segments: list[Path] = []
    expect_segment = False
    allowed_tags = ("#EXTM3U", "#EXT-X-VERSION:3", "#EXT-X-MEDIA-SEQUENCE:0",
                    "#EXT-X-PLAYLIST-TYPE:VOD", "#EXT-X-ENDLIST", targets[0])
    for line in lines[1:]:
        if line.startswith("#EXTINF:"):
            if expect_segment or not re.fullmatch(r"#EXTINF:[0-9]+(?:\.[0-9]+)?,", line):
                raise RuntimeError("Malformed HLS segment duration")
            duration = float(line[8:-1])
            if not 0 < duration <= int(targets[0].split(':')[1]) + 1:
                raise RuntimeError("HLS segment duration exceeds target")
            expect_segment = True
        elif line.startswith("#"):
            if line not in allowed_tags or expect_segment:
                raise RuntimeError("Unexpected HLS playlist directive")
        else:
            if not expect_segment or line != f"seg_{len(segments):05d}.ts":
                raise RuntimeError("Unexpected HLS segment URI or order")
            path = folder / line
            if path.is_symlink() or not path.is_file() or not 100 <= path.stat().st_size <= 80_000_000:
                raise RuntimeError("Missing or empty HLS segment")
            segments.append(path)
            expect_segment = False
    if expect_segment or not segments or len(segments) > 2500:
        raise RuntimeError("Invalid HLS segment count")
    return segments


def _master_variants(output_dir: Path) -> list[str]:
    master = output_dir / "master.m3u8"
    if output_dir.is_symlink() or master.is_symlink() or not master.is_file() or master.stat().st_size > 10_000:
        raise RuntimeError("Missing or oversized master playlist")
    lines = [line.strip() for line in master.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not lines or lines[0] != "#EXTM3U" or lines.count("#EXTM3U")!=1 or lines.count("#EXT-X-VERSION:3")!=1:
        raise RuntimeError("Invalid master playlist")
    variants: list[str] = []
    expect_variant = False
    for line in lines[1:]:
        if line.startswith("#EXT-X-MEDIA:TYPE=SUBTITLES") or line.startswith("#EXT-X-MEDIA:TYPE=AUDIO"):
            if expect_variant:
                raise RuntimeError("Subtitle tag inside stream descriptor")
            continue
        if line.startswith("#EXT-X-STREAM-INF:"):
            if expect_variant or not re.fullmatch(
                r'#EXT-X-STREAM-INF:BANDWIDTH=[1-9][0-9]{2,9},RESOLUTION=[0-9]{2,5}x[0-9]{2,5}(?:,AUDIO="audio")?(?:,SUBTITLES="subs")?', line
            ):
                raise RuntimeError("Invalid master stream descriptor")
            expect_variant = True
        elif line.startswith("#"):
            if expect_variant or (line != "#EXT-X-VERSION:3" and not line.startswith("# REELNEST:")):
                raise RuntimeError("Unexpected master directive")
        else:
            if not expect_variant or not re.fullmatch(r"[0-9]{2,4}p/index\.m3u8", line):
                raise RuntimeError("Unexpected master variant path")
            if line in variants:
                raise RuntimeError("Duplicate HLS variant")
            variants.append(line)
            expect_variant = False
    if expect_variant or not 1 <= len(variants) <= 5:
        raise RuntimeError("Invalid master variant count")
    validate_captions(output_dir, lines)
    validate_audio(output_dir, lines)
    return variants

def generate_previews(source: Path, output_dir: Path, duration: float, *, timeout: int = 120) -> int:
    """Generate bounded JPEG seek previews and a private WebVTT image index.

    Every thumbnail is a separate JPEG so no extra image library is required.
    Previews are delivered through the same authenticated session/CDN path as HLS.
    """
    if not math.isfinite(duration) or duration <= 0:
        raise InvalidMedia("Invalid preview duration")
    interval = max(10, math.ceil(duration / 120)) if duration >= 10 else 1
    folder = output_dir / "previews"
    folder.mkdir(parents=True, exist_ok=True)
    try:
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
            "-protocol_whitelist", "file,pipe,crypto,data", "-i", str(source),
            "-map", "0:v:0", "-an", "-sn", "-dn",
            "-vf", f"fps=1/{interval}:start_time=0:round=down,scale=w=320:h=180:force_original_aspect_ratio=decrease:flags=lanczos,pad=320:180:(ow-iw)/2:(oh-ih)/2",
            "-frames:v", "120", "-threads", "1", "-pix_fmt", "yuvj420p", "-q:v", "5", str(folder / "thumb_%04d.jpg")
        ], check=True, capture_output=True, text=True, timeout=timeout)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError) as exc:
        raise RuntimeError("Could not generate seek previews") from exc
    images = sorted(folder.glob("thumb_*.jpg"))
    if not images or len(images) > 120 or any(p.stat().st_size < 100 for p in images):
        raise RuntimeError("Invalid preview images")
    def timestamp(value: float) -> str:
        milliseconds = max(0, int(round(value * 1000)))
        hours, remainder = divmod(milliseconds, 3_600_000)
        minutes, remainder = divmod(remainder, 60_000)
        seconds, millis = divmod(remainder, 1000)
        return f"{hours:02}:{minutes:02}:{seconds:02}.{millis:03}"
    lines = ["WEBVTT", ""]
    for index, image in enumerate(images):
        start = index * interval
        end = min(duration, (index + 1) * interval)
        if end <= start:
            break
        lines.extend([f"{timestamp(start)} --> {timestamp(end)}", image.name, ""])
    (folder / "preview.vtt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    # Atlas generation is optional: legacy individual thumbnails remain usable.
    try:
        create_sprite_atlas(folder, duration, interval=interval, timeout=timeout)
    except RuntimeError:
        for artifact in folder.glob("sprite_*.jpg"):
            artifact.unlink(missing_ok=True)
        (folder / "sprites.vtt").unlink(missing_ok=True)
    return len(images)


def validate_package(output_dir: Path, *, decode_samples: bool = True,
                     decode_all_segments: bool = False, duration_seconds: float | None = None) -> None:
    """Validate every manifest URI and file; optionally decode all media frames.

    Full decode is expensive for feature-length films; it can be explicitly enabled
    for a strict QA run. Representative decode remains the normal worker default.
    """
    variants = _master_variants(output_dir)
    for variant in variants:
        folder = output_dir / variant.split("/")[0]
        segments = _validate_playlist(folder)
        if decode_all_segments:
            # FFmpeg reads the *validated local* playlist and decodes each segment
            # in one invocation; no network protocol or arbitrary extensions.
            try:
                subprocess.run([
                    "ffmpeg", "-v", "error", "-nostdin", "-protocol_whitelist", "file,crypto,data",
                    "-allowed_extensions", "ts", "-i", str(folder / "index.m3u8"),
                    "-f", "null", "-"
                ], check=True, capture_output=True,
                   timeout=min(1800, max(45, int((duration_seconds or len(segments) * 6) * 2))))
            except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError) as exc:
                raise RuntimeError("Full HLS decode failed") from exc
        elif decode_samples:
            for index in sorted({0, len(segments)//2, len(segments)-1}):
                try:
                    subprocess.run(["ffmpeg", "-v", "error", "-nostdin", "-protocol_whitelist",
                        "file,crypto,data", "-i", str(segments[index]),
                        "-f", "null", "-"], check=True, capture_output=True, timeout=35)
                except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError) as exc:
                    raise RuntimeError("Corrupt HLS segment") from exc
    validate_audio(output_dir, (output_dir / "master.m3u8").read_text(encoding="utf-8").splitlines(),
                   decode_samples=decode_samples, decode_all_segments=decode_all_segments,
                   duration_seconds=duration_seconds)
    previews = output_dir / "previews"
    if previews.exists():
        vtt = previews / "preview.vtt"
        if previews.is_symlink() or vtt.is_symlink() or not vtt.is_file() or not vtt.read_text(encoding="utf-8").startswith("WEBVTT\n"):
            raise RuntimeError("Invalid seek preview index")
        for name in (line.strip() for line in vtt.read_text().splitlines() if line.endswith('.jpg')):
            if not re.fullmatch(r"thumb_[0-9]{4}\.jpg", name) or (previews/name).is_symlink() or not (previews/name).is_file():
                raise RuntimeError("Invalid seek preview image reference")
        sprite_vtt = previews / "sprites.vtt"
        if sprite_vtt.exists():
            if sprite_vtt.is_symlink():
                raise RuntimeError("Unsafe sprite index")
            text = sprite_vtt.read_text(encoding="utf-8")
            if not text.startswith("WEBVTT\n") or len(text) > 40_000:
                raise RuntimeError("Invalid sprite preview index")
            for line in text.splitlines():
                if "#xywh=" not in line:
                    continue
                match = re.fullmatch(r"(sprite_[0-9]{2}\.jpg)#xywh=([0-9]+),([0-9]+),320,180", line)
                if not match or (previews/match[1]).is_symlink() or not (previews/match[1]).is_file():
                    raise RuntimeError("Invalid sprite reference")
                if int(match[2]) > 2880 or int(match[3]) > 1620:
                    raise RuntimeError("Sprite crop outside atlas")
    poster = output_dir / "artwork" / "poster.jpg"
    if poster.is_symlink() or (poster.exists() and not 100 < poster.stat().st_size < 5_000_000):
        raise RuntimeError("Invalid poster image")

def encode(source: Path, output_dir: Path, *, max_duration: int = 14_400,
           timeout_seconds: int = 3600) -> tuple[str, MediaInfo, list[dict]]:
    info = probe(source, max_duration=max_duration)
    output_dir.mkdir(parents=True, exist_ok=True)
    variants = []
    master = ["#EXTM3U", "#EXT-X-VERSION:3", "# REELNEST: locally generated HLS VOD"]
    ladder = _renditions(info)
    budget_per_rendition = max(30, timeout_seconds // len(ladder))
    for height, width, video_bps, audio_bps in ladder:
        name = f"{height}p"
        folder = output_dir / name
        folder.mkdir(parents=True, exist_ok=True)
        # Use scale=-2 to preserve aspect ratio and even output width; never upscale.
        cmd = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
               "-protocol_whitelist", "file,pipe,crypto,data",
               "-i", str(source), "-map", "0:v:0", "-map", "0:a:0?",
               "-map_metadata", "-1", "-sn", "-dn", "-vf",
               f"scale=w=-2:h='min({height},ih)':flags=lanczos,"
               "scale=w=trunc(iw/2)*2:h=trunc(ih/2)*2",
               "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
               "-b:v", str(video_bps), "-maxrate", str(int(video_bps * 1.3)),
               "-bufsize", str(video_bps * 2), "-sc_threshold", "0",
               "-force_key_frames", "expr:gte(t,n_forced*6)",
               "-c:a", "aac", "-b:a", str(audio_bps), "-ac", "2",
               "-f", "hls", "-hls_time", "6", "-hls_playlist_type", "vod",
               "-hls_segment_filename", str(folder / "seg_%05d.ts"),
               str(folder / "index.m3u8")]
        try:
            subprocess.run(cmd, check=True, capture_output=True, text=True,
                           timeout=budget_per_rendition)
        except subprocess.TimeoutExpired as exc:
            raise RuntimeError("Transcoding exceeded time budget") from exc
        except subprocess.CalledProcessError as exc:
            raise InvalidMedia(f"FFmpeg rejected source: {exc.stderr[-300:]}") from exc
        _validate_playlist(folder)
        # Use probed dimensions from the encoded output (not the intended ladder width).
        encoded = probe(folder / "seg_00000.ts", max_duration=max_duration)
        bandwidth = int(video_bps * 1.3 + (audio_bps if info.has_audio else 0))
        master.extend([f"#EXT-X-STREAM-INF:BANDWIDTH={bandwidth},RESOLUTION={encoded.width}x{encoded.height}",
                       f"{name}/index.m3u8"])
        variants.append({"name": name, "width": encoded.width, "height": encoded.height,
                         "bandwidth": bandwidth})
    (output_dir / "master.m3u8").write_text("\n".join(master) + "\n", encoding="utf-8")
    if not variants:
        raise RuntimeError("No usable video renditions")
    # Multilingual embedded audio is a mandatory part of an eligible source package.
    # Fail the attempt rather than silently discard licensed alternate languages.
    package_audio(source, output_dir, info.duration, timeout=min(timeout_seconds, 1800))
    # Preview extraction is best-effort: never publish a broken preview index.
    try:
        generate_previews(source, output_dir, info.duration, timeout=min(180, timeout_seconds))
    except RuntimeError:
        shutil.rmtree(output_dir / "previews", ignore_errors=True)
    # Optional private artwork; a poster failure must not invalidate a good movie.
    try:
        create_poster(source, output_dir, info.duration)
    except RuntimeError:
        shutil.rmtree(output_dir / "artwork", ignore_errors=True)
    validate_package(output_dir, decode_all_segments=os.getenv("REELNEST_FULL_SEGMENT_DECODE", "false").lower() == "true",
                     duration_seconds=info.duration)
    return "master.m3u8", info, variants


def content_type(key: str) -> str:
    if key.endswith(".m3u8"):
        return "application/vnd.apple.mpegurl"
    if key.endswith(".ts"):
        return "video/mp2t"
    if key.endswith(".vtt"):
        return "text/vtt"
    if key.endswith(".jpg"):
        return "image/jpeg"
    return "application/octet-stream"
