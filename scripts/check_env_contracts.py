#!/usr/bin/env python3
"""Static contract verification; does not claim running-container health."""
import json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def validate(root=ROOT):
    violations=[]
    realm=json.loads((root/'infrastructure/keycloak/reelnest-realm.json').read_text())
    web=next(c for c in realm['clients'] if c['clientId']=='reelnest-web')
    if not any(m.get('protocolMapper')=='oidc-audience-mapper' and m.get('config',{}).get('included.custom.audience')=='reelnest-api' and m.get('config',{}).get('access.token.claim')=='true' for m in web.get('protocolMappers',[])):
        violations.append('Local OIDC client does not emit reelnest-api audience')
    if web.get('directAccessGrantsEnabled') or web.get('implicitFlowEnabled') or web.get('attributes',{}).get('pkce.code.challenge.method') != 'S256':
        violations.append('OIDC public client must use PKCE and disable direct/implicit grant')
    cfg=(root/'apps/api/src/main/resources/application.yml').read_text()
    compose=(root/'compose.yaml').read_text()
    if 'JWT_REQUIRED_AUDIENCE: reelnest-api' not in compose or 'required-audience: ${JWT_REQUIRED_AUDIENCE:reelnest-api}' not in cfg:
        violations.append('Missing matching JWT audience contract')
    package=json.loads((root/'apps/web/package.json').read_text())
    version=package['dependencies']['next'].lstrip('^~')
    try: parts=tuple(int(x) for x in version.split('.')[:3])
    except ValueError: parts=(0,0,0)
    if parts < (16,4,1): violations.append('Next.js minimum version predates October 2026 patches')
    image=(root/'apps/web/Dockerfile').read_text()
    if 'npm ci' not in image or 'npm install' not in image: violations.append('Web image must use lockfile when present and bootstrap safely otherwise')
    if 'npm ci' not in (root/'.github/workflows/checks.yml').read_text(): violations.append('Web CI must support npm ci')
    stage=(root/'scripts/generate_keycloak_staging_realm.py').read_text()
    if 'CONFIGURE_TOTP' not in stage or "data.pop('users',None)" not in stage: violations.append('Staging realm missing OTP requirement or demo-user stripping')
    return violations

if __name__=='__main__':
    errors=validate()
    if errors:
        print('FAIL: '+'; '.join(errors));sys.exit(1)
    print('PASS: dependency, issuer/client, MFA template and Compose contracts (static only)')
