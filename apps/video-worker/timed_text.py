"""Strict, offline SRT -> WebVTT conversion for future creator subtitle ingest.

Only textual cues are accepted. External resources, HTML and overlapping cues are rejected.
This is a library, not an upload API; creator authorization is still required at ingest.
"""
from __future__ import annotations
import re

STAMP = re.compile(r"^(?:(\d{1,2}):)?(\d{2}):(\d{2})[,.](\d{3})$")
TIME_RANGE = re.compile(r"^\s*(\S+)\s+-->\s+(\S+)\s*$")
MAX_BYTES = 200_000
MAX_CUES = 3000


class InvalidCaption(ValueError):
    pass


def _milliseconds(stamp: str) -> int:
    match = STAMP.fullmatch(stamp)
    if not match:
        raise InvalidCaption('Invalid caption timestamp')
    hours, minutes, seconds, ms = (int(v) if v is not None else 0 for v in match.groups())
    if minutes >= 60 or seconds >= 60 or hours > 23:
        raise InvalidCaption('Caption timestamp out of range')
    return ((hours * 60 + minutes) * 60 + seconds) * 1000 + ms


def _timestamp(ms: int) -> str:
    hours, rem = divmod(ms, 3_600_000)
    minutes, rem = divmod(rem, 60_000)
    seconds, milli = divmod(rem, 1000)
    return f'{hours:02}:{minutes:02}:{seconds:02}.{milli:03}'


def convert_srt_to_vtt(content: str, *, duration_seconds: float) -> str:
    if not isinstance(content, str) or len(content.encode('utf-8')) > MAX_BYTES:
        raise InvalidCaption('Caption payload exceeds limit')
    if not (0 < duration_seconds <= 14_400):
        raise InvalidCaption('Invalid media duration')
    blocks = re.split(r'\n\s*\n', content.replace('\r\n', '\n').replace('\r', '\n').lstrip('\ufeff').strip())
    if not blocks or len(blocks) > MAX_CUES:
        raise InvalidCaption('Invalid caption count')
    output = ['WEBVTT', '']
    previous_end = 0
    for index, block in enumerate(blocks, start=1):
        lines = block.splitlines()
        if lines and lines[0].strip().isdigit():
            lines = lines[1:]
        if len(lines) < 2:
            raise InvalidCaption('Missing caption text or timestamp')
        timing = TIME_RANGE.fullmatch(lines[0])
        if not timing:
            raise InvalidCaption('Invalid caption time range')
        start, end = (_milliseconds(x) for x in timing.groups())
        if start < previous_end or end <= start or end > int(duration_seconds * 1000) + 1000:
            raise InvalidCaption('Overlapping, empty or out-of-bounds captions')
        text = '\n'.join(lines[1:]).strip()
        if not text or len(text) > 600 or any(ord(c) < 32 and c not in '\n\t' for c in text):
            raise InvalidCaption('Invalid caption text')
        if '<' in text or '>' in text or '\u202e' in text or '\u202d' in text:
            raise InvalidCaption('Markup or bidi overrides are not permitted')
        output.extend([f'{_timestamp(start)} --> {_timestamp(end)}', text, ''])
        previous_end = end
    return '\n'.join(output) + '\n'
