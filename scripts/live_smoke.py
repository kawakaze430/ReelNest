#!/usr/bin/env python3
"""Read-only live API security/availability smoke check. No account or payment mutations.

Requires the entire Docker stack and migrated PostgreSQL. Endpoint responses are
asserted, not simulated. Works with urllib from Python's standard library.
"""
import argparse
import datetime as dt
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request
import uuid

CHECKS=[
  ('health', '/actuator/health', {200}),
  ('public_catalog', '/api/v1/catalog?size=1', {200}),
  ('public_plans','/api/v1/billing/plans', {200}),
  ('private_account','/api/v1/me', {401}),
  ('private_data_export','/api/v1/privacy/export', {401}),
  ('private_creator','/api/v1/creators/me', {401}),
  ('private_playback','/api/v1/playback/continue-watching?profileId=00000000-0000-4000-8000-000000000001', {401}),
  ('private_media_segment','/api/v1/playback/sessions/'+str(uuid.uuid4())+'/files/master.m3u8', {401}),
]

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--api',default='http://localhost:8080')
    ap.add_argument('--output',default='docs/live-smoke.local.json')
    ap.add_argument('--allow-remote',action='store_true',help='Explicitly allow tests against a remote HTTPS API')
    args=ap.parse_args()
    base=args.api.rstrip('/')
    if not args.allow_remote and not (base.startswith('http://localhost:') or base.startswith('http://127.0.0.1:')):
        ap.error('Only local API targets are allowed by default; use --allow-remote for an explicitly approved target')
    if args.allow_remote and not base.startswith('https://'):
        ap.error('Remote smoke tests require HTTPS')
    results={}
    for name,path,allowed in CHECKS:
        req=urllib.request.Request(base+path,headers={'Accept':'application/json','User-Agent':'ReelNest-ReadOnly-Smoke/1.0'})
        try:
            with urllib.request.urlopen(req,timeout=8) as resp:
                code=resp.status
                snippet=resp.read(256).decode('utf8','replace')
        except urllib.error.HTTPError as ex:
            code=ex.code;snippet=ex.read(256).decode('utf8','replace')
        except (OSError,ValueError) as ex:
            code=None;snippet=str(ex)
        passed=code in allowed
        results[name]={'status':'PASS' if passed else 'FAIL','actual_http_status':code,
                       'expected_http_status':sorted(allowed),'excerpt':snippet[:180]}
        print(f"{'PASS' if passed else 'FAIL'}: {name} (HTTP {code}, expected {sorted(allowed)})")
    summary={'generated_at':dt.datetime.now(dt.timezone.utc).isoformat(),
             'target':base,'read_only':True,'results':results,
             'passed':sum(x['status']=='PASS' for x in results.values()),'total':len(results)}
    out=Path(args.output)
    if not out.is_absolute():out=Path(__file__).resolve().parents[1]/out
    out.parent.mkdir(exist_ok=True,parents=True)
    out.write_text(json.dumps(summary,indent=2)+'\n')
    print(f"{summary['passed']}/{summary['total']} passed; evidence saved to {out}")
    return 0 if summary['passed']==summary['total'] else 1
if __name__=='__main__':sys.exit(main())
