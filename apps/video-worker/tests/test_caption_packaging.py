import pathlib
import subprocess
import sys
import tempfile
import unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from pipeline import encode,validate_package
from caption_packaging import package_captions
from timed_text import InvalidCaption

class CaptionPackagingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory(prefix='reelnest-caption-hls-')
        cls.root=pathlib.Path(cls.temp.name)
        source=cls.root/'sample.mp4'
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y',
            '-f','lavfi','-i','testsrc2=size=640x360:rate=12',
            '-t','3','-c:v','libx264','-preset','ultrafast',str(source)],check=True,timeout=40)
        encode(source,cls.root/'out',timeout_seconds=100)
    @classmethod
    def tearDownClass(cls):cls.temp.cleanup()

    def test_packages_multiple_protected_hls_subtitles(self):
        srt='1\n00:00:00,100 --> 00:00:01,500\nHello!'
        langs=package_captions(self.root/'out',3,[{'language':'en','srt':srt},{'language':'hi','srt':srt}])
        self.assertEqual(langs,['en','hi'])
        validate_package(self.root/'out',decode_all_segments=True,duration_seconds=3)
        master=(self.root/'out/master.m3u8').read_text()
        self.assertIn('TYPE=SUBTITLES',master)
        self.assertIn('SUBTITLES="subs"',master)
        self.assertIn('captions/en/index.m3u8',master)
        self.assertIn('captions/hi/index.m3u8',master)
        self.assertIn('X-TIMESTAMP-MAP=LOCAL:00:00:00.000,MPEGTS:0',
                      (self.root/'out/captions/en/captions.vtt').read_text())

    def test_rejects_untrusted_language_and_timing(self):
        srt='00:00:00,100 --> 00:00:01,500\nHello'
        with self.assertRaises(InvalidCaption):
            package_captions(self.root/'out',3,[{'language':'../../x','srt':srt}])
        with self.assertRaises(InvalidCaption):
            package_captions(self.root/'out',3,[{'language':'en','srt':'00:00:01,000 --> 00:00:10,000\nLate'}])

    def test_rejects_unlinked_subtitle_group(self):
        path=self.root/'out/master.m3u8'
        original=path.read_text()
        try:
            path.write_text('\n'.join(line for line in original.splitlines() if not line.startswith('#EXT-X-MEDIA:TYPE=SUBTITLES'))+'\n')
            with self.assertRaises(RuntimeError):validate_package(self.root/'out',decode_samples=False)
        finally:path.write_text(original)

    def test_rejects_tampered_caption_playlist(self):
        path=self.root/'out/captions/en/index.m3u8'
        original=path.read_text()
        try:
            path.write_text(original.replace('captions.vtt','https://evil.example/secret.vtt'))
            with self.assertRaises(RuntimeError):validate_package(self.root/'out',decode_samples=False)
        finally:path.write_text(original)
