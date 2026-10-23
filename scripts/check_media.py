#!/usr/bin/env python3
"""Verify the original sample clip and HLS packaging with local FFmpeg tools."""
from pathlib import Path
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
assets = root / 'apps' / 'web' / 'public' / 'sample'
mp4 = assets / 'demo.mp4'
playlist = assets / 'hls' / 'index.m3u8'
assert mp4.exists() and playlist.exists(), 'Missing demo video or HLS playlist'
segments = sorted(playlist.parent.glob('segment_*.ts'))
assert len(segments) >= 3, 'Expected multiple HLS segments'
text = playlist.read_text()
assert '#EXTM3U' in text and '#EXT-X-ENDLIST' in text
assert all(seg.name in text for seg in segments)
assert (root / 'preview' / 'media' / 'demo.mp4').read_bytes() == mp4.read_bytes()
if shutil.which('ffprobe'):
    raw = subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(mp4)],text=True)
    duration = float(raw.strip())
    assert 7.5 <= duration <= 8.5, f'Unexpected duration {duration}'
if shutil.which('ffmpeg'):
    subprocess.run(['ffmpeg','-v','error','-i',str(playlist),'-f','null','-'],check=True,stdout=subprocess.DEVNULL)
print(f'PASS: original sample clip, HLS playlist and {len(segments)} segments')
