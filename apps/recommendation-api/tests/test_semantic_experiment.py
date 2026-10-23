import os
import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from semantic_search import search, SearchFilm
from experiments import bucket, summarize_binary
from drift import population_stability_index

class SearchExperiments(unittest.TestCase):
    def setUp(self):
        self.movies=[SearchFilm('a','Orbit Rescue','A team explores the space station','Sci-Fi','en'),
                     SearchFilm('b','Village Rain','A family rebuilds a farm','Drama','hi'),
                     SearchFilm('c','Solar Mission','Scientists launch a spacecraft','Sci-Fi','en')]
    def test_semantic_retrieves_relevant_only(self):
        got=search('space station',self.movies)
        self.assertEqual(got[0][0],'a')
        self.assertNotIn('b',[x[0] for x in got])
    def test_semantic_no_matching_returns_empty(self):
        self.assertEqual(search('xyzqqv',self.movies),[])
    def test_semantic_invalid_query(self):
        with self.assertRaises(ValueError):search(' ',self.movies)
    def test_semantic_duplicates(self):
        with self.assertRaises(ValueError):search('space',[self.movies[0],self.movies[0]])
    def test_deterministic_experiment(self):
        secret='A-long-per-deployment-test-secret-01234'
        self.assertEqual(bucket('profile','rec-v1',secret),bucket('profile','rec-v1',secret))
    def test_experiment_requires_secret(self):
        with self.assertRaises(ValueError):bucket('profile','rec-v1','weak')
    def test_wilson_interval(self):
        stats=summarize_binary(10,100)
        self.assertTrue(stats['lower95']<stats['rate']<stats['upper95'])
    def test_drift(self):
        self.assertAlmostEqual(population_stability_index(['hi']*10,['hi']*10),0)
        self.assertGreater(population_stability_index(['hi']*10,['en']*10),1)

if __name__=='__main__':unittest.main()
