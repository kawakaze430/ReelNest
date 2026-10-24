#!/usr/bin/env python3
"""Real browser identity/profile/player-mount regression on a disposable local stack.

This component check deliberately denies unprocessed metadata. It does not prove
media encoding or decoded playback; e2e_playback_live.py verifies those later.
"""
import asyncio
import json
import subprocess

from playwright.async_api import async_playwright
from live_e2e_helpers import API, ROOT, WEB, chromium_launch_options, login, require_disposable_stack


async def main():
    require_disposable_stack()
    output = ROOT / 'build-evidence' / 'browser-identity-latest.json'
    output.parent.mkdir(exist_ok=True)
    report = {'status': 'RUNNING', 'scope': 'Browser identity, owned profiles, locale and denied player startup; no media decode claim',
              'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()}
    output.write_text(json.dumps(report, indent=2) + '\n')
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(**chromium_launch_options())
        try:
            for username, password in (('demo', 'DemoOnly123!'), ('reviewer', 'LocalReviewer123!')):
                page = await browser.new_page()
                account = await login(page, username, password)
                headers = {'Authorization': 'Bearer ' + account['access_token']}
                response = await page.request.get(API + '/api/v1/profiles', headers=headers)
                assert response.status == 200, f'Authenticated profiles failed: HTTP {response.status}'
                profiles = await response.json()
                if not profiles:
                    response = await page.request.post(API + '/api/v1/profiles', headers=headers,
                        data={'name': 'Browser regression ' + username, 'preferredLanguage': 'en', 'child': False})
                    assert response.status == 201, f'Test profile creation failed: HTTP {response.status}'
                    profiles = [await response.json()]
                await page.goto(WEB + '/my-list')
                picker = page.get_by_role('combobox', name='Active viewer profile')
                await picker.wait_for()
                ids = await picker.locator('option').evaluate_all('(items) => items.map(item => item.value)')
                assert set(ids) == {profile['id'] for profile in profiles}, 'Profile picker exposed foreign or stale profiles'
                await page.locator('.locale-picker select').select_option('ar')
                await page.wait_for_function("document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'")
                await page.reload()
                await page.wait_for_function("document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'")
                await page.locator('.locale-picker select').select_option('en')
                await page.wait_for_function("document.documentElement.dir === 'ltr' && document.documentElement.lang === 'en'")
                async with page.expect_response(
                    lambda response: response.url == API + '/api/v1/playback/sessions' and response.request.method == 'POST',
                    timeout=30000,
                ) as starting:
                    await page.goto(WEB + '/watch/00000000-0000-4000-8000-000000000001')
                started = await starting.value
                assert started.status == 404, f'Expected unprocessed metadata denial, got HTTP {started.status}'
                assert started.request.post_data_json['profileId'] in {profile['id'] for profile in profiles}, 'Player used a foreign profile'
                assert await page.locator('video').count() == 1, 'Player failed to mount after profile resolution'
                report[username] = {'PKCE': True, 'ownerProfilePicker': True, 'localePersistsAcrossReload': True,
                                    'playerMountedAfterIdentityResolution': True, 'unprocessedTitleHttp': started.status}
            report['status'] = 'PASS'
        except BaseException:
            report['status'] = 'FAIL'
            raise
        finally:
            await browser.close()
            output.write_text(json.dumps(report, indent=2) + '\n')
    print('PASS real browser PKCE, owner profile picker, persisted RTL locale and denied unprocessed player startup for two accounts')


if __name__ == '__main__':
    asyncio.run(main())
