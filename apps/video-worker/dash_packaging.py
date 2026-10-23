"""Optional, local-only MPEG-DASH/fMP4 experimental packager.

Not enabled in the production media worker; no DRM, encryption, or licence integration.
Never accepts remote URLs, uploaded manifests, or arbitrary template names.
"""
from __future__ import annotations
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
from pipeline import probe, InvalidMedia


def package_dash(source: Path, folder: Path, *, timeout: int = 600) -> Path:
    info = probe(source)
    folder.mkdir(parents=True, exist_ok=True)
    if any(folder.iterdir()):
        raise ValueError('DASH output directory must be empty')
    output = folder/'stream.mpd'
    cmd = ['ffmpeg','-hide_banner','-loglevel','error','-nostdin','-y',
           '-protocol_whitelist','file,pipe,crypto,data','-i',str(source),
           '-map','0:v:0','-map','0:a:0?','-map_metadata','-1','-sn','-dn',
           '-c:v','libx264','-pix_fmt','yuv420p','-preset','veryfast',
           '-vf',f"scale=w=-2:h='min(720,ih)':flags=lanczos",
           '-c:a','aac','-b:a','128k','-f','dash','-seg_duration','6',
           '-use_template','1','-use_timeline','1',
           '-init_seg_name','init_$RepresentationID$.m4s',
           '-media_seg_name','chunk_$RepresentationID$_$Number%05d$.m4s',str(output)]
    try:
        subprocess.run(cmd,check=True,capture_output=True,text=True,timeout=timeout)
    except subprocess.TimeoutExpired as exc:
        raise RuntimeError('DASH packager timed out') from exc
    except subprocess.CalledProcessError as exc:
        raise InvalidMedia('DASH packaging failed: '+exc.stderr[-200:]) from exc
    validate_dash(folder)
    return output


def validate_dash(folder: Path) -> None:
    manifest=folder/'stream.mpd'
    if not manifest.is_file() or not 64 < manifest.stat().st_size <= 512_000:
        raise RuntimeError('Missing or oversized DASH manifest')
    try:
        root=ET.fromstring(manifest.read_bytes())
    except ET.ParseError as exc:
        raise RuntimeError('Malformed DASH XML') from exc
    if root.tag.split('}')[-1]!='MPD':
        raise RuntimeError('Not a DASH MPD')
    if root.get('type')!='static':
        raise RuntimeError('Only on-demand DASH is supported')
    segments=list(folder.glob('*.m4s'))
    if not segments or len(segments)>8000:
        raise RuntimeError('Invalid DASH segment count')
    if any(not re.fullmatch(r'(?:init|chunk)_\d+(?:_\d{5})?\.m4s',p.name) or p.stat().st_size<16
           or p.is_symlink() for p in segments):
        raise RuntimeError('Unsafe or invalid DASH segments')
    for element in root.iter():
        if element.tag.split('}')[-1] in ('BaseURL','Location','UTCTiming'):
            raise RuntimeError('DASH manifest cannot reference external resource')
        if element.tag.split('}')[-1]=='SegmentTemplate':
            for key in ('initialization','media'):
                value=element.get(key,'')
                if not re.fullmatch(r'(?:init|chunk)_\$RepresentationID\$(?:_\$Number%05d\$)?\.m4s',value):
                    raise RuntimeError('Untrusted DASH template')
    if not any(p.name.startswith('init_') for p in segments) or not any(p.name.startswith('chunk_') for p in segments):
        raise RuntimeError('Missing initialization or media segments')
