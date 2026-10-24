#!/usr/bin/env python3
"""Package limited-feature Tizen/webOS catalog previews; no signing or deployment."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import shutil
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"apps/tv/shared"

def validated_origin(origin: str) -> str:
    url=urlsplit(origin)
    if (url.scheme!="https" or not url.hostname or url.username or url.password
        or url.path not in ("","/") or url.query or url.fragment or url.port==0):
        raise ValueError("A clean HTTPS API origin is required")
    if url.hostname.lower() in {"localhost","127.0.0.1"}:
        raise ValueError("Public TV packages may not target localhost")
    if any(ch.isspace() for ch in origin):
        raise ValueError("Invalid origin")
    return url.scheme+"://"+url.netloc.lower()

def build(destination: Path, origin: str) -> list[Path]:
    api=validated_origin(origin)
    # Explicitly avoid rewriting a source folder or shared application workspace.
    resolved=destination.resolve()
    if resolved==ROOT.resolve() or ROOT.resolve() in resolved.parents and (
        resolved==SOURCE.resolve() or resolved in SOURCE.resolve().parents):
        raise ValueError("Unsafe output path")
    outputs=[]
    for platform in ("tizen","webos"):
        folder=resolved/platform
        folder.mkdir(parents=True,exist_ok=True)
        html=(SOURCE/"index.html").read_text().replace("__REELNEST_CONNECT_ORIGIN__",api)
        (folder/"index.html").write_text(html,encoding="utf-8")
        (folder/"tv.css").write_bytes((SOURCE/"tv.css").read_bytes())
        (folder/"tv.js").write_bytes((SOURCE/"tv.js").read_bytes())
        (folder/"config.js").write_text(
            "window.REELNEST_API_ORIGIN="+json.dumps(api)+";\n",encoding="utf-8")
        if platform=="tizen":
            root=ET.Element("widget",{
                "xmlns":"http://www.w3.org/ns/widgets",
                "xmlns:tizen":"http://tizen.org/ns/widgets",
                "id":"http://reelnest.invalid/tv-preview","version":"0.1.0","viewmodes":"fullscreen"})
            ET.SubElement(root,"name").text="ReelNest Catalog Preview"
            ET.SubElement(root,"content",{"src":"index.html"})
            ET.SubElement(root,"access",{"origin":api,"subdomains":"false"})
            ET.SubElement(root,"{http://tizen.org/ns/widgets}application",{
                "id":"ReelNestP.ReelNestPreview","package":"ReelNestP","required_version":"6.0"})
            ET.SubElement(root,"{http://tizen.org/ns/widgets}privilege",{
                "name":"http://tizen.org/privilege/internet"})
            ET.ElementTree(root).write(folder/"config.xml",encoding="utf-8",xml_declaration=True)
        else:
            (folder/"appinfo.json").write_text(json.dumps({
                "id":"com.reelnest.catalog.preview","version":"0.1.0",
                "vendor":"ReelNest", "type":"web","main":"index.html",
                "title":"ReelNest Catalog Preview",
                "description":"Read-only catalog UI. No playback or auth."},
                indent=2)+"\n",encoding="utf-8")
        outputs.append(folder)
    return outputs

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-origin",required=True)
    parser.add_argument("--out",type=Path,required=True)
    args=parser.parse_args(argv)
    for path in build(args.out,args.api_origin):
        print("Prepared NOT signed/certified TV preview:",path)
    return 0

if __name__=="__main__":
    raise SystemExit(main())
