#!/usr/bin/env python3
"""OPTIONAL live integration, requires Docker + Playwright + Chromium.

Run against a DISPOSABLE developer stack only; it creates records in the DB.
This has NOT been executed in the authoring environment.

    docker compose up --build -d
    python3 scripts/e2e_processing_live.py
"""
import asyncio
from datetime import date, timedelta
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import urlopen
from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]
WEB = 'http://localhost:3000'

async def login(page, button, username, password):
    await page.get_by_role('button', name=button).click()
    await page.locator('#username').fill(username)
    await page.locator('#password').fill(password)
    await page.locator('#kc-login').click()
    await page.wait_for_url('**/auth/callback*', timeout=40000)
    await page.wait_for_timeout(1500)

async def main():
    async with async_playwright() as playwright:
        browser=await playwright.chromium.launch(headless=True)
        creator=await browser.new_page()
        await creator.goto(WEB+'/creator')
        await login(creator,'Sign in to Creator Studio','demo','DemoOnly123!')
        await creator.goto(WEB+'/creator')
        await creator.get_by_label('Display name').fill('Integration Test Film Studio')
        await creator.get_by_label('Legal name').fill('Test Studio')
        await creator.get_by_label('Contact email').fill('integration@example.invalid')
        await creator.get_by_label('Describe your work and rights to distribute it').fill(
            'A programmatically generated original test video; no third-party content is distributed.')
        await creator.get_by_role('button',name='Submit application').click()
        await creator.get_by_text('PENDING',exact=True).wait_for()

        reviewer=await browser.new_page()
        await reviewer.goto(WEB+'/admin')
        await login(reviewer,'Sign in as reviewer','reviewer','LocalReviewer123!')
        await reviewer.goto(WEB+'/admin')
        await reviewer.get_by_role('button',name='Approve',exact=True).first.click()
        await reviewer.get_by_text('Creator approved.').wait_for()

        await creator.reload()
        await creator.get_by_label('Film title').fill('Build 03 Original Worker Integration')
        await creator.get_by_label('Synopsis').fill('An original generated film used solely for local testing.')
        await creator.get_by_label('Duration (minutes)').fill('1')
        await creator.get_by_role('button',name='Create draft').click()
        await creator.get_by_text('Draft created.').wait_for()
        await creator.get_by_label('Valid from').fill(str(date.today()))
        await creator.get_by_label('Valid until').fill(str(date.today()+timedelta(days=365)))
        await creator.get_by_label('Rights evidence reference').fill('Local generated test asset in scripts/generate_demo_media.sh')
        await creator.get_by_label('Agreement notes').fill(
            'Created programmatically for internal application validation only; no licensed films involved.')
        await creator.get_by_role('button',name='Save rights').click()
        await creator.get_by_text('Rights declaration saved.').wait_for()
        await creator.locator('input[type="file"]').set_input_files(str(ROOT/'apps/web/public/sample/demo.mp4'))
        await creator.get_by_role('button',name='Upload securely').click()
        await creator.get_by_text('Source uploaded.').wait_for(timeout=90000)
        # Worker is separate; poll via UI refresh rather than arbitrary sleep-only assumptions.
        ready=False
        for _ in range(36):
            await creator.get_by_role('button',name='Refresh status').click()
            if await creator.locator('.studio-list-item strong').get_by_text('READY',exact=True).count():
                ready=True
                break
            await asyncio.sleep(5)
        assert ready,'Worker never reached READY; inspect docker compose logs video-worker'

        await creator.get_by_role('button',name='Submit film for review').click()
        await creator.get_by_text('Sent to administrator review.').wait_for()
        await reviewer.reload()
        await reviewer.get_by_role('button',name='Inspect rights').first.click()
        await reviewer.get_by_role('button',name='Approve rights').first.click()
        await reviewer.get_by_text('Rights approved.').wait_for()
        await reviewer.get_by_role('button',name='Attempt publish').first.click()
        await reviewer.get_by_text('Title published.').wait_for(timeout=15000)
        await browser.close()

    # Processed storage stays private even though the title is in the catalog.
    try:
        urlopen('http://localhost:9000/reelnest-processed-private/',timeout=5)
    except HTTPError as e:
        assert e.code in (401,403),f'Unexpected processed bucket response: {e.code}'
    else:
        raise AssertionError('Processed bucket unexpectedly exposed to anonymous callers')
    print('PASS: creator -> approved -> upload -> HLS READY -> rights approved -> publish')

if __name__=='__main__':
    asyncio.run(main())
