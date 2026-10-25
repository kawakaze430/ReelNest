"""Exercise the upload acceptance action without requiring a live media stack.

These tests prove response scoping and failure propagation, not S3 processing or
browser media decode. The complete Playwright workflow remains a separate G1.
"""
import asyncio
import importlib.util
from pathlib import Path
from types import SimpleNamespace

import pytest


ROOT = Path(__file__).resolve().parents[2]
TITLE = 'a37fe46a-1111-4111-8111-b390e53b71e9'
UPLOAD = 'b37fe46a-2222-4222-8222-b390e53b71e9'
API = 'http://localhost:8080'
COMPLETE = f'{API}/api/v1/creator/titles/{TITLE}/uploads/{UPLOAD}/complete'
PUBLISH = f'{API}/api/v1/admin/titles/{TITLE}/publish'
TITLE_NAME = 'Original Worker Integration synthetic-fixture'


@pytest.fixture
def processing(monkeypatch):
    # The entrypoint only needs Playwright when executing main(). This allows
    # ordinary CI regression tests to exercise its actual upload action directly.
    monkeypatch.syspath_prepend(str(ROOT / 'scripts'))
    spec = importlib.util.spec_from_file_location('processing_contract', ROOT / 'scripts/e2e_processing_live.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Response:
    def __init__(self, body, *, url=COMPLETE, method='POST', status=200):
        self.url = url
        self.request = SimpleNamespace(method=method)
        self.status = status
        self.body = body

    async def json(self):
        return self.body


class UploadPage:
    """Only the video input, upload button and network events used by the action.

    There is deliberately no old 'Source uploaded.' status element. Waiting for
    that removed UI text fails instead of accidentally passing on a fake label.
    """
    def __init__(self, responses):
        self.responses = responses
        self.selected_file = None
        self.response_listener = None
        self.clicked = False

    def locator(self, selector):
        assert selector == 'input[type="file"][accept*="video/mp4"]'
        return self

    async def set_input_files(self, source):
        self.selected_file = source

    def get_by_role(self, role, *, name=None, exact=False):
        assert (role, name, exact) == ('button', 'Upload securely', True)
        return self

    def expect_response(self, predicate, *, timeout):
        assert timeout == 120000
        page = self

        class PendingResponse:
            async def __aenter__(self):
                self.value = asyncio.get_running_loop().create_future()
                page.response_listener = (predicate, self.value)
                return self

            async def __aexit__(self, exc_type, exc, traceback):
                if exc_type is None:
                    assert self.value.done(), 'No scoped upload completion was observed'
                page.response_listener = None

        return PendingResponse()

    async def click(self):
        assert self.selected_file and self.response_listener, 'Listen before the UI starts uploading'
        self.clicked = True
        predicate, future = self.response_listener
        for response in self.responses:
            if predicate(response):
                future.set_result(response)
                break


def original(tmp_path):
    source = tmp_path / 'original.mp4'
    source.write_bytes(b'original unit-test fixture')
    return source, {'status': 'COMPLETED', 'titleId': TITLE,
                    'filename': source.name, 'expectedBytes': source.stat().st_size}


def test_upload_uses_scoped_backend_completion_instead_of_removed_status_text(processing, tmp_path):
    source, body = original(tmp_path)
    page = UploadPage([
        Response(body, method='GET'),
        Response(body, url=COMPLETE.replace(TITLE, 'c37fe46a-3333-4333-8333-b390e53b71e9')),
        Response(body, url=COMPLETE.replace('http://localhost:8080', 'https://storage.invalid')),
        Response(body, url=COMPLETE + '?other=1'),
        Response(body, url=COMPLETE.replace('/complete', '/parts')),
        Response(body),
    ])
    asyncio.run(processing.upload_original(page, TITLE, source))
    assert page.clicked and page.selected_file == str(source)


def test_upload_http_error_is_not_replaced_by_confirmation_timeout(processing, tmp_path):
    source, body = original(tmp_path)
    with pytest.raises(AssertionError, match='Upload completion failed: HTTP 409'):
        asyncio.run(processing.upload_original(UploadPage([Response(body, status=409)]), TITLE, source))


@pytest.mark.parametrize('patch,reason', [
    ({'status': 'INITIATED'}, 'did not complete'),
    ({'titleId': UPLOAD}, 'another title'),
    ({'filename': 'another.mp4'}, 'another file'),
    ({'expectedBytes': 1}, 'byte count'),
])
def test_upload_rejects_incomplete_or_unrelated_backend_result(processing, tmp_path, patch, reason):
    source, body = original(tmp_path)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(processing.upload_original(UploadPage([Response(dict(body, **patch))]), TITLE, source))


def test_unrelated_response_cannot_mark_upload_complete(processing, tmp_path):
    source, body = original(tmp_path)
    with pytest.raises(AssertionError, match='No scoped upload completion'):
        asyncio.run(processing.upload_original(UploadPage([Response(body, method='GET')]), TITLE, source))


class CorsRequests:
    def __init__(self, *, allowed_status=200, allowed_patch=None, foreign_origin='', foreign_status=403):
        self.calls = []
        self.allowed_status = allowed_status
        self.allowed_headers = {'Access-Control-Allow-Origin': 'http://localhost:3000',
                                'Access-Control-Allow-Methods': 'GET, PUT, HEAD',
                                'Access-Control-Allow-Headers': 'Content-Type'}
        self.allowed_headers.update(allowed_patch or {})
        self.foreign_status, self.foreign_origin = foreign_status, foreign_origin

    async def fetch(self, url, **kwargs):
        assert url == 'http://localhost:9000/reelnest-source-private/original-cors-probe.mp4'
        assert kwargs['method'] == 'OPTIONS' and kwargs['max_redirects'] == 0
        assert kwargs['timeout'] == 15000
        assert set(kwargs['headers']) == {'Origin', 'Access-Control-Request-Method', 'Access-Control-Request-Headers'}
        assert kwargs['headers']['Access-Control-Request-Method'] == 'PUT'
        assert kwargs['headers']['Access-Control-Request-Headers'] == 'content-type'
        self.calls.append((url, kwargs))
        if kwargs['headers']['Origin'] == 'http://localhost:3000':
            return SimpleNamespace(status=self.allowed_status, headers=self.allowed_headers)
        assert kwargs['headers']['Origin'] == 'https://foreign.example.invalid'
        return SimpleNamespace(status=self.foreign_status,
                               headers={'Access-Control-Allow-Origin': self.foreign_origin})


@pytest.fixture
def disposable(monkeypatch):
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', 'true')
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', 'reelnest-test-cors-contract')


@pytest.mark.parametrize('status,accepted_headers,foreign_status', [(200, 'Content-Type', 403), (204, '*', 200)])
def test_local_cors_requires_real_preflight_headers_and_foreign_denial(processing, disposable, status, accepted_headers, foreign_status):
    request = CorsRequests(allowed_status=status, allowed_patch={'Access-Control-Allow-Headers': accepted_headers},
                           foreign_status=foreign_status)
    result = asyncio.run(processing.verify_local_upload_cors(request))
    assert result == {'allowedOriginStatus': status, 'foreignOriginStatus': foreign_status,
                      'exactDevelopmentOrigin': True, 'putAndContentTypeAllowed': True, 'foreignOriginDenied': True}
    assert len(request.calls) == 2


@pytest.mark.parametrize('options,reason', [
    ({'allowed_status': 400}, 'preflight failed: HTTP 400'),
    ({'allowed_patch': {'Access-Control-Allow-Origin': '*'}}, 'exact development origin'),
    ({'allowed_patch': {'Access-Control-Allow-Origin': 'https://foreign.example.invalid'}}, 'exact development origin'),
    ({'allowed_patch': {'Access-Control-Allow-Methods': 'GET, HEAD'}}, 'does not permit PUT'),
    ({'allowed_patch': {'Access-Control-Allow-Headers': 'authorization'}}, 'does not permit content-type'),
    ({'allowed_patch': {'Access-Control-Allow-Headers': ''}}, 'does not permit content-type'),
    ({'foreign_origin': '*'}, 'permits a foreign origin'),
    ({'foreign_origin': 'https://foreign.example.invalid'}, 'permits a foreign origin'),
])
def test_local_cors_permissive_or_missing_policy_is_not_accepted(processing, disposable, options, reason):
    request = CorsRequests(**options)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(processing.verify_local_upload_cors(request))


@pytest.mark.parametrize('confirmed,project', [('false', 'reelnest-test-cors'), ('true', 'shared-stack')])
def test_local_cors_refuses_unconfirmed_stack_before_any_request(processing, monkeypatch, confirmed, project):
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', confirmed)
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', project)
    request = CorsRequests()
    with pytest.raises(RuntimeError, match='BLOCKED'):
        asyncio.run(processing.verify_local_upload_cors(request))
    assert not request.calls


class PublicationPage:
    def __init__(self, responses, *, catalog_status=200, catalog_patch=None, ui_success=True):
        self.responses = responses
        self.catalog_status = catalog_status
        self.catalog_title = {'id': TITLE, 'name': TITLE_NAME, **(catalog_patch or {})}
        self.ui_success = ui_success
        self.response_listener = None
        self.clicked = False
        self.confirmed = False
        self.request = self
        self.catalog_calls = []

    def expect_response(self, predicate, *, timeout):
        assert timeout == 20000
        page = self
        class PendingResponse:
            async def __aenter__(self):
                self.value = asyncio.get_running_loop().create_future()
                page.response_listener = (predicate, self.value)
                return self
            async def __aexit__(self, exc_type, exc, traceback):
                if exc_type is None:
                    assert self.value.done(), 'No scoped title publication response was observed'
                page.response_listener = None
        return PendingResponse()

    def get_by_role(self, role, *, name=None, exact=False):
        assert (role, name, exact) in [('button', 'Attempt publish', True), ('status', None, False)]
        return self

    async def click(self):
        assert self.response_listener, 'Listen before the console publishes'
        self.clicked = True
        predicate, future = self.response_listener
        for response in self.responses:
            if predicate(response):
                future.set_result(response)
                break

    def get_by_text(self, text):
        assert text == 'Title published.'
        return self

    async def wait_for(self, *, timeout):
        assert timeout == 20000
        assert self.ui_success, 'The moderator console did not confirm publication'
        self.confirmed = True

    async def get(self, url):
        assert self.confirmed, 'UI confirmation remains required before catalog verification'
        assert url == f'{API}/api/v1/catalog/{TITLE}'
        self.catalog_calls.append(url)
        return Response(self.catalog_title, status=self.catalog_status)


def test_publication_requires_scoped_no_content_response_ui_and_matching_public_title(processing, disposable):
    page = PublicationPage([
        Response({}, url=PUBLISH, method='GET', status=204),
        Response({}, url=PUBLISH.replace(TITLE, UPLOAD), status=204),
        Response({}, url=PUBLISH.replace(API, 'https://outside.invalid'), status=204),
        Response({}, url=PUBLISH + '?other=1', status=204),
        Response({}, url=PUBLISH, status=204),
    ])
    result = asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))
    assert page.clicked and page.confirmed and len(page.catalog_calls) == 1
    assert result == {'publishStatus': 204, 'catalogStatus': 200, 'titleId': TITLE, 'publishedTitleVerified': True}


@pytest.mark.parametrize('status', [200, 401, 403, 409, 500])
def test_publication_http_failure_does_not_become_a_ui_timeout(processing, disposable, status):
    page = PublicationPage([Response({}, url=PUBLISH, status=status)])
    with pytest.raises(AssertionError, match=f'Title publication failed: HTTP {status}'):
        asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))
    assert not page.confirmed and not page.catalog_calls


def test_publication_cannot_pass_on_backend_response_when_console_confirmation_is_missing(processing, disposable):
    page = PublicationPage([Response({}, url=PUBLISH, status=204)], ui_success=False)
    with pytest.raises(AssertionError, match='moderator console did not confirm'):
        asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))
    assert not page.catalog_calls


@pytest.mark.parametrize('options,reason', [
    ({'catalog_status': 404}, 'unavailable in public catalog: HTTP 404'),
    ({'catalog_patch': {'id': UPLOAD}}, 'belongs to another title'),
    ({'catalog_patch': {'name': 'Another film'}}, 'belongs to another title'),
])
def test_publication_requires_matching_eligible_public_catalog_state(processing, disposable, options, reason):
    page = PublicationPage([Response({}, url=PUBLISH, status=204)], **options)
    with pytest.raises(AssertionError, match=reason):
        asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))


def test_unrelated_response_cannot_mark_publication_complete(processing, disposable):
    page = PublicationPage([Response({}, url=PUBLISH.replace(TITLE, UPLOAD), status=204)])
    with pytest.raises(AssertionError, match='No scoped title publication response'):
        asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))


@pytest.mark.parametrize('confirmed,project', [('false', 'reelnest-test-publication'), ('true', 'shared-stack')])
def test_publication_refuses_unconfirmed_stack_before_ui_action(processing, monkeypatch, confirmed, project):
    monkeypatch.setenv('REELNEST_DISPOSABLE_STACK', confirmed)
    monkeypatch.setenv('COMPOSE_PROJECT_NAME', project)
    page = PublicationPage([Response({}, url=PUBLISH, status=204)])
    with pytest.raises(RuntimeError, match='BLOCKED'):
        asyncio.run(processing.publish_original(page, page, TITLE, TITLE_NAME))
    assert not page.clicked and not page.catalog_calls
