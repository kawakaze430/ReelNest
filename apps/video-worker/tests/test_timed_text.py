import sys
from pathlib import Path
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from timed_text import convert_srt_to_vtt,InvalidCaption

class CaptionTests(unittest.TestCase):
    def test_valid_srt(self):
        result=convert_srt_to_vtt('1\n00:00:00,100 --> 00:00:01,300\nHello!\n\n2\n00:00:01,500 --> 00:00:02,000\nBonjour',duration_seconds=3)
        self.assertIn('WEBVTT\n',result)
        self.assertIn('00:00:00.100 --> 00:00:01.300',result)
        self.assertIn('Bonjour',result)
    def test_reject_markup_and_bidi(self):
        for text in ['<script>alert(1)</script>','Hello\u202e']:
            with self.assertRaises(InvalidCaption):
                convert_srt_to_vtt('1\n00:00:00,100 --> 00:00:01,300\n'+text,duration_seconds=2)
    def test_reject_overlap_and_out_of_range(self):
        with self.assertRaises(InvalidCaption):
            convert_srt_to_vtt('00:00:00,100 --> 00:00:01,300\nA\n\n00:00:01,200 --> 00:00:01,900\nB',duration_seconds=3)
        with self.assertRaises(InvalidCaption):
            convert_srt_to_vtt('00:00:00,100 --> 00:00:03,300\nA',duration_seconds=2)
    def test_reject_excessive_cues(self):
        with self.assertRaises(InvalidCaption):
            convert_srt_to_vtt('0\n00:00:00,000 --> 00:00:01,000\nA\n\n'*3001,duration_seconds=2)
