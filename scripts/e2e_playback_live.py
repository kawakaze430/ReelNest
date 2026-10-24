#!/usr/bin/env python3
"""Real private HLS API, browser decode, two-account isolation and revocation tests.

Requires the successful unique fixture from e2e_processing_live.py on the same
opted-in disposable developer stack. No payment transaction is performed.
"""
import asyncio
import json

from playwright.async_api import async_playwright
from live_e2e_helpers import API, ROOT, WEB, chromium_launch_options, hls_child_url, load_fixture, login, require_disposable_stack


def playlist_children(text):
    assert text.startswith('#EXTM3U'), 'Invalid HLS playlist'
    return [line.strip() for line in text.splitlines() if line.strip() and not line.startswith('#')]


async def main():
    require_disposable_stack()
    report_path = ROOT / 'build-evidence' / 'playback-latest.json'
    report_path.unlink(missing_ok=True)
    fixture = load_fixture()
    report_path.write_text(json.dumps({'status': 'RUNNING', 'project': fixture['project'], 'sourceCommit': fixture['sourceCommit']}, indent=2) + '\n')
    title_id = fixture['titleId']
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(**chromium_launch_options())
        try:
            page = await browser.new_page()
            viewer = await login(page, 'demo', 'DemoOnly123!')
            await page.locator('.locale-picker select').select_option('ar')
            await page.wait_for_function("document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'")
            await page.reload()
            await page.wait_for_function("document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'")
            await page.locator('.locale-picker select').select_option('en')
            await page.wait_for_function("document.documentElement.dir === 'ltr' && document.documentElement.lang === 'en'")
            headers = {'Authorization': 'Bearer ' + viewer['access_token'], 'Content-Type': 'application/json'}
            response = await page.request.get(API + '/api/v1/profiles', headers=headers)
            assert response.status == 200, f'Owner profiles request failed: HTTP {response.status}'
            profiles = await response.json()
            profile = next((item for item in profiles if not item['child']), None)
            if profile is None:
                response = await page.request.post(API + '/api/v1/profiles', headers=headers,
                    data={'name': 'Playback Integration', 'preferredLanguage': 'en', 'child': False})
                assert response.status == 201, f'Profile creation failed: HTTP {response.status}'
                profile = await response.json()
            other_page = await browser.new_page()
            other = await login(other_page, 'reviewer', 'LocalReviewer123!')
            assert other['profile']['sub'] != viewer['profile']['sub'], 'Isolation test requires two independent accounts'
            other_headers = {'Authorization': 'Bearer ' + other['access_token'], 'Content-Type': 'application/json'}
            other_profiles = await other_page.request.get(API + '/api/v1/profiles', headers=other_headers)
            assert other_profiles.status == 200, 'Second account authentication failed'
            history_path = API + f'/api/v1/playback/continue-watching?profileId={profile["id"]}'
            assert (await other_page.request.get(history_path, headers=other_headers)).status in (403, 404), 'Cross-account history access permitted'
            assert (await page.request.get(API + '/api/v1/me', headers={'Authorization': 'Bearer ' + viewer['access_token'] + '.invalid'})).status == 401, 'Tampered JWT accepted'
            cross_start = await other_page.request.post(API + '/api/v1/playback/sessions', headers=other_headers,
                data={'profileId': profile['id'], 'titleId': title_id})
            assert cross_start.status in (403, 404), 'Second account started playback with another account profile'
            invalid = await page.request.post(API + '/api/v1/playback/sessions', headers=headers,
                data={'titleId': '00000000-0000-4000-8000-000000000001', 'profileId': profile['id']})
            assert invalid.status == 404, 'Unprocessed demo metadata became playable'
            result = await page.request.post(API + '/api/v1/playback/sessions', headers=headers,
                data={'profileId': profile['id'], 'titleId': title_id})
            assert result.status == 201, f'Playback session creation failed: HTTP {result.status}'
            session = await result.json()
            manifest = API + session['manifestUrl']
            master = await page.request.get(manifest, headers=headers)
            assert master.status == 200, f'Private master unavailable: HTTP {master.status}'
            variants = playlist_children(await master.text())
            assert variants, 'Master playlist contains no video variant'
            variant_url = hls_child_url(manifest, variants[0])
            variant = await page.request.get(variant_url, headers=headers)
            assert variant.status == 200, f'Private variant unavailable: HTTP {variant.status}'
            segments = playlist_children(await variant.text())
            assert segments, 'Variant contains no media segments'
            segment_urls = [hls_child_url(variant_url, item) for item in segments]
            total_bytes = 0
            for segment_url in segment_urls:
                segment = await page.request.get(segment_url, headers=headers)
                assert segment.status == 200, f'Private segment unavailable: HTTP {segment.status}'
                payload = await segment.body()
                assert len(payload) >= 188 and payload[0] == 0x47, 'Expected nonempty MPEG-TS video bytes'
                total_bytes += len(payload)
            for url in (manifest, variant_url, segment_urls[0]):
                assert (await page.request.get(url)).status == 401, 'Anonymous private HLS access permitted'
                assert (await other_page.request.get(url, headers=other_headers)).status in (403, 404), 'Cross-account private HLS access permitted'
            sid = session['sessionId']
            progress_url = API + f'/api/v1/playback/sessions/{sid}/progress'
            progress = await page.request.put(progress_url, headers=headers, data={'positionSeconds': 2, 'sequence': 1})
            assert progress.status == 200, f'Progress write failed: HTTP {progress.status}'
            assert (await page.request.put(progress_url, headers=headers, data={'positionSeconds': 1, 'sequence': 1})).status == 409, 'Stale progress sequence accepted'
            history = await page.request.get(history_path, headers=headers)
            assert history.status == 200 and any(item['titleId'] == title_id for item in await history.json()), 'Resume history missing'
            watchlist_item = API + f'/api/v1/watchlist/{title_id}?profileId={profile["id"]}'
            assert (await page.request.put(watchlist_item, headers=headers)).status == 204
            watchlist = await page.request.get(API + f'/api/v1/watchlist?profileId={profile["id"]}', headers=headers)
            assert watchlist.status == 200 and any(item['id'] == title_id for item in await watchlist.json())
            assert (await page.request.delete(watchlist_item, headers=headers)).status == 204
            clear = await page.request.delete(API + f'/api/v1/playback/continue-watching/{title_id}?profileId={profile["id"]}', headers=headers)
            assert clear.status == 204
            after = await page.request.get(history_path, headers=headers)
            assert after.status == 200 and not any(item['titleId'] == title_id for item in await after.json())
            # Close API-created session before the real player; active-session limits remain enabled.
            assert (await page.request.delete(API + f'/api/v1/playback/sessions/{sid}', headers=headers)).status == 204
            for url in (manifest, variant_url, segment_urls[0]):
                assert (await page.request.get(url, headers=headers)).status == 403, 'Revoked HLS session remained usable'
            assert (await page.request.put(progress_url, headers=headers, data={'positionSeconds': 3, 'sequence': 2})).status == 403, 'Revoked session accepted progress'

            await page.evaluate('''({subject, id}) => localStorage.setItem(`reelnest:active-profile:${subject}`, id)''',
                                {'subject': viewer['profile']['sub'], 'id': profile['id']})
            await page.goto(WEB + '/watch/' + title_id)
            play = page.get_by_role('button', name='Play video', exact=True)
            await play.wait_for(timeout=60000)
            await page.wait_for_function('''() => {
                const button = document.querySelector('button[aria-label="Play video"]');
                const video = document.querySelector('video');
                return button && !button.disabled && video && video.readyState >= 2;
            }''', timeout=60000)
            await play.click()
            # Exercise the shipped hls.js player and actual browser H.264/AAC decode.
            await page.wait_for_function('''() => {
                const video = document.querySelector('video');
                return video && !video.paused && !video.error && video.currentTime >= 2
                    && video.videoWidth > 0 && video.videoHeight > 0
                    && video.getVideoPlaybackQuality().totalVideoFrames > 0;
            }''', timeout=45000)
            playback = await page.locator('video').evaluate('''video => ({
                currentTime: video.currentTime, duration: video.duration,
                width: video.videoWidth, height: video.videoHeight,
                decodedFrames: video.getVideoPlaybackQuality().totalVideoFrames
            })''')
            # Global APP logout must invalidate the old JWT and its active media,
            # while the independently authenticated second account remains usable.
            sessions = await page.request.get(API + '/api/v1/me/sessions', headers=headers)
            assert sessions.status == 200
            active = [item for item in await sessions.json() if item['status'] == 'ACTIVE' and item['profileId'] == profile['id']]
            assert active, 'Browser player did not create an active playback session'
            revoked = await page.request.post(API + '/api/v1/me/sessions/revoke-all', headers=headers)
            assert revoked.status == 200 and (await revoked.json())['affected'] >= 1, 'Application logout did not revoke active playback'
            assert (await page.request.get(API + '/api/v1/profiles', headers=headers)).status == 401, 'Pre-logout JWT remained usable'
            for item in active:
                url = API + f'/api/v1/playback/sessions/{item["id"]}/files/master.m3u8'
                assert (await page.request.get(url, headers=headers)).status == 401, 'Old JWT accessed media after global application logout'
            assert (await other_page.request.get(API + '/api/v1/profiles', headers=other_headers)).status == 200, 'Logout unexpectedly affected another account'
            report = {'status': 'PASS', 'project': fixture['project'], 'sourceCommit': fixture['sourceCommit'],
                      'titleId': title_id, 'variantSegmentsFetched': len(segments),
                      'variantBytesFetched': total_bytes, 'browserPlayback': playback,
                      'anonymousMediaDenied': True, 'crossAccountMediaDenied': True,
                      'crossAccountProfileDenied': True, 'tamperedJwtDenied': True,
                      'revokedSessionDenied': True, 'globalAppJwtRevocation': True, 'progressAndWatchlist': True,
                      'localeDirectionPersistence': True}
            report_path.write_text(json.dumps(report, indent=2) + '\n')
        finally:
            await browser.close()
    print('PASS: authenticated HLS bytes -> actual browser playback/decoded frames; two-account isolation, tampered JWT, resume/watchlist, revoked media and global application JWT logout denial')


if __name__ == '__main__':
    asyncio.run(main())
