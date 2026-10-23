"""FFmpeg-backed tests, only on local synthetic/licensed test footage."""
import os
import shutil
import subprocess
import sys
from pathlib import Path
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from dash_packaging import package_dash,validate_dash
class DashPackagingTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which('ffmpeg') and shutil.which('ffprobe'),'FFmpeg required')
    def test_encode_synthetic_mp4_and_reject_external_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);src=base/'src.mp4';out=base/'dash'
            subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','lavfi',
                            '-i','testsrc2=size=320x240:rate=12','-t','2','-c:v','mpeg4',str(src)],
                           check=True,timeout=30)
            mpd=package_dash(src,out,timeout=120)
            self.assertTrue(mpd.is_file());validate_dash(out)
            text=mpd.read_text();mpd.write_text(text.replace('<MPD ','<MPD ><BaseURL>https://evil.example/</BaseURL>',1))
            with self.assertRaises(RuntimeError):validate_dash(out)
    def test_missing_dash(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(RuntimeError):validate_dash(Path(tmp))
if __name__=='__main__':unittest.main()
