#!/usr/bin/env python3
"""Read-only decision aid for optional scaling and CDN origin shield.

NEVER deploys or enables scaling. Absence of observed traffic must fail closed.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys

def review(evidence: dict) -> dict:
    if not isinstance(evidence,dict):
        raise ValueError("Evidence must be a JSON object")
    required=("observed_hours","requests","cache_hit_rate_pct",
        "origin_egress_gb_24h","p95_origin_latency_ms","estimated_monthly_savings_usd")
    if any(isinstance(evidence.get(k),bool) or not isinstance(evidence.get(k),(int,float))
           for k in required):
        raise ValueError("All evidence fields must be finite numeric values")
    import math
    if any(not math.isfinite(evidence[k]) for k in required):
        raise ValueError("NaN/Infinity evidence forbidden")
    hours=evidence["observed_hours"];requests=evidence["requests"]
    hit=evidence["cache_hit_rate_pct"]
    egress=evidence["origin_egress_gb_24h"]
    latency=evidence["p95_origin_latency_ms"]
    savings=evidence["estimated_monthly_savings_usd"]
    if not (0<=hit<=100 and 0<=hours<=8760 and 0<=requests<=10**12
            and 0<=egress<=10**9 and 0<=latency<=10**8 and -10**9<=savings<=10**9):
        raise ValueError("Evidence field outside valid bounds")
    adequate=hours>=168 and requests>=10000
    benefit=hit<80 and egress>=100 and latency>=200 and savings>=100
    return {
        "observations_sufficient":adequate,
        "origin_shield_candidate":bool(adequate and benefit),
        "autoscaling_review_candidate":bool(adequate and latency>=600 and requests>=100000),
        "regional_routing_candidate":False,
        "changes_applied":False,
        "requires_manual_security_cost_and_failover_review":True,
        "reason":"Advisory only; measurements must be validated against origin billing and load tests.",
    }

def main(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("evidence_json",type=Path)
    p.add_argument("--output",type=Path)
    args=p.parse_args(argv)
    try:
        evidence=json.loads(args.evidence_json.read_text())
        result=review(evidence)
    except (ValueError,OSError,UnicodeError) as exc:
        print("INVALID: "+str(exc),file=sys.stderr)
        return 2
    out=json.dumps(result,indent=2,sort_keys=True)
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(out+"\n")
    print(out)
    return 0

if __name__=="__main__":
    raise SystemExit(main())
