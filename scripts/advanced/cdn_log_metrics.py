#!/usr/bin/env python3
"""Offline, privacy-conscious CloudFront standard-log metrics.

Viewer response bytes are NOT origin egress. The latter needs origin-side logs.
No AWS access, automatic provisioning, event-level output or user identifiers.
"""
from __future__ import annotations
import argparse
import gzip
import json
from pathlib import Path
import sys

FIELDS={"x-edge-result-type","sc-bytes","sc-status"}
HITS={"Hit","RefreshHit"}
MISSES={"Miss","OriginShieldHit","LimitExceeded","CapacityExceeded"}
MAX_FILE=50*1024*1024
MAX_LINES=500000

def measure(path: Path) -> dict:
    if path.stat().st_size>MAX_FILE:
        raise ValueError("Log file exceeds 50 MiB bound")
    open_fn=gzip.open if path.suffix==".gz" else open
    fieldnames=None
    viewed=hits=misses=errors4=errors5=bytes_viewed=unknown=0
    with open_fn(path,"rt",encoding="utf-8") as stream:
        for n,line in enumerate(stream,1):
            if n>MAX_LINES:raise ValueError("Too many log lines")
            # Bound each decompressed row as well as compressed file size to prevent
            # gzip expansion or a forged enormous field from exhausting CI memory.
            if len(line)>16384:raise ValueError("CloudFront log row exceeds safe limit")
            line=line.rstrip("\r\n")
            if line.startswith("#Fields:"):
                fieldnames=line[len("#Fields:"):].strip().split()
                if not FIELDS.issubset(fieldnames) or len(fieldnames)!=len(set(fieldnames)):
                    raise ValueError("Missing or duplicate CloudFront log columns")
                continue
            if not line or line.startswith("#"):continue
            if fieldnames is None:raise ValueError("CloudFront #Fields header required")
            cells=line.split("\t")
            if len(cells)!=len(fieldnames):raise ValueError("Malformed tab-separated record")
            row=dict(zip(fieldnames,cells))
            try:
                response_bytes=int(row["sc-bytes"])
                status=int(row["sc-status"])
            except ValueError as exc:raise ValueError("Invalid CloudFront counts") from exc
            if not 0<=response_bytes<=10**12 or not 100<=status<=599:
                raise ValueError("Out-of-range CloudFront response")
            viewed+=1
            bytes_viewed+=response_bytes
            kind=row["x-edge-result-type"]
            if kind in HITS:hits+=1
            elif kind in MISSES:misses+=1
            else:unknown+=1
            errors4+=400<=status<500
            errors5+=500<=status<600
    if not viewed:raise ValueError("No viewer requests to analyze")
    known=hits+misses
    return {
        "schema_version":1,"requests":viewed,
        "cache_hits":hits,"cache_misses_or_origin_paths":misses,
        "other_result_types":unknown,
        "cache_hit_rate_pct":round(100*hits/known,2) if known else None,
        "viewer_response_bytes":bytes_viewed,
        "viewer_4xx":errors4,"viewer_5xx":errors5,
        "origin_egress_bytes":None,
        "origin_egress_verified":False,
        "note":"Cache-hit percentage only among recognized result types; origin egress requires trusted origin-side metrics.",
    }

def main(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("logfile",type=Path)
    p.add_argument("--output",type=Path)
    args=p.parse_args(argv)
    try:
        result=measure(args.logfile)
    except (ValueError,OSError,UnicodeError,EOFError) as exc:
        print("INVALID LOG: "+str(exc),file=sys.stderr)
        return 2
    rendered=json.dumps(result,indent=2,sort_keys=True)
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(rendered+"\n",encoding="utf-8")
    print(rendered)
    return 0

if __name__=="__main__":
    raise SystemExit(main())
