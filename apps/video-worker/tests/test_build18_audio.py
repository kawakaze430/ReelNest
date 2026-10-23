"""Real FFmpeg multi-language packaging and fail-closed media tests."""
import pathlib
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from pipeline import encode, validate_package
from audio_packaging import inspect_audio_tracks
from caption_packaging import package_captions


class MultilingualAudioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory(prefix='reelnest-multiaudio-')
        cls.root = pathlib.Path(cls.tmp.name)
        cls.source = cls.root / 'multi.mp4'
        subprocess.run([
            'ffmpeg', '-hide_banner', '-loglevel', 'error', '-y',
            '-f', 'lavfi', '-i', 'testsrc2=size=640x360:rate=12',
            '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=44100',
            '-f', 'lavfi', '-i', 'sine=frequency=880:sample_rate=44100',
            '-map', '0:v:0', '-map', '1:a:0', '-map', '2:a:0',
            '-metadata:s:a:0', 'language=eng', '-metadata:s:a:1', 'language=hin',
            '-t', '3', '-c:v', 'libx264', '-preset', 'ultrafast',
            '-c:a', 'aac', '-shortest', str(cls.source)
        ], check=True, timeout=45)
        cls.out = cls.root / 'hls'
        encode(cls.source, cls.out, timeout_seconds=120)

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def test_languages_and_playlists(self):
        self.assertEqual(inspect_audio_tracks(self.source), ['eng', 'hin'])
        master = (self.out / 'master.m3u8').read_text()
        self.assertIn('TYPE=AUDIO,GROUP-ID="audio"', master)
        self.assertIn('LANGUAGE="hin"', master)
        self.assertIn('AUDIO="audio"', master)
        for index in range(2):
            self.assertTrue((self.out / f'audio/a0{index}/seg_00000.ts').is_file())
        validate_package(self.out, decode_samples=True, duration_seconds=3)
        validate_package(self.out, decode_all_segments=True, duration_seconds=3)

    def test_audio_and_subtitles_can_coexist(self):
        # Package in a disposable copy: source fixture is shared by other tests.
        import shutil
        combined = self.root / 'combined'
        if combined.exists(): shutil.rmtree(combined)
        shutil.copytree(self.out, combined)
        srt = '1\n00:00:00,100 --> 00:00:01,200\nHello world'
        package_captions(combined, 3, [{'language':'en','srt':srt}])
        validate_package(combined, decode_samples=True, duration_seconds=3)
        master = (combined / 'master.m3u8').read_text()
        self.assertIn('AUDIO="audio",SUBTITLES="subs"', master)

    def test_rejects_audio_path_injection(self):
        path = self.out / 'audio/a01/index.m3u8'
        original = path.read_text()
        try:
            path.write_text(original.replace('seg_00000.ts', '../seg_00000.ts'))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_text(original)

    def test_rejects_audio_tag_injection(self):
        path = self.out / 'master.m3u8'
        original = path.read_text()
        try:
            path.write_text(original.replace('audio/a01/index.m3u8', 'https://untrusted.invalid/audio.m3u8'))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_text(original)

    def test_rejects_missing_audio_segment(self):
        path = self.out / 'audio/a01/seg_00000.ts'
        backup = path.read_bytes()
        try:
            path.unlink()
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_bytes(backup)

    def test_rejects_unlinked_audio_group(self):
        path = self.out / 'master.m3u8'
        original = path.read_text()
        try:
            path.write_text(original.replace(',AUDIO="audio"', ''))
            with self.assertRaises(RuntimeError): validate_package(self.out, decode_samples=False)
        finally:
            path.write_text(original)

class AudioMetadataPolicyTests(unittest.TestCase):
    def test_unsafe_language_metadata_never_enters_hls(self):
        from unittest.mock import patch
        from subprocess import CompletedProcess
        import json
        payload={'streams':[{'codec_type':'audio','tags':{'language':'en"\n#EXT-X-KEY'}},
                            {'codec_type':'audio','tags':{'language':'hi_IN'}}]}
        with patch('audio_packaging.subprocess.run',return_value=CompletedProcess([],0,json.dumps(payload),'') ):
            self.assertEqual(inspect_audio_tracks(pathlib.Path('/dummy.mp4')),['und','hi-in'])

    def test_audio_track_limit(self):
        from unittest.mock import patch
        from subprocess import CompletedProcess
        import json
        payload={'streams':[{'codec_type':'audio'} for _ in range(9)]}
        with patch('audio_packaging.subprocess.run',return_value=CompletedProcess([],0,json.dumps(payload),'') ):
            from audio_packaging import InvalidAudio
            with self.assertRaises(InvalidAudio):inspect_audio_tracks(pathlib.Path('/dummy.mp4'))
