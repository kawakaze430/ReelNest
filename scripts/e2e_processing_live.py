#!/usr/bin/env python3
"""Real browser creator/upload/worker/rights publication on an opted-in disposable stack."""
import asyncio
from datetime import datetime, timedelta, timezone
import hashlib
import json
import os
import re
import subprocess
import uuid

from live_e2e_helpers import API, FIXTURE, ROOT, WEB, chromium_launch_options, login, require_disposable_stack


async def verify_local_upload_cors(request):
    """Check the developer storage server's actual browser preflight policy.

    OPTIONS never uploads an object. The fixed loopback destination must not be
    replaced with a fixture-controlled URL or production storage endpoint.
    """
    require_disposable_stack()
    url = 'http://localhost:9000/reelnest-source-private/original-cors-probe.mp4'
    origin = 'http://localhost:3000'
    headers = {'Origin': origin, 'Access-Control-Request-Method': 'PUT',
               'Access-Control-Request-Headers': 'content-type'}
    allowed = await request.fetch(url, method='OPTIONS', headers=headers,
                                  max_redirects=0, timeout=15000)
    assert allowed.status in (200, 204), f'Local upload CORS preflight failed: HTTP {allowed.status}'
    policy = {name.lower(): value for name, value in allowed.headers.items()}
    assert policy.get('access-control-allow-origin') == origin, 'Local upload CORS must allow the exact development origin'
    methods = {item.strip().upper() for item in policy.get('access-control-allow-methods', '').split(',')}
    assert 'PUT' in methods, 'Local upload CORS does not permit PUT'
    accepted_headers = {item.strip().lower() for item in policy.get('access-control-allow-headers', '').split(',')}
    assert accepted_headers.intersection({'content-type', '*'}), 'Local upload CORS does not permit content-type'

    foreign = 'https://foreign.example.invalid'
    denied = await request.fetch(url, method='OPTIONS', headers={**headers, 'Origin': foreign},
                                 max_redirects=0, timeout=15000)
    foreign_policy = {name.lower(): value for name, value in denied.headers.items()}
    granted = {item.strip() for item in foreign_policy.get('access-control-allow-origin', '').split(',')}
    assert not granted.intersection({foreign, '*'}), 'Local upload CORS permits a foreign origin'
    return {'allowedOriginStatus': allowed.status, 'foreignOriginStatus': denied.status,
            'exactDevelopmentOrigin': True, 'putAndContentTypeAllowed': True, 'foreignOriginDenied': True}


async def upload_original(creator, title_id, source):
    """Drive the shipped upload form and require its scoped backend completion.

    UI confirmation wording can change without changing the multipart contract.
    A completed request must still belong to this title and original file before
    the test proceeds to independently verify the worker's READY state.
    """
    await creator.locator('input[type="file"][accept*="video/mp4"]').set_input_files(str(source))
    complete_url = re.compile(re.escape(API + f'/api/v1/creator/titles/{title_id}/uploads/')
                              + r'[0-9a-fA-F-]{36}/complete')
    async with creator.expect_response(
            lambda response: response.request.method == 'POST'
            and complete_url.fullmatch(response.url) is not None,
            timeout=120000) as completed:
        await creator.get_by_role('button', name='Upload securely', exact=True).click()
    response = await completed.value
    assert response.status == 200, f'Upload completion failed: HTTP {response.status}'
    upload = await response.json()
    assert upload.get('status') == 'COMPLETED', 'Upload did not complete in the backend'
    assert upload.get('titleId') == title_id, 'Upload completion belongs to another title'
    assert upload.get('filename') == source.name, 'Upload completion belongs to another file'
    assert upload.get('expectedBytes') == source.stat().st_size, 'Upload completion byte count differs from the original'


async def main():
    from playwright.async_api import async_playwright

    require_disposable_stack()
    source = ROOT / 'apps/web/public/sample/demo.mp4'
    assert source.is_file() and source.stat().st_size > 0, 'Original generated sample is missing'
    title_name = 'Original Worker Integration ' + uuid.uuid4().hex[:12]
    fixture = {'status': 'RUNNING', 'titleName': title_name,
               'project': os.environ['COMPOSE_PROJECT_NAME'],
               'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
               'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest()}
    FIXTURE.parent.mkdir(exist_ok=True)
    FIXTURE.write_text(json.dumps(fixture, indent=2) + '\n')
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(**chromium_launch_options())
        try:
            creator = await browser.new_page()
            fixture['cors'] = await verify_local_upload_cors(creator.request)
            FIXTURE.write_text(json.dumps(fixture, indent=2) + '\n')
            creator_identity = await login(creator, 'demo', 'DemoOnly123!', '/creator', 'Sign in to Creator Studio')
            headers = {'Authorization': 'Bearer ' + creator_identity['access_token']}
            await creator.goto(WEB + '/creator')
            current = await creator.request.get(API + '/api/v1/creators/me', headers=headers)
            if current.status == 404:
                await creator.get_by_label('Display name', exact=True).fill('Integration Test Film Studio')
                await creator.get_by_label('Legal name', exact=True).fill('Test Studio')
                await creator.get_by_label('Contact email', exact=True).fill('integration@example.invalid')
                await creator.get_by_label('Describe your work and rights to distribute it').fill(
                    'A programmatically generated original test video; no third-party content is distributed.')
                await creator.get_by_role('button', name='Submit application').click()
                await creator.get_by_text('PENDING', exact=True).wait_for()
                current = await creator.request.get(API + '/api/v1/creators/me', headers=headers)
            assert current.status == 200, f'Creator lookup failed: HTTP {current.status}'
            application = await current.json()

            reviewer = await browser.new_page()
            await login(reviewer, 'reviewer', 'LocalReviewer123!', '/admin', 'Sign in as reviewer')
            await reviewer.goto(WEB + '/admin')
            if application['status'] == 'PENDING':
                card = reviewer.locator('article.review-card').filter(
                    has=reviewer.get_by_role('heading', name=application['displayName'], exact=True))
                await card.get_by_role('button', name='Approve', exact=True).click()
                await reviewer.get_by_role('status').get_by_text('Creator approved.').wait_for()
            else:
                assert application['status'] == 'APPROVED', 'Use a fresh disposable stack for a rejected creator'

            await creator.reload()
            await creator.get_by_label('Film title', exact=True).fill(title_name)
            await creator.get_by_label('Synopsis', exact=True).fill('Original generated film used solely for local software testing.')
            await creator.get_by_label('Duration (minutes)', exact=True).fill('1')
            async with creator.expect_response(lambda r: r.url == API + '/api/v1/creator/titles' and r.request.method == 'POST') as created:
                await creator.get_by_role('button', name='Create draft', exact=True).click()
            response = await created.value
            assert response.status == 201, f'Draft creation failed: HTTP {response.status}'
            draft = await response.json()
            fixture['titleId'] = draft['id']
            FIXTURE.write_text(json.dumps(fixture, indent=2) + '\n')
            await creator.get_by_role('status').get_by_text('Draft created.').wait_for()
            today = datetime.now(timezone.utc).date()
            await creator.get_by_label('Valid from', exact=True).fill(str(today))
            await creator.get_by_label('Valid until', exact=True).fill(str(today + timedelta(days=365)))
            await creator.get_by_label('Rights evidence reference', exact=True).fill('Repository-owned synthetic media: scripts/generate_demo_media.sh')
            await creator.get_by_label('Agreement notes', exact=True).fill(
                'Created programmatically for internal application validation only; no third-party film is involved.')
            await creator.get_by_role('button', name='Save rights', exact=True).click()
            await creator.get_by_role('status').get_by_text('Rights declaration saved.').wait_for()
            captions = FIXTURE.parent / 'original-caption.local.srt'
            captions.write_text('1\n00:00:00,000 --> 00:00:01,000\nOriginal ReelNest integration fixture.\n')
            await creator.get_by_label('SRT subtitle file', exact=True).set_input_files(str(captions))
            await creator.get_by_role('button', name='Save subtitle track', exact=True).click()
            await creator.get_by_role('status').get_by_text('Caption source saved.').wait_for()
            await upload_original(creator, draft['id'], source)
            for _ in range(120):
                media_response = await creator.request.get(API + f'/api/v1/creator/titles/{draft["id"]}/media', headers=headers)
                assert media_response.status == 200, f'Media status failed: HTTP {media_response.status}'
                media = await media_response.json()
                if any(item['processing_status'] == 'READY' and item.get('duration_seconds', 0) > 0 for item in media):
                    break
                assert not any(item.get('job_state') == 'FAILED' for item in media), 'Worker job failed; inspect private worker diagnostics'
                await asyncio.sleep(3)
            else:
                raise AssertionError('Worker did not reach READY within six minutes')
            await creator.get_by_role('button', name='Refresh status', exact=True).click()
            await creator.locator('.studio-list-item strong').get_by_text('READY', exact=True).wait_for()
            await creator.get_by_role('button', name='Submit film for review').click()
            await creator.get_by_role('status').get_by_text('Sent to administrator review.').wait_for()
            await reviewer.reload()
            film = reviewer.locator('article.review-card').filter(
                has=reviewer.get_by_role('heading', name=title_name, exact=True))
            await film.get_by_role('button', name='Inspect rights', exact=True).click()
            await film.get_by_label('Review note', exact=True).fill('Verified repository-owned original generated test media and rights declaration.')
            await film.get_by_role('button', name='Approve rights', exact=True).click()
            await reviewer.get_by_role('status').get_by_text('Rights approved.').wait_for()
            await film.get_by_role('button', name='Attempt publish', exact=True).click()
            await reviewer.get_by_role('status').get_by_text('Title published.').wait_for(timeout=20000)
            for bucket in ('reelnest-source-private', 'reelnest-processed-private'):
                result = await creator.request.get('http://localhost:9000/' + bucket + '/')
                assert result.status in (401, 403), 'Private media bucket permitted anonymous access'
            fixture['status'] = 'PASS'
            FIXTURE.write_text(json.dumps(fixture, indent=2) + '\n')
        except BaseException:
            fixture['status'] = 'FAIL'
            FIXTURE.write_text(json.dumps(fixture, indent=2) + '\n')
            raise
        finally:
            await browser.close()
    print('PASS: creator browser login -> scoped approval -> original upload + captions -> worker READY -> rights review -> publish; storage stays private')


if __name__ == '__main__':
    asyncio.run(main())
