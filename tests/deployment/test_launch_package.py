from pathlib import Path
import importlib.util
import sys
import tempfile
import zipfile

ROOT=Path(__file__).resolve().parents[2]

def load(name):
    spec=importlib.util.spec_from_file_location(name,ROOT/'scripts'/f'{name}.py')
    m=importlib.util.module_from_spec(spec)
    sys.modules[name]=m
    spec.loader.exec_module(m)
    return m

def test_preflight_never_claims_production_ready():
    data=load('deployment_preflight').collect()
    assert data['release_decision']=='NO-GO'
    assert data['full_ott_production_ready'] is False
    assert len(data['external_gates_unverified'])>=7

def test_static_preview_allowlist_and_valid_zip():
    module=load('prepare_public_preview')
    with tempfile.TemporaryDirectory() as tmp:
        path=Path(tmp)/'preview.zip'
        zip_path, digest=module.create(output=path)
        with zipfile.ZipFile(zip_path) as z:
            assert z.testzip() is None
            assert 'index.html' in z.namelist()
            assert 'media/demo.mp4' in z.namelist()
            assert '404.html' in z.namelist()
            assert 'robots.txt' in z.namelist()
            assert not any('.env' in n or '/src/' in n for n in z.namelist())
            assert len(z.namelist())==len(digest)==4
            assert 'OFFLINE UI PROTOTYPE' in z.read('index.html').decode()
