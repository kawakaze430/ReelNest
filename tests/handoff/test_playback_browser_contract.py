"""Behavior checks for G1 caption, independent-viewer and traversal assertions.

Requests below are controlled fakes; these tests do not establish actual MinIO,
encoded-media delivery or browser decode acceptance.
"""
import asyncio
import importlib.util
from pathlib import Path
import re
from types import SimpleNamespace

import pytest


ROOT = Path(__file__).resolve().parents[2]
API = 'http://localhost:8080'
TITLE = 'a37fe46a-1111-4111-8111-b390e53b71e9'
SESSION_ID = 'b37fe46a-2222-4222-8222-b390e53b71e9'
PROFILE = 'c37fe46a-3333-4333-8333-b390e53b71e9'
SESSION = {'sessionId': SESSION_ID,
           'manifestUrl': f'/api/v1/playback/sessions/{SESSION_ID}/files/master.m3u8'}
MANIFEST = API + SESSION['manifestUrl']
BASE = MANIFEST.removesuffix('master.m3u8')
CAPTION_PLAYLIST = BASE + 'captions/en/index.m3u8'
CAPTION_SEGMENT = BASE + 'captions/en/captions.vtt'
HEADERS = {'Authorization': 'Bearer unit-viewer'}
OTHER_HEADERS = {'Authorization': 'Bearer unit-other'}
MASTER = '\n'.join(('#EXTM3U',
    '#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Original, English",LANGUAGE="en",URI="captions/en/index.m3u8"',
    '#EXT-X-STREAM-INF:BANDWIDTH=800000,SUBTITLES="subs"', '480p/index.m3u8', ''))
VTT = 'WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nOriginal ReelNest integration fixture.\n'
TS = b'\x47' + bytes(187)


@pytest.fixture
def playback(monkeypatch):
    monkeypatch.syspath_prepend(str(ROOT / 'scripts'))
    spec = importlib.util.spec_from_file_location('playback_contract', ROOT / 'scripts/e2e_playback_live.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Response:
    def __init__(self, status=200, *, data=None, text='', body=b''):
        self.status, self.data, self.content, self.payload = status, data, text, body

    async def json(self):
        return self.data

    async def text(self):
        return self.content

    async def body(self):
        return self.payload


class Requests:
    def __init__(self, routes):
        self.routes, self.calls = routes, []

    def invoke(self, method, url, kwargs):
        # A safety assertion in the fake catches any unguarded remote credential
        # request before it could be represented as a successful test response.
        assert url.startswith(API + '/api/v1/'), 'Unexpected nonlocal request'
        self.calls.append((method, url, kwargs))
        auth = kwargs.get('headers', {}).get('Authorization')
        return self.routes[(method, url, auth)]

    async def get(self, url, **kwargs):
        return self.invoke('GET', url, kwargs)

    async def post(self, url, **kwargs):
        return self.invoke('POST', url, kwargs)

    async def delete(self, url, **kwargs):
        return self.invoke('DELETE', url, kwargs)


def page(routes):
    return SimpleNamespace(request=Requests(routes))


def caption_pages(*, vtt=VTT, anonymous=401, cross_account=404):
    routes, other_routes = {}, {}
    routes[('GET', CAPTION_PLAYLIST, HEADERS['Authorization'])] = Response(text='#EXTM3U\n#EXTINF:8.0,\ncaptions.vtt\n')
    routes[('GET', CAPTION_SEGMENT, HEADERS['Authorization'])] = Response(text=vtt)
    for url in (CAPTION_PLAYLIST, CAPTION_SEGMENT):
        routes[('GET', url, None)] = Response(anonymous)
        other_routes[('GET', url, OTHER_HEADERS['Authorization'])] = Response(cross_account)
    return page(routes), page(other_routes)


def test_subtitle_attributes_follow_quoted_uri_and_allow_comma_in_name(playback):
    assert playback.subtitle_playlists(MANIFEST, MASTER) == [('en', CAPTION_PLAYLIST)]


@pytest.mark.parametrize('uri', ['https://outside.invalid/track.m3u8', '//outside.invalid/track.m3u8',
    '%2e%2e/%2e%2e/private.m3u8', 'captions/en/index.m3u8?token=unit', 'captions%5cen/index.m3u8'])
def test_subtitle_uri_cannot_escape_authorized_local_session(playback, uri):
    with pytest.raises(ValueError):
        playback.subtitle_playlists(MANIFEST, MASTER.replace('captions/en/index.m3u8', uri))


@pytest.mark.parametrize('text', ['#EXTM3U\n480p/index.m3u8\n', MASTER.replace('LANGUAGE="en",', ''),
    MASTER.replace('URI="captions/en/index.m3u8"', 'URI="captions/en/index.m3u8",URI="other.m3u8"')])
def test_missing_or_duplicate_subtitle_attributes_do_not_pass(playback, text):
    with pytest.raises(AssertionError):
        playback.subtitle_playlists(MANIFEST, text)


def test_real_caption_text_and_denials_are_required_for_playlist_and_vtt(playback):
    viewer, other = caption_pages()
    result = asyncio.run(playback.verify_captions(viewer, other, MANIFEST, MASTER, HEADERS, OTHER_HEADERS))
    assert result['originalCueVerified'] and result['segmentsFetched'] == 1
    assert result['denials'] == [
        {'resource': 'captions/en/index.m3u8', 'anonymousStatus': 401, 'crossAccountStatus': 404},
        {'resource': 'captions/en/captions.vtt', 'anonymousStatus': 401, 'crossAccountStatus': 404},
    ]


@pytest.mark.parametrize('vtt,reason', [
    ('<html>not captions</html>', 'actual timed WebVTT'),
    (VTT.replace('Original ReelNest integration fixture.', 'Unrelated caption.'), 'uploaded original caption cue'),
])
def test_subtitle_metadata_alone_cannot_prove_uploaded_caption_delivery(playback, vtt, reason):
    viewer, other = caption_pages(vtt=vtt)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(playback.verify_captions(viewer, other, MANIFEST, MASTER, HEADERS, OTHER_HEADERS))


@pytest.mark.parametrize('anonymous,cross_account,reason', [
    (200, 404, 'Anonymous private caption'), (401, 200, 'Cross-account private caption'),
])
def test_public_or_cross_account_caption_access_is_a_failure(playback, anonymous, cross_account, reason):
    viewer, other = caption_pages(anonymous=anonymous, cross_account=cross_account)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(playback.verify_captions(viewer, other, MANIFEST, MASTER, HEADERS, OTHER_HEADERS))


def independent_page(*, tier='FREE', segment_status=200, segment_bytes=TS):
    auth = OTHER_HEADERS['Authorization']
    return page({
        ('GET', API + '/api/v1/catalog/' + TITLE, auth): Response(data={'accessTier': tier}),
        ('POST', API + '/api/v1/playback/sessions', auth): Response(201, data=SESSION),
        ('GET', MANIFEST, auth): Response(text=MASTER),
        ('GET', BASE + '480p/index.m3u8', auth): Response(text='#EXTM3U\nseg_00001.ts\n'),
        ('GET', BASE + '480p/seg_00001.ts', auth): Response(segment_status, body=segment_bytes),
        ('DELETE', API + '/api/v1/playback/sessions/' + SESSION_ID, auth): Response(204),
    })


def test_second_viewer_uses_own_profile_gets_video_bytes_and_ends_session(playback):
    viewer = independent_page()
    result = asyncio.run(playback.verify_independent_playback(viewer, OTHER_HEADERS, PROFILE, TITLE))
    assert result == {'ownProfilePlayback': True, 'segmentBytesFetched': len(TS)}
    start = next(call for call in viewer.request.calls if call[0] == 'POST')
    assert start[2]['data'] == {'profileId': PROFILE, 'titleId': TITLE}
    assert viewer.request.calls[-1][:2] == ('DELETE', API + '/api/v1/playback/sessions/' + SESSION_ID)


def test_free_fixture_metadata_matches_actual_catalog_controller_contract(playback):
    # Cross-check the mocked HTTP contract against the actual Java route instead
    # of letting the test and Python client agree on a nonexistent endpoint.
    controller = (ROOT / 'apps/api/src/main/java/com/reelnest/catalog/CatalogController.java').read_text()
    response = (ROOT / 'apps/api/src/main/java/com/reelnest/catalog/TitleResponse.java').read_text()
    base = re.search(r'@RequestMapping\("([^"]+)"\)', controller)
    detail = re.search(r'@GetMapping\("([^"]+)"\)\s+public TitleResponse get\(', controller)
    assert base and detail and re.search(r'\bString accessTier\b', response)
    expected = API + base[1] + detail[1].replace('{id}', TITLE)
    viewer = independent_page()
    asyncio.run(playback.verify_independent_playback(viewer, OTHER_HEADERS, PROFILE, TITLE))
    assert viewer.request.calls[0][:2] == ('GET', expected)


def test_second_viewer_never_invokes_payment_for_a_nonfree_fixture(playback):
    viewer = independent_page(tier='PREMIUM')
    with pytest.raises(AssertionError, match='FREE integration fixture'):
        asyncio.run(playback.verify_independent_playback(viewer, OTHER_HEADERS, PROFILE, TITLE))
    assert [method for method, _, _ in viewer.request.calls] == ['GET']


@pytest.mark.parametrize('status,payload,reason', [(404, TS, 'segment unavailable'),
    (200, b'<html>not media</html>', 'MPEG-TS bytes')])
def test_independent_viewer_failure_still_ends_its_session(playback, status, payload, reason):
    viewer = independent_page(segment_status=status, segment_bytes=payload)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(playback.verify_independent_playback(viewer, OTHER_HEADERS, PROFILE, TITLE))
    assert viewer.request.calls[-1][0] == 'DELETE'


@pytest.mark.parametrize('manifest', ['https://outside.invalid/master.m3u8',
    '/api/v1/playback/sessions/a37fe46a-1111-4111-8111-b390e53b71e9/files/master.m3u8'])
def test_manifest_must_match_validated_local_session_before_requests(playback, manifest):
    with pytest.raises(AssertionError, match='current local playback session'):
        playback.local_manifest(dict(SESSION, manifestUrl=manifest))


def traversal_page(status=400):
    class ProbeRequests:
        def __init__(self):
            self.calls = []

        async def get(self, url, **kwargs):
            assert url.startswith(BASE), 'Traversal probe left the fixed local session path'
            assert kwargs['headers'] == HEADERS
            self.calls.append((url, kwargs))
            if url == MANIFEST:
                return Response(200)
            assert kwargs['max_redirects'] == 0, 'Negative probes must never follow redirects'
            return Response(status)

    return SimpleNamespace(request=ProbeRequests())


def test_encoded_traversal_rejections_record_real_response_codes(playback):
    viewer = traversal_page()
    recorded = []
    result = asyncio.run(playback.verify_traversal_denials(viewer, HEADERS, SESSION, record=recorded.append))
    assert len(result) == 4 and all(item['httpStatus'] == 400 for item in result)
    assert len(recorded) == 4 and recorded[-1] == result
    assert viewer.request.calls[-1][0] == MANIFEST, 'Legitimate media must still work after negative probes'


@pytest.mark.parametrize('status', [200, 302, 401, 500])
def test_traversal_success_redirect_auth_failure_or_server_error_cannot_pass(playback, status):
    viewer = traversal_page(status)
    recorded = []
    with pytest.raises(AssertionError, match=f'not denied: HTTP {status}'):
        asyncio.run(playback.verify_traversal_denials(viewer, HEADERS, SESSION, record=recorded.append))
    assert recorded[-1][0]['httpStatus'] == status, 'Preserve the actual failed probe observation'
    assert len(viewer.request.calls) == 1
