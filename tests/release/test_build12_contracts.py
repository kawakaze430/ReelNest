from pathlib import Path
import subprocess
import importlib.util
import sys
import yaml
ROOT=Path(__file__).resolve().parents[2]

spec=importlib.util.spec_from_file_location('keycloak_setup',ROOT/'scripts/generate_keycloak_staging_realm.py')
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def test_secure_staging_realm():
    realm=module.generate('https://app.example.com')
    assert not realm.get('users')
    assert realm['verifyEmail'] is True
    assert realm['registrationAllowed'] is False
    client=next(x for x in realm['clients'] if x['clientId']=='reelnest-web')
    assert client['redirectUris']==['https://app.example.com/auth/callback']


def test_unsafe_identity_origins_rejected():
    for url in ['http://example.com','https://example.com/abc','https://u:p@example.com','https://example.com?q=1']:
        try:module.generate(url)
        except ValueError:pass
        else:raise AssertionError('Unsafe URL accepted: '+url)


def test_migration_and_tombstone_protection():
    db=(ROOT/'apps/api/src/main/resources/db/migration/V9__verified_erasure.sql').read_text()
    assert 'ON DELETE SET NULL' in db and 'identity_tombstones' in db
    current=(ROOT/'apps/api/src/main/java/com/reelnest/users/CurrentUserService.java').read_text()
    assert 'IdentityTombstone.digest' in current
    assert 'Account erased; cannot recreate' in current
    erasure=(ROOT/'apps/api/src/main/java/com/reelnest/privacy/PrivacyErasureService.java').read_text()
    assert 'client_credentials' in erasure and 'ERASURE_LOCKED' in erasure
    assert 'DELETE FROM users' in erasure
    assert 'SELECT COUNT(*) FROM subscriptions' in erasure


def test_ml_service_is_private_and_optional():
    compose=yaml.safe_load((ROOT/'compose.yaml').read_text())
    assert 'ports' not in compose['services']['recommendation-api']
    assert compose['services']['api']['environment']['RECOMMENDATION_ENABLED']=='true'
    assert (ROOT/'apps/recommendation-api/Dockerfile').exists()
    j=(ROOT/'apps/api/src/main/java/com/reelnest/discovery/PrivateRecommendationClient.java').read_text()
    assert 'allowed.containsKey(item.id())' in j
    assert 'eligible.stream().limit(limit)' in j


def test_player_does_not_native_play_local_proxy():
    source=(ROOT/'apps/web/src/components/protected-player.tsx').read_text()
    assert "isCdn && video.canPlayType" in source
    assert "video.crossOrigin='use-credentials'" in source
    assert 'tabIndex={0}' in source
