"""Bounded, private HLS alternate-audio renditions from *embedded* source tracks.

No user-controlled paths or HLS URLs are accepted. Audio languages are advisory
metadata, not an authorization signal. A separately uploaded audio workflow will
require creator/rights approval and is deliberately outside this module.
"""
from __future__ import annotations
import json
import math
from pathlib import Path
import re
import subprocess

LANGUAGE = re.compile(r"[a-z]{2,3}(?:-[a-z]{2,8})?")
AUDIO_TAG = re.compile(
    r'#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Audio ([1-8])",'
    r'LANGUAGE="([a-z]{2,3}(?:-[a-z]{2,8})?)",AUTOSELECT=YES,'
    r'DEFAULT=(YES|NO),URI="audio/a0([0-7])/index\.m3u8"'
)
MAX_AUDIO_TRACKS = 8


class InvalidAudio(ValueError):
    """Malformed or unsupported alternate-audio source or package."""


def inspect_audio_tracks(source: Path, *, timeout: int = 30) -> list[str]:
    """Return safe HLS language labels in source stream order."""
    try:
        completed = subprocess.run([
            'ffprobe', '-v', 'error', '-protocol_whitelist', 'file,pipe,crypto,data',
            '-select_streams', 'a', '-show_entries', 'stream=codec_type:stream_tags=language',
            '-of', 'json', str(source)
        ], check=True, capture_output=True, text=True, timeout=timeout)
        data = json.loads(completed.stdout)
    except (OSError, ValueError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as exc:
        raise InvalidAudio('Unable to inspect audio tracks') from exc
    streams = data.get('streams', [])
    if not isinstance(streams, list) or len(streams) > MAX_AUDIO_TRACKS:
        raise InvalidAudio('Too many audio tracks')
    languages = []
    for stream in streams:
        if stream.get('codec_type') != 'audio':
            raise InvalidAudio('Invalid audio stream')
        language = stream.get('tags', {}).get('language', 'und')
        if not isinstance(language, str):
            language = 'und'
        language = language.lower().replace('_', '-')
        # Untrusted container tags must never reach a playlist URI or quoted value.
        languages.append(language if LANGUAGE.fullmatch(language) else 'und')
    return languages


def _check_audio_playlist(folder: Path) -> list[Path]:
    """Accept only local, sequential worker-generated MPEG-TS audio segments."""
    path = folder / 'index.m3u8'
    if not path.is_file() or path.stat().st_size > 256_000:
        raise RuntimeError('Missing audio playlist')
    lines = [x.strip() for x in path.read_text(encoding='utf-8').splitlines() if x.strip()]
    if len(lines) < 8 or lines[0] != '#EXTM3U' or lines[-1] != '#EXT-X-ENDLIST':
        raise RuntimeError('Invalid audio playlist')
    required = ('#EXTM3U', '#EXT-X-VERSION:3', '#EXT-X-MEDIA-SEQUENCE:0',
                '#EXT-X-PLAYLIST-TYPE:VOD', '#EXT-X-ENDLIST')
    if any(lines.count(x) != 1 for x in required):
        raise RuntimeError('Missing audio playlist directive')
    targets = [x for x in lines if x.startswith('#EXT-X-TARGETDURATION:')]
    if len(targets) != 1 or not re.fullmatch(r'#EXT-X-TARGETDURATION:[1-9][0-9]{0,3}', targets[0]):
        raise RuntimeError('Invalid audio target duration')
    allowed = set(required) | {targets[0]}
    files = []
    pending = False
    for line in lines[1:]:
        if line.startswith('#EXTINF:'):
            match = re.fullmatch(r'#EXTINF:([0-9]+(?:\.[0-9]+)?),', line)
            if pending or not match or not 0 < float(match[1]) <= int(targets[0].split(':')[1]) + 1:
                raise RuntimeError('Invalid audio segment duration')
            pending = True
        elif line.startswith('#'):
            if pending or line not in allowed:
                raise RuntimeError('Untrusted audio playlist directive')
        else:
            if not pending or line != f'seg_{len(files):05d}.ts':
                raise RuntimeError('Untrusted audio segment URI')
            segment = folder / line
            if not segment.is_file() or segment.stat().st_size < 100:
                raise RuntimeError('Missing audio segment')
            files.append(segment)
            pending = False
    if pending or not 1 <= len(files) <= 2500:
        raise RuntimeError('Invalid audio segment count')
    return files


def package_audio(source: Path, output: Path, duration: float, *, timeout: int = 600) -> list[str]:
    """Create separate HLS audio playlists only for multilingual embedded sources.

    Existing video renditions retain their original muxed audio for backwards
    compatibility. Each separate audio rendition is transcoded to stereo AAC.
    """
    if not math.isfinite(duration) or not 0 < duration <= 14_400:
        raise InvalidAudio('Invalid media duration')
    languages = inspect_audio_tracks(source)
    if len(languages) < 2:
        return []
    master = output / 'master.m3u8'
    lines = master.read_text(encoding='utf-8').splitlines()
    if any('TYPE=AUDIO' in x or 'AUDIO="audio"' in x for x in lines):
        raise InvalidAudio('Audio renditions already packaged')
    tags = []
    for index, language in enumerate(languages):
        folder = output / 'audio' / f'a{index:02d}'
        folder.mkdir(parents=True, exist_ok=True)
        cmd = [
            'ffmpeg', '-hide_banner', '-loglevel', 'error', '-nostdin', '-y',
            '-protocol_whitelist', 'file,pipe,crypto,data', '-i', str(source),
            '-map', f'0:a:{index}', '-vn', '-sn', '-dn', '-map_metadata', '-1',
            '-c:a', 'aac', '-b:a', '128k', '-ac', '2',
            '-f', 'hls', '-hls_time', '6', '-hls_playlist_type', 'vod',
            '-hls_segment_filename', str(folder / 'seg_%05d.ts'),
            str(folder / 'index.m3u8')
        ]
        try:
            subprocess.run(cmd, check=True, capture_output=True, text=True,
                           timeout=max(30, timeout // len(languages)))
        except (OSError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as exc:
            raise InvalidAudio('Could not encode alternate audio') from exc
        _check_audio_playlist(folder)
        tags.append(
            f'#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Audio {index+1}",'
            f'LANGUAGE="{language}",AUTOSELECT=YES,DEFAULT={"YES" if index==0 else "NO"},'
            f'URI="audio/a{index:02d}/index.m3u8"'
        )
    first_variant = next((i for i, line in enumerate(lines) if line.startswith('#EXT-X-STREAM-INF:')), None)
    if first_variant is None:
        raise InvalidAudio('Missing video variants')
    lines[first_variant:first_variant] = tags
    lines = [line + ',AUDIO="audio"' if line.startswith('#EXT-X-STREAM-INF:') else line for line in lines]
    master.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    return languages


def validate_audio(output: Path, master_lines: list[str], *, decode_samples: bool = False,
                   decode_all_segments: bool = False, duration_seconds: float | None = None) -> None:
    tags = [x for x in master_lines if x.startswith('#EXT-X-MEDIA:TYPE=AUDIO')]
    linked = [x for x in master_lines if x.startswith('#EXT-X-STREAM-INF:')]
    if not tags:
        if any('AUDIO="audio"' in line for line in linked):
            raise RuntimeError('Missing HLS audio group')
        return
    if not 2 <= len(tags) <= MAX_AUDIO_TRACKS or len(set(tags)) != len(tags):
        raise RuntimeError('Invalid audio group size')
    if not linked or not all('AUDIO="audio"' in line for line in linked):
        raise RuntimeError('Unlinked HLS audio group')
    seen = set()
    for index, tag in enumerate(tags):
        match = AUDIO_TAG.fullmatch(tag)
        if not match or int(match[1]) != index + 1 or int(match[4]) != index:
            raise RuntimeError('Untrusted HLS audio rendition URI')
        if match[3] != ('YES' if index == 0 else 'NO'):
            raise RuntimeError('Invalid default audio rendition')
        if index in seen:
            raise RuntimeError('Duplicate audio rendition')
        seen.add(index)
        files = _check_audio_playlist(output / 'audio' / f'a{index:02d}')
        if decode_all_segments:
            playlist = output / 'audio' / f'a{index:02d}' / 'index.m3u8'
            try:
                subprocess.run(['ffmpeg', '-v', 'error', '-nostdin', '-protocol_whitelist',
                                'file,crypto,data', '-allowed_extensions', 'ts',
                                '-i', str(playlist), '-f', 'null', '-'],
                               check=True, capture_output=True,
                               timeout=min(1800, max(45, int((duration_seconds or len(files)*6)*2))))
            except (OSError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as exc:
                raise RuntimeError('Full HLS audio decode failed') from exc
        elif decode_samples:
            for segment in {files[0], files[-1]}:
                try:
                    subprocess.run(['ffmpeg', '-v', 'error', '-nostdin', '-protocol_whitelist',
                                    'file,crypto,data', '-i', str(segment), '-f', 'null', '-'],
                                   check=True, capture_output=True, timeout=35)
                except (OSError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as exc:
                    raise RuntimeError('Corrupt HLS audio segment') from exc
