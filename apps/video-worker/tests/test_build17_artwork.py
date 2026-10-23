"""Real FFmpeg artwork, HLS integrity and fail-closed URI regression tests."""
import pathlib
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from pipeline import encode, validate_package, _validate_playlist


class ArtworkAndIntegrityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory(prefix='reelnest-build17-')
        cls.root = pathlib.Path(cls.tmp.name)
        source = cls.root / 'clip.mp4'
        subprocess.run([
            'ffmpeg', '-hide_banner', '-loglevel', 'error', '-y',
            '-f', 'lavfi', '-i', 'testsrc2=size=640x360:rate=12',
            '-t', '3', '-c:v', 'libx264', '-preset', 'ultrafast', str(source)
        ], check=True, timeout=40)
        cls.out = cls.root / 'hls'
        encode(source, cls.out, timeout_seconds=100)

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def test_private_poster_and_sprite_are_real_jpegs(self):
        for path in (self.out/'artwork/poster.jpg', self.out/'previews/sprite_01.jpg'):
            self.assertTrue(path.is_file())
            self.assertGreater(path.stat().st_size, 100)
            probe = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'stream=width,height',
                '-of', 'csv=p=0', str(path)], capture_output=True, text=True, check=True)
            self.assertTrue(probe.stdout.strip())
        self.assertIn('sprite_01.jpg#xywh=0,0,320,180',
                      (self.out/'previews/sprites.vtt').read_text())

    def test_full_decode_of_every_segment(self):
        validate_package(self.out, decode_all_segments=True, duration_seconds=3)

    def test_rejects_untrusted_playlist_uris_and_tags(self):
        path = self.out/'360p/index.m3u8'
        original = path.read_text()
        try:
            path.write_text(original.replace('seg_00000.ts', 'https://evil.example/segment.ts'))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
            path.write_text(original.replace('seg_00000.ts', '../seg_00000.ts'))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
            path.write_text(original.replace('#EXT-X-ENDLIST', '#EXT-X-KEY:METHOD=AES-128,URI="https://evil.example/key"\n#EXT-X-ENDLIST'))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_text(original)

    def test_rejects_extra_or_duplicated_master_variants(self):
        path = self.out/'master.m3u8'
        original = path.read_text()
        try:
            path.write_text(original + '#EXT-X-STREAM-INF:BANDWIDTH=1000,RESOLUTION=640x360\n360p/index.m3u8\n')
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_text(original)

    def test_rejects_missing_sprite(self):
        path = self.out/'previews/sprite_01.jpg'
        backup = path.read_bytes()
        try:
            path.unlink()
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_bytes(backup)
