#!/usr/bin/env python3
"""Optional Chromium/Playwright smoke tests for the zero-install preview.
Install `playwright` and Chromium beforehand if not present.
set_content avoids requiring an HTTP server or external network.
"""
import asyncio
import shutil
from pathlib import Path
from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]

async def main():
    html = (ROOT / 'preview' / 'index.html').read_text()
    async with async_playwright() as playwright:
        browser_path = shutil.which('chromium') or shutil.which('google-chrome')
        browser = await playwright.chromium.launch(
            headless=True, executable_path=browser_path, args=['--no-sandbox'] if browser_path else []
        )
        page = await browser.new_page(viewport={'width': 1440, 'height': 930})
        await page.set_content(html, wait_until='load')
        # about:blank has an opaque origin where localStorage is disabled.
        # Replace it only for this isolated synthetic-origin test.
        await page.evaluate('''() => {
          const memory = {};
          Object.defineProperty(window, 'localStorage', {configurable: true, value: {
            getItem(k) { return memory[k] ?? null; },
            setItem(k, v) { memory[k] = v; }
          }});
        }''')
        assert await page.locator('.poster').count() == 8
        await page.locator('#search').fill('neon')
        assert await page.locator('.poster').count() == 1
        await page.locator('#search').fill('')
        await page.locator('#genre').select_option('Sci-Fi')
        assert await page.locator('.poster').count() == 2
        await page.locator('#genre').select_option('')
        await page.locator('.heart').first.click()
        await page.locator('#favorites').click()
        assert await page.locator('.poster').count() == 1
        await page.locator('.poster').first.click()
        assert await page.locator('#modal').is_visible()
        await page.locator('#play').click()
        assert await page.locator('#play-result').is_visible()
        assert await page.locator('#sample-video').is_visible()
        await page.keyboard.press('Escape')
        assert await page.locator('#modal').is_hidden()
        print('PASS: preview renders, searches, filters, saves favorites and opens/closes media details')
        await browser.close()

if __name__ == '__main__':
    asyncio.run(main())
