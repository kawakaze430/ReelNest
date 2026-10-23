"""Package creator-owned SRT sidecars as protected HLS WebVTT subtitle renditions."""
from __future__ import annotations
import math
from pathlib import Path
import re
from timed_text import convert_srt_to_vtt, InvalidCaption

LANGUAGE = re.compile(r"[a-z]{2,3}(?:-[a-z]{2,8})?")
SUBTITLE_TAG = re.compile(r'#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="([a-z]{2,3}(?:-[a-z]{2,8})?)",LANGUAGE="\1",AUTOSELECT=YES,DEFAULT=(YES|NO),URI="captions/\1/index.m3u8"')


def package_captions(output: Path, duration: float, sources: list[dict]) -> list[str]:
    """Validate before writing; if any track is invalid the job must be retried.

    Sources are retrieved from a title-owned DB table, never from user-supplied
    paths. A single WebVTT media segment is sufficient for a bounded 200KB SRT.
    """
    if not math.isfinite(duration) or not 0 < duration <= 14_400:
        raise InvalidCaption('Invalid media duration')
    if len(sources)>8:
        raise InvalidCaption('Too many subtitle tracks')
    checked: list[tuple[str,str]] = []
    for row in sources:
        language = row.get('language')
        if not isinstance(language,str) or not LANGUAGE.fullmatch(language) or any(lang==language for lang,_ in checked):
            raise InvalidCaption('Invalid or duplicate caption language')
        checked.append((language,convert_srt_to_vtt(row['srt'],duration_seconds=duration)))
    if not checked:
        return []
    master=output/'master.m3u8'
    text=master.read_text(encoding='utf-8')
    if '#EXT-X-MEDIA:TYPE=SUBTITLES' in text:
        raise InvalidCaption('Subtitle tracks already packaged')
    duration_int=max(1,math.ceil(duration))
    for language,vtt in checked:
        folder=output/'captions'/language
        folder.mkdir(parents=True,exist_ok=True)
        # HLS WebVTT segments need a media-timeline mapping for MPEG-TS A/V.
        hls_vtt=vtt.replace('WEBVTT\n\n','WEBVTT\nX-TIMESTAMP-MAP=LOCAL:00:00:00.000,MPEGTS:0\n\n',1)
        (folder/'captions.vtt').write_text(hls_vtt,encoding='utf-8')
        (folder/'index.m3u8').write_text('\n'.join([
            '#EXTM3U','#EXT-X-VERSION:3',f'#EXT-X-TARGETDURATION:{duration_int}',
            '#EXT-X-MEDIA-SEQUENCE:0','#EXT-X-PLAYLIST-TYPE:VOD',
            f'#EXTINF:{duration:.3f},','captions.vtt','#EXT-X-ENDLIST',''
        ]),encoding='utf-8')
    tags=[f'#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="{language}",LANGUAGE="{language}",AUTOSELECT=YES,DEFAULT={"YES" if i==0 else "NO"},URI="captions/{language}/index.m3u8"'
          for i,(language,_) in enumerate(checked)]
    lines=text.splitlines()
    index=next((i for i,line in enumerate(lines) if line.startswith('#EXT-X-STREAM-INF:')),None)
    if index is None:raise InvalidCaption('Missing HLS variants')
    lines[index:index]=tags
    lines=[line+',SUBTITLES="subs"' if line.startswith('#EXT-X-STREAM-INF:') else line for line in lines]
    master.write_text('\n'.join(lines)+'\n',encoding='utf-8')
    return [language for language,_ in checked]


def validate_captions(output: Path, master_lines: list[str]) -> None:
    tags=[line for line in master_lines if line.startswith('#EXT-X-MEDIA:TYPE=SUBTITLES')]
    if not tags:
        if any('SUBTITLES="subs"' in line for line in master_lines if line.startswith('#EXT-X-STREAM-INF:')):
            raise RuntimeError('Missing HLS subtitle group')
        return
    if len(tags)>8 or len(set(tags))!=len(tags):
        raise RuntimeError('Invalid subtitle tag count')
    languages=[]
    for tag in tags:
        match=SUBTITLE_TAG.fullmatch(tag)
        if not match:raise RuntimeError('Untrusted HLS subtitle URI')
        lang=match[1]
        if lang in languages:raise RuntimeError('Duplicate subtitle language')
        languages.append(lang)
        folder=output/'captions'/lang
        playlist=folder/'index.m3u8'
        vtt=folder/'captions.vtt'
        if not playlist.is_file() or not vtt.is_file() or vtt.stat().st_size>220_000:
            raise RuntimeError('Missing or oversized subtitle files')
        lines=[line.strip() for line in playlist.read_text(encoding='utf-8').splitlines() if line.strip()]
        if len(lines)!=8 or lines[0]!='#EXTM3U' or lines[1]!='#EXT-X-VERSION:3' or lines[3]!='#EXT-X-MEDIA-SEQUENCE:0' or lines[4]!='#EXT-X-PLAYLIST-TYPE:VOD' or lines[6]!='captions.vtt' or lines[7]!='#EXT-X-ENDLIST':
            raise RuntimeError('Invalid subtitle playlist structure')
        target=re.fullmatch(r'#EXT-X-TARGETDURATION:([1-9][0-9]{0,4})',lines[2])
        extinf=re.fullmatch(r'#EXTINF:([0-9]+(?:\.[0-9]+)?),',lines[5])
        if not target or not extinf or not 0<float(extinf[1])<=int(target[1]):
            raise RuntimeError('Invalid subtitle segment duration')
        if not vtt.read_text(encoding='utf-8').startswith('WEBVTT\n'):
            raise RuntimeError('Invalid subtitle text')
    if not all('SUBTITLES="subs"' in line for line in master_lines if line.startswith('#EXT-X-STREAM-INF:')):
        raise RuntimeError('Subtitle rendition not linked to video variants')
