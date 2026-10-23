import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from pipeline import probe, encode, content_type, InvalidMedia, validate_package, _renditions, MediaInfo


class PipelineTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='reelnest-worker-tests-')
        self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)

    def sample(self, resolution='1280x720', duration=2):
        output=self.root/'sample.mp4'
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y',
          '-f','lavfi','-i',f'testsrc2=size={resolution}:rate=24',
          '-f','lavfi','-i','sine=frequency=440:sample_rate=44100',
          '-t',str(duration),'-c:v','libx264','-preset','ultrafast','-pix_fmt','yuv420p',
          '-c:a','aac','-shortest',str(output)],check=True,timeout=90)
        return output

    def test_1080p_ladder_only_when_source_supports_it(self):
        self.assertEqual([r[0] for r in _renditions(MediaInfo(1920,1080,60,True,'h264'))],
                         [360,480,720,1080])
        self.assertEqual([r[0] for r in _renditions(MediaInfo(1280,720,60,True,'h264'))],
                         [360,480,720])

    def test_media_probe_rejects_invalid(self):
        invalid=self.root/'garbage.mp4'
        invalid.write_bytes(b'this is not video')
        with self.assertRaises(InvalidMedia):
            probe(invalid)

    def test_encode_adaptive_ladder_and_playlists(self):
        src=self.sample()
        info=probe(src)
        self.assertEqual(info.width,1280)
        self.assertTrue(info.has_audio)
        manifest, _, variants=encode(src,self.root/'out',timeout_seconds=120)
        self.assertEqual(manifest,'master.m3u8')
        self.assertEqual([v['name'] for v in variants],['360p','480p','720p'])
        master=(self.root/'out'/manifest).read_text()
        self.assertEqual(master.count('#EXT-X-STREAM-INF'),3)
        self.assertEqual(content_type('720p/index.m3u8'),'application/vnd.apple.mpegurl')
        self.assertEqual(content_type('previews/preview.vtt'),'text/vtt')
        self.assertEqual(content_type('previews/thumb_0001.jpg'),'image/jpeg')
        preview=self.root/'out'/'previews'/'preview.vtt'
        self.assertTrue(preview.exists())
        self.assertIn('thumb_0001.jpg',preview.read_text())
        self.assertTrue((preview.parent/'thumb_0001.jpg').stat().st_size>100)
        validate_package(self.root/'out')
        for variant in variants:
            folder=self.root/'out'/variant['name']
            self.assertIn('#EXT-X-ENDLIST',(folder/'index.m3u8').read_text())
            segment=next(folder.glob('seg_*.ts'))
            self.assertTrue(segment.stat().st_size>0)
            # Test media output actually decodes (not just playlist existence).
            subprocess.run(['ffmpeg','-v','error','-i',str(segment),'-f','null','-'],
                           check=True,timeout=30)

    def test_package_rejects_missing_segment(self):
        src=self.sample('320x240')
        encode(src,self.root/'out-broken',timeout_seconds=90)
        segment=next((self.root/'out-broken').glob('**/seg_*.ts'))
        segment.unlink()
        with self.assertRaises(RuntimeError):
            validate_package(self.root/'out-broken',decode_samples=False)

    def test_low_resolution_does_not_upscale(self):
        src=self.sample('320x240')
        _,_,variants=encode(src,self.root/'out-low',timeout_seconds=90)
        self.assertEqual(len(variants),1)
        self.assertLessEqual(variants[0]['width'],320)
        self.assertLessEqual(variants[0]['height'],240)

    def test_video_without_audio_is_supported(self):
        source=self.root/'silent.mp4'
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y',
          '-f','lavfi','-i','testsrc2=size=640x360:rate=24',
          '-t','1.5','-c:v','libx264','-preset','ultrafast','-an',str(source)],
          check=True,timeout=60)
        self.assertFalse(probe(source).has_audio)
        _,_,variants=encode(source,self.root/'out-silent',timeout_seconds=60)
        self.assertEqual(len(variants),1)
        self.assertTrue((self.root/'out-silent'/'360p'/'index.m3u8').exists())

    def test_rejects_disguised_playlist(self):
        fake=self.root/'fake.mp4'
        fake.write_text('#EXTM3U\n#EXT-X-TARGETDURATION:6\nhttp://example.invalid/video.ts\n')
        with self.assertRaises(InvalidMedia):
            probe(fake)


if __name__=='__main__':
    unittest.main()
