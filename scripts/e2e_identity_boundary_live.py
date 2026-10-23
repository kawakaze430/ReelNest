#!/usr/bin/env python3
"""Read-only negative authorization probe. Use two independently authenticated
accounts against *your own isolated development stack*. Never prints tokens.

Required environment: REELNEST_API_BASE, REELNEST_TOKEN_A, REELNEST_TOKEN_B,
REELNEST_PROFILE_A (an existing profile belonging to A).
"""
import os,sys,urllib.error,urllib.request,urllib.parse

def require(key):
    v=os.getenv(key,'').strip()
    if not v:raise RuntimeError('Missing '+key)
    return v

def main():
    base=require('REELNEST_API_BASE').rstrip('/')
    if not (base.startswith('http://127.0.0.1:') or base.startswith('http://localhost:') or base.startswith('https://')):
        raise RuntimeError('API must be localhost or HTTPS')
    a=require('REELNEST_TOKEN_A');b=require('REELNEST_TOKEN_B');pid=require('REELNEST_PROFILE_A')
    def get(path, token):
        req=urllib.request.Request(base+path,headers={'Authorization':'Bearer '+token,'Accept':'application/json'})
        try:
            with urllib.request.urlopen(req,timeout=10) as response:return response.status
        except urllib.error.HTTPError as ex:return ex.code
    own=get('/api/v1/profiles',a)
    other=get('/api/v1/playback/continue-watching?profileId='+urllib.parse.quote(pid),b)
    wrong_audience=get('/api/v1/me',b+'.invalid')
    assert own==200, f'Owner profiles request failed ({own}); check stack before concluding IDOR test'
    assert other in (403,404),f'Cross-account profile history exposed/incorrect: HTTP {other}'
    assert wrong_audience==401,f'Tampered JWT unexpectedly accepted: HTTP {wrong_audience}'
    print('PASS: authorized profiles, cross-account history isolation, invalid JWT rejection')

if __name__=='__main__':
    try:main()
    except Exception as ex:
        print('FAILED/BLOCKED: '+str(ex),file=sys.stderr);sys.exit(1)
