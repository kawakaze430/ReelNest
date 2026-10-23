#!/usr/bin/env python3
"""Generate identity realm template with NO demo users and strict staging redirect URLs.
The output is only a setup file. Operator must configure TLS, SMTP and admin MFA.
"""
from __future__ import annotations
import argparse,json
from urllib.parse import urlsplit
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def generate(origin:str):
    u=urlsplit(origin)
    if u.scheme!='https' or not u.hostname or u.username or u.password or u.query or u.fragment or u.path not in ('','/'):
        raise ValueError('Web origin must be an HTTPS hostname with no path/credentials')
    data=json.loads((ROOT/'infrastructure/keycloak/reelnest-realm.json').read_text())
    data.pop('users',None)
    data['verifyEmail']=True
    data['registrationAllowed']=False   # Explicitly enable only after working SMTP+verification
    data['bruteForceProtected']=True
    # Force OTP enrollment as an identity-provider enforced gate (covers admins too).
    # Deployments must verify this during Keycloak 26 realm import; do not rely on frontend-only MFA.
    data['requiredActions']=[{
        'alias':'CONFIGURE_TOTP','name':'Configure OTP','providerId':'CONFIGURE_TOTP',
        'enabled':True,'defaultAction':True,'priority':10
    }]
    data['otpPolicyType']='totp'
    data['otpPolicyDigits']=6
    data['otpPolicyPeriod']=30
    data['otpPolicyAlgorithm']='HmacSHA256'
    # Do not permit users to register until SMTP and email verification work.

    for client in data.get('clients',[]):
        if client.get('clientId')=='reelnest-web':
            client['redirectUris']=[f'{origin}/auth/callback']
            client['webOrigins']=[origin]
            client['attributes']['post.logout.redirect.uris']=origin
            client['directAccessGrantsEnabled']=False
            client['implicitFlowEnabled']=False
    return data

def main():
    p=argparse.ArgumentParser();p.add_argument('--web-origin',required=True)
    p.add_argument('--output',type=Path,required=True)
    args=p.parse_args();args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(generate(args.web_origin.rstrip('/')),indent=2)+'\n')
    print('Identity template generated. Configure SMTP, MFA, hostname and secret before staging. No demo accounts included.')
if __name__=='__main__':main()
