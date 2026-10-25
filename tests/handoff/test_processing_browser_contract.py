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
