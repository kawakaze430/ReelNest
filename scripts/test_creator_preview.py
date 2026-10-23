#!/usr/bin/env python3
"""Browser tests for OFFLINE, SIMULATED creator-flow preview, not actual REST APIs."""
import asyncio
import shutil
from pathlib import Path
from playwright.async_api import async_playwright
ROOT=Path(__file__).resolve().parents[1]
async def main():
    async with async_playwright() as p:
        executable=shutil.which('chromium') or shutil.which('google-chrome')
        browser=await p.chromium.launch(headless=True,executable_path=executable,
            args=['--no-sandbox'] if executable else [])
        page=await browser.new_page(viewport={'width':1280,'height':850})
        await page.set_content((ROOT/'preview/creator-flow.html').read_text(),wait_until='load')
        await page.get_by_role('button',name='Submit creator application').click()
        await page.get_by_role('button',name='Review Console').click()
        await page.get_by_role('button',name='Approve creator').click()
        await page.get_by_role('button',name='Creator Studio').click()
        await page.get_by_role('button',name='Create film draft').click()
        await page.get_by_role('button',name='Declare rights').click()
        await page.locator('#video').set_input_files({'name':'clip.mp4','mimeType':'video/mp4','buffer':b'not a real video'})
        await page.get_by_role('button',name='Simulate private upload').click()
        await page.get_by_role('button',name='Submit for admin review').click()
        await page.get_by_role('button',name='Review Console').click()
        await page.get_by_role('button',name='Approve rights declaration').click()
        await page.get_by_role('button',name='Attempt publication').click()
        assert 'BLOCKED:' in await page.locator('#message').inner_text()
        assert await page.locator('.step.complete').count()==7
        # Also validate the small-screen design does not introduce horizontal overflow.
        await page.set_viewport_size({'width':390,'height':844})
        overflow=await page.evaluate('document.documentElement.scrollWidth > window.innerWidth + 2')
        assert not overflow, 'Creator preview overflows mobile viewport'
        print('PASS: simulated creator workflow 7/7 checkpoints, blocked publish, mobile layout')
        await browser.close()
if __name__=='__main__': asyncio.run(main())
