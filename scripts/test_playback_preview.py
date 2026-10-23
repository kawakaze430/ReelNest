#!/usr/bin/env python3
"""Browser UI tests for simulated offline Build 04 preview (NOT real API or secure playback)."""
from pathlib import Path
from playwright.sync_api import sync_playwright

html=(Path(__file__).resolve().parents[1]/'preview/playback-flow.html').read_text()
with sync_playwright() as pw:
    browser=pw.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':1280,'height':800})
    page.set_content(html,wait_until='load')
    page.evaluate('''() => {
      const memory = {};
      Object.defineProperty(window,'localStorage',{configurable:true,value:{
        getItem(k){return memory[k]??null},setItem(k,v){memory[k]=v}
      }});
    }''')
    assert page.get_by_role('heading',name='Your cinema, resumed.').is_visible()
    assert page.locator('#clip').count()==1
    page.get_by_role('button',name='＋ Save').click()
    assert page.get_by_role('button',name='♥ Saved').is_visible()
    page.locator('#profile').select_option('kids')
    assert page.get_by_role('button',name='＋ Save').is_visible(), 'Profile data isolation failed'
    page.get_by_role('button',name='＋ Save').click()
    page.locator('#profile').select_option('alex')
    assert page.get_by_role('button',name='♥ Saved').is_visible()
    page.locator('#clip').evaluate('(v) => {v.currentTime=2;v.dispatchEvent(new Event("pause"))}')
    assert page.get_by_role('button',name='Resume').count()==1
    page.get_by_role('button',name='Reset progress').click()
    assert page.get_by_text('No unfinished video for this profile.').count()==1
    page.set_viewport_size({'width':390,'height':844})
    assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth+2'), 'Horizontal overflow on mobile'
    browser.close()
print('PASS: Build04 offline UI, profile separation, watchlist, progress, reset and mobile layout')
