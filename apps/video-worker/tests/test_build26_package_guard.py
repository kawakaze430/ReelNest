"""Build 26: synthetic media trees ensure unsafe HLS/DASH references fail closed."""
from pathlib import Path
import sys
import tempfile
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from pipeline import _validate_playlist, _master_variants, validate_package
from dash_packaging import validate_dash

HLS="""#EXTM3U
#EXT-X-VERSION:3
#EXT-X-PLAYLIST-TYPE:VOD
#EXT-X-TARGETDURATION:6
#EXT-X-MEDIA-SEQUENCE:0
#EXTINF:5.000,
seg_00000.ts
#EXT-X-ENDLIST
"""
MASTER="""#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=320x240
240p/index.m3u8
"""
DASH='''<?xml version="1.0"?><MPD type="static" xmlns="urn:mpeg:dash:schema:mpd:2011">
<Period><AdaptationSet><Representation id="0" bandwidth="500000">
<SegmentTemplate timescale="1000" initialization="init_$RepresentationID$.m4s"
media="chunk_$RepresentationID$_$Number%05d$.m4s" startNumber="1"/>
</Representation></AdaptationSet></Period></MPD>'''

def hls_tree(root):
    folder=root/"240p"
    folder.mkdir()
    (folder/"index.m3u8").write_text(HLS)
    (folder/"seg_00000.ts").write_bytes(b"T"*200)
    (root/"master.m3u8").write_text(MASTER)
    return folder

def dash_tree(root):
    (root/"stream.mpd").write_text(DASH)
    (root/"init_0.m4s").write_bytes(b"I"*64)
    (root/"chunk_0_00001.m4s").write_bytes(b"C"*64)

def test_local_hls_playlist_valid():
    with tempfile.TemporaryDirectory() as td:
        folder=hls_tree(Path(td))
        assert len(_validate_playlist(folder))==1

def test_symlink_hls_segment_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        folder=hls_tree(root)
        (folder/"seg_00000.ts").unlink()
        (folder/"seg_00000.ts").symlink_to(root/"master.m3u8")
        with pytest.raises(RuntimeError):_validate_playlist(folder)

def test_symlink_hls_variant_playlist_denied():
    with tempfile.TemporaryDirectory() as td:
        folder=hls_tree(Path(td))
        (folder/"index.m3u8").unlink()
        (folder/"index.m3u8").symlink_to(Path(td)/"master.m3u8")
        with pytest.raises(RuntimeError):_validate_playlist(folder)

def test_oversized_hls_segment_denied_without_disk_allocation():
    with tempfile.TemporaryDirectory() as td:
        folder=hls_tree(Path(td))
        with (folder/"seg_00000.ts").open("wb") as out:
            out.truncate(80_000_001)
        with pytest.raises(RuntimeError):_validate_playlist(folder)

def test_hls_master_symlink_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        hls_tree(root)
        (root/"master.m3u8").unlink()
        (root/"master.m3u8").symlink_to(root/"240p"/"index.m3u8")
        with pytest.raises(RuntimeError):_master_variants(root)

def test_hls_segment_uri_injection_denied():
    with tempfile.TemporaryDirectory() as td:
        folder=hls_tree(Path(td))
        (folder/"index.m3u8").write_text(HLS.replace("seg_00000.ts","https://example.org/evil.ts"))
        with pytest.raises(RuntimeError):_validate_playlist(folder)

def test_valid_static_dash_manifest():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        dash_tree(root)
        validate_dash(root)

def test_dash_segment_url_override_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        dash_tree(root)
        mpd=root/"stream.mpd"
        mpd.write_text(DASH.replace("</Representation>", '<SegmentURL media="https://example.invalid/evil.m4s"/></Representation>'))
        with pytest.raises(RuntimeError):validate_dash(root)

def test_dash_doctype_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        dash_tree(root)
        mpd=root/"stream.mpd"
        mpd.write_text('<!DOCTYPE MPD [<!ENTITY test "bad">]>'+DASH)
        with pytest.raises(RuntimeError):validate_dash(root)

def test_dash_symlink_segment_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        dash_tree(root)
        (root/"init_0.m4s").unlink()
        (root/"init_0.m4s").symlink_to(root/"stream.mpd")
        with pytest.raises(RuntimeError):validate_dash(root)

def test_dash_rejects_manifest_without_segment_template():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        dash_tree(root)
        mpd=root/"stream.mpd"
        mpd.write_text(DASH.replace("<SegmentTemplate","<IgnoredTemplate"))
        with pytest.raises(RuntimeError):validate_dash(root)

def test_seek_preview_index_symlink_denied():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        hls_tree(root)
        previews=root/"previews"
        previews.mkdir()
        (previews/"preview.vtt").symlink_to(root/"master.m3u8")
        with pytest.raises(RuntimeError):validate_package(root,decode_samples=False)
