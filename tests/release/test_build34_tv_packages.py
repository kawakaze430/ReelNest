"""No runtime device, signing or paid network needed to test TV packaging."""
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET
import pytest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location("tv_packager",ROOT/"scripts/package_tv_clients.py")
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def test_https_origin_is_required():
    assert module.validated_origin("https://api.example.org")=="https://api.example.org"
    for s in ("http://api.example.org","https://evil.org/a","https://user:pass@evil.org",
              "https://127.0.0.1","https://evil.org/?token=1","javascript:alert(1)"):
        with pytest.raises(ValueError):module.validated_origin(s)

def test_both_packages_are_csp_restricted(tmp_path):
    dirs=module.build(tmp_path/"packages","https://api.example.org")
    assert len(dirs)==2
    for d in dirs:
        html=(d/"index.html").read_text()
        assert "connect-src https://api.example.org" in html
        assert "__REELNEST_CONNECT_ORIGIN__" not in html
        assert "REELNEST_API_ORIGIN" in (d/"config.js").read_text()
        assert (d/"tv.js").is_file()
        assert "api/v1/catalog" in (d/"tv.js").read_text()
    xml=ET.parse(dirs[0]/"config.xml")
    access=[v for v in xml.getroot().iter() if v.tag.endswith("access")]
    assert len(access)==1 and access[0].attrib["origin"]=="https://api.example.org"
    data=json.loads((dirs[1]/"appinfo.json").read_text())
    assert data["type"]=="web" and data["main"]=="index.html"
