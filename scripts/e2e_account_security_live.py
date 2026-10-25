#!/usr/bin/env python3
"""Real PKCE/API/PostgreSQL/Redis account regressions on a disposable stack.

Exercises same-account one-time handoff and PIN-protected profile deletion.
Creates and removes only this run's profiles; no media or payment claim.
"""
import asyncio
import json
import os
import subprocess
import uuid

from cloud_evidence import source_state
from live_e2e_helpers import API, ROOT, chromium_launch_options, login, require_disposable_stack


def pin_failures(profile_id):
    """Read committed state, rather than inferring persistence from a 403."""
    profile_id = uuid.UUID(str(profile_id))
    result = subprocess.run(
        ['docker', 'compose', 'exec', '-T', 'postgres', 'psql', '-v', 'ON_ERROR_STOP=1',
         '-U', 'reelnest', '-d', 'reelnest', '-Atc',
         f"SELECT pin_failed_attempts FROM profiles WHERE id='{profile_id}'"],
        cwd=ROOT, capture_output=True, text=True, check=True, timeout=20,
    )
    return int(result.stdout.strip())


async def main():
    require_disposable_stack()
    from playwright.async_api import async_playwright

    output = ROOT / 'build-evidence/account-security-latest.json'
    output.parent.mkdir(exist_ok=True)
    report = {
        'status': 'RUNNING',
        'scope': 'Real account/PIN/handoff component checks; no encoded-media or financial claim',
        'project': os.environ['COMPOSE_PROJECT_NAME'],
        'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'sourceAtStart': source_state(),
        'checks': {},
    }
    owned = {}
    owner_page = None
    owner_headers = None
    output.write_text(json.dumps(report, indent=2) + '\n')
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(**chromium_launch_options())
        try:
            owner_page = await browser.new_page()
            owner = await login(owner_page, 'demo', 'DemoOnly123!')
            stranger_page = await browser.new_page()
            stranger = await login(stranger_page, 'reviewer', 'LocalReviewer123!')
            assert owner['profile']['sub'] != stranger['profile']['sub'], 'Two independent identities required'
            owner_headers = {'Authorization': 'Bearer ' + owner['access_token']}
            stranger_headers = {'Authorization': 'Bearer ' + stranger['access_token']}

            async def call(method, path, *, status, foreign=False, data=None, headers=None):
                page = stranger_page if foreign else owner_page
                auth = stranger_headers if foreign else owner_headers
                response = await page.request.fetch(API + path, method=method,
                    headers=auth | (headers or {}), data=data, timeout=20000)
                assert response.status == status, f'{method} {path.split("?")[0]}: expected {status}, got {response.status}'
                return response

            profiles_path = '/api/v1/profiles'
            current = await call('GET', profiles_path, status=200)
            assert len(await current.json()) <= 3, 'BLOCKED: use a fresh disposable database with room for two test profiles'
            for child in (False, True):
                response = await call('POST', profiles_path, status=201, data={
                    'name': 'Security fixture ' + uuid.uuid4().hex[:10],
                    'preferredLanguage': 'en', 'child': child,
                })
                profile_id = str(uuid.UUID((await response.json())['id']))
                owned[profile_id] = None
                if child:
                    child_id = profile_id
                else:
                    adult_id = profile_id

            # A foreign account cannot inspect/change a PIN or delete this profile.
            pin_path = profiles_path + '/' + adult_id + '/pin'
            await call('GET', pin_path, status=404, foreign=True)
            await call('PUT', pin_path, status=404, foreign=True, data={'newPin': '287594'})
            await call('DELETE', profiles_path + '/' + adult_id, status=404, foreign=True)
            await call('PUT', profiles_path + '/' + child_id + '/pin', status=400, data={'newPin': '287594'})
            report['checks']['profileAndPinOwnership'] = True
            report['checks']['childAdultPinDenied'] = True

            # Wrong-account redemption does not consume the owner's invitation.
            handoff_path = '/api/v1/device-handoff'
            await call('POST', handoff_path, status=404, foreign=True, data={'profileId': adult_id})
            response = await call('POST', handoff_path, status=201, data={'profileId': adult_id})
            assert 'no-store' in response.headers.get('cache-control', ''), 'Invitation must not be cached'
            code = (await response.json())['code']
            await call('POST', handoff_path + '/redeem', status=404, foreign=True, data={'code': code})
            redeemed = await call('POST', handoff_path + '/redeem', status=200, data={'code': code})
            selection = await redeemed.json()
            assert set(selection) == {'profileId', 'profileName'} and selection['profileId'] == adult_id
            assert 'no-store' in redeemed.headers.get('cache-control', ''), 'Selection must not be cached'
            await call('POST', handoff_path + '/redeem', status=404, data={'code': code})
            await call('POST', handoff_path + '/redeem', status=400, data={'code': 'invalid'})
            report['checks']['wrongAccountDoesNotConsumeHandoff'] = True
            report['checks']['handoffNoCredentialsAndNoCache'] = True
            report['checks']['handoffReplayAndMalformedCodeDenied'] = True

            response = await call('POST', handoff_path, status=201, data={'profileId': adult_id})
            code = (await response.json())['code']
            raced = await asyncio.gather(*(
                owner_page.request.post(API + handoff_path + '/redeem', headers=owner_headers, data={'code': code})
                for _ in range(2)
            ))
            assert sorted(response.status for response in raced) == [200, 404], 'Concurrent handoff was not consumed exactly once'
            report['checks']['concurrentHandoffExactlyOnce'] = True

            response = await call('POST', handoff_path, status=201, data={'profileId': child_id})
            orphan_code = (await response.json())['code']
            await call('DELETE', profiles_path + '/' + child_id, status=204)
            owned.pop(child_id)
            await call('POST', handoff_path + '/redeem', status=404, data={'code': orphan_code})
            report['checks']['deletedProfileHandoffDenied'] = True

            # PostgreSQL reads prove denied requests committed their attempt counters.
            await call('PUT', pin_path, status=400, data={'newPin': '111111'})
            owned[adult_id] = '287594'
            await call('PUT', pin_path, status=200, data={'newPin': '287594'})
            assert pin_failures(adult_id) == 0
            await call('POST', pin_path + '/check', status=403, data={'pin': '583729'})
            await call('DELETE', profiles_path + '/' + adult_id, status=403,
                       headers={'X-Profile-Pin': '583729'})
            assert pin_failures(adult_id) == 2, 'Denied PIN operations rolled back their failure counters'
            current = await call('GET', profiles_path, status=200)
            assert any(row['id'] == adult_id for row in await current.json()), 'Wrong PIN deleted the profile'
            await call('POST', pin_path + '/check', status=200, data={'pin': '287594'})
            assert pin_failures(adult_id) == 0, 'Correct PIN did not reset the persisted failure counter'
            await call('DELETE', profiles_path + '/' + adult_id, status=204,
                       headers={'X-Profile-Pin': '287594'})
            owned.pop(adult_id)
            current = await call('GET', profiles_path, status=200)
            assert all(row['id'] != adult_id for row in await current.json()), 'Authorized deletion did not persist'
            report['checks']['deniedPinAttemptsPersist'] = True
            report['checks']['successfulPinResetsAttempts'] = True
            report['checks']['pinProtectedDeletion'] = True
            report['status'] = 'PASS'
        except BaseException:
            report['status'] = 'FAIL'
            raise
        finally:
            cleanup_failures = 0
            if owner_page is not None and owner_headers is not None:
                for profile_id, pin in owned.items():
                    try:
                        response = await owner_page.request.delete(API + '/api/v1/profiles/' + profile_id,
                            headers=owner_headers | ({'X-Profile-Pin': pin} if pin else {}), timeout=20000)
                        cleanup_failures += response.status not in (204, 404)
                    except Exception:
                        cleanup_failures += 1
            report['fixtureCleanup'] = 'FAIL' if cleanup_failures else 'PASS'
            if cleanup_failures:
                report['status'] = 'FAIL'
            await browser.close()
            output.write_text(json.dumps(report, indent=2) + '\n')
            if cleanup_failures:
                raise RuntimeError('Test-owned profile cleanup failed; inspect the disposable project')
    print('PASS real two-account profile/PIN ownership, committed denial counters, protected deletion and one-time concurrent Redis handoff')


if __name__ == '__main__':
    asyncio.run(main())
