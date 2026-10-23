"""Regressions protecting cumulative, model-agnostic ReelNest handoffs."""
from pathlib import Path
from collections import Counter
import csv
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
HANDOFF = ROOT / 'docs' / 'AI_HANDOFF'

class HandoffContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with (ROOT/'docs/BUILD23_282_TASKS.csv').open(encoding='utf-8-sig', newline='') as f:
            cls.rows = list(csv.DictReader(f))
        cls.context = json.loads((ROOT/'AI_HANDOFF_CONTEXT.json').read_text())

    def test_all_282_ids_unique_and_valid(self):
        self.assertEqual(282,len(self.rows))
        self.assertEqual(282,len({r['task_id'] for r in self.rows}))
        self.assertEqual(set('ABCDEFGHIJKLMNOPQ'), {r['task_id'].split('-')[0] for r in self.rows})

    def test_summary_counts_match_canonical_csv(self):
        self.assertEqual(dict(Counter(r['build23_status'] for r in self.rows)),self.context['status_counts'])
        self.assertEqual(282,sum(self.context['status_counts'].values()))
        self.assertEqual(0,self.context['fully_acceptance_verified_count'])

    def test_all_machine_items_preserve_descriptions(self):
        items=[json.loads(s) for s in (HANDOFF/'ALL_282_TASKS.jsonl').read_text().splitlines()]
        self.assertEqual(282,len(items))
        self.assertEqual([r['task_id'] for r in self.rows],[v['id'] for v in items])
        self.assertEqual([r['description'] for r in self.rows],[v['description'] for v in items])

    def test_all_documentation_exists(self):
        self.assertTrue((ROOT/'AI_START_HERE.md').exists())
        for p in self.context['files']:
            self.assertTrue((ROOT/p).exists(),p)
        self.assertTrue((HANDOFF/'06_PROMPTS_FOR_OTHER_MODELS.md').exists())

    def test_production_no_go_not_overstated(self):
        self.assertEqual('NO-GO',self.context['release_decision'])
        txt=(HANDOFF/'04_RELEASE_GATES_AND_REAL_TESTS.md').read_text()
        for phrase in ('Gate G0','Gate G1','Gate G2','Gate G3','Gate G4','Gate G5','V1–V25','NOT RUN'):
            self.assertIn(phrase,txt)

if __name__ == '__main__':
    unittest.main()
