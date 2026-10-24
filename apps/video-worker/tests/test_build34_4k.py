"""Build34 opt-in 4K SDR: never upscale, never silently convert HDR."""
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from pipeline import _renditions,MediaInfo,InvalidMedia,LADDER_4K_SDR

class Optional4KSdrPolicy(unittest.TestCase):
    def test_opt_in_required_for_true_4k_sdr_source(self):
        source=MediaInfo(3840,2160,80.0,True,"hevc",False)
        with patch.dict(os.environ,{"REELNEST_ENABLE_4K_SDR_H264":"false"}):
            self.assertNotIn(LADDER_4K_SDR,_renditions(source))
        with patch.dict(os.environ,{"REELNEST_ENABLE_4K_SDR_H264":"true"}):
            self.assertEqual(_renditions(source)[-1],LADDER_4K_SDR)
            self.assertEqual(len(_renditions(source)),5)

    def test_no_upscale_from_1080_or_portrait(self):
        with patch.dict(os.environ,{"REELNEST_ENABLE_4K_SDR_H264":"true"}):
            for width,height in [(1920,1080),(2160,3840),(2560,1440)]:
                self.assertNotIn(LADDER_4K_SDR,
                                 _renditions(MediaInfo(width,height,60.0,True,"h264",False)))

    def test_hdr_source_fails_even_with_opt_in(self):
        with patch.dict(os.environ,{"REELNEST_ENABLE_4K_SDR_H264":"true"}):
            with self.assertRaises(InvalidMedia):
                _renditions(MediaInfo(3840,2160,60.0,True,"hevc",True))

if __name__=="__main__":
    unittest.main()
