import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts'))
from check_env_contracts import validate
from generate_keycloak_staging_realm import generate

def test_contracts():
    assert validate(ROOT)==[]

def test_staging_realm_no_demo_users():
    template=generate('https://ott.example.com')
    assert 'users' not in template
    assert not template['registrationAllowed']
    assert template['verifyEmail']
    assert template['bruteForceProtected']
    assert any(x['alias']=='CONFIGURE_TOTP' and x['defaultAction'] for x in template['requiredActions'])
    assert all('localhost' not in u for c in template['clients'] for u in c.get('redirectUris',[]))

def test_staging_rejects_http():
    import pytest
    with pytest.raises(ValueError):generate('http://example.com')
    with pytest.raises(ValueError):generate('https://example.com@attacker.test')

def test_audience_java():
    result=subprocess.run(['bash','scripts/test_token_claims_java.sh'],cwd=ROOT,capture_output=True,text=True)
    assert result.returncode==0,result.stderr

def test_migrations_and_revocation_contract():
    versions={}
    migration_dir=ROOT/'apps/api/src/main/resources/db/migration'
    for sql in migration_dir.glob('V*__*.sql'):
        version=int(sql.name.split('__')[0][1:])
        assert version not in versions, f'duplicate Flyway version V{version}'
        versions[version]=sql.name
    assert set(range(1,16)).issubset(versions)
    revocation=(migration_dir/'V15__account_token_revocation.sql').read_text()
    assert 'token_valid_after TIMESTAMPTZ' in revocation
    assert "TokenClaimsPolicy.issuedAfterRevocation" in (ROOT/'apps/api/src/main/java/com/reelnest/users/CurrentUserService.java').read_text()
    controller=(ROOT/'apps/api/src/main/java/com/reelnest/users/AccountSessionsController.java').read_text()
    assert "WHERE id=? AND user_id=? AND status='ACTIVE'" in controller
    assert 'UPDATE users SET token_valid_after=NOW() WHERE id=?' in controller

def test_no_local_demo_users_in_staging():
    out=generate('https://app.staging.example.com')
    assert 'users' not in out
    assert out['clients'][0]['protocolMappers'][0]['config']['included.custom.audience']=='reelnest-api'

def test_security_headers_on_account_paths():
    headers=(ROOT/'apps/api/src/main/java/com/reelnest/security/RequestSecurityHeaders.java').read_text()
    assert 'path.startsWith("/api/v1/me")' in headers
    assert 'Cache-Control","no-store"' in headers
