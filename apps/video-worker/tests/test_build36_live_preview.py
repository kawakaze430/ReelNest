"""FFmpeg packaging integration using only original synthetic generated frames."""
from pathlib import Path
import sys
import tempfile
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from live_preview import generate_synthetic_live_preview

def test_refuses_external_existing_output_and_unbounded_duration(tmp_path):
    with pytest.raises(ValueError): generate_synthetic_live_preview(tmp_path)
    for duration in (0,2,11,1000):
        with pytest.raises(ValueError):
            generate_synthetic_live_preview(tmp_path/("invalid-"+str(duration)),duration)
    link=tmp_path/"symlink"
    link.symlink_to(tmp_path,target_is_directory=True)
    with pytest.raises(ValueError):generate_synthetic_live_preview(link)

def test_real_synthetic_2_second_live_hls_packaging(tmp_path):
    playlist=generate_synthetic_live_preview(tmp_path/"generated",6)
    assert playlist.is_file()
    text=playlist.read_text()
    assert "#EXTM3U" in text
    assert "#EXT-X-PROGRAM-DATE-TIME" in text
    assert "#EXT-X-ENDLIST" not in text
    assert "part-" in text
    pieces=list(playlist.parent.glob("part-*.ts"))
    assert pieces
    assert all(0<p.stat().st_size<5_000_000 for p in pieces)
    # No remote ingest port, private media, account data or user upload consumed.
