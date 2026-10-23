#!/usr/bin/env python3
"""Build 04 REAL integration (requires full stack + Build03 video upload fixture).

Not executed in file authoring environment without Docker/Maven/network.
Prerequisite: scripts/e2e_processing_live.py creates and publishes a READY original film.
Usage: python3 scripts/e2e_playback_live.py
"""
import asyncio
import json
from playwright.async_api import async_playwright

WEB = "http://localhost:3000"
API = "http://localhost:8080"
TITLE_NAME = "Build 03 Original Worker Integration"

async def bearer(page):
    # oidc-client-ts default browser session storage (and compatible local storage fallback).
    raw = await page.evaluate("""() => {
      for (const storage of [sessionStorage,localStorage]) {
        for (let i=0;i<storage.length;i++) {
          const key=storage.key(i);
          if(key && key.startsWith('oidc.user:')) return storage.getItem(key);
        }
      }
      return null;
    }""")
    assert raw, "OIDC token missing in browser storage"
    return json.loads(raw)["access_token"]

async def login(page, username, password):
    await page.goto(WEB+'/profiles')
    await page.get_by_role('button',name='Sign in',exact=True).click()
    await page.locator('#username').fill(username)
    await page.locator('#password').fill(password)
    await page.locator('#kc-login').click()
    await page.wait_for_url('**/auth/callback*',timeout=45000)
    await page.wait_for_timeout(1200)
    return await bearer(page)

async def main():
    async with async_playwright() as p:
        browser=await p.chromium.launch(headless=True)
        page=await browser.new_page()
        token=await login(page,'demo','DemoOnly123!')
        headers={'Authorization':f'Bearer {token}','Content-Type':'application/json'}
        response=await page.request.get(API+'/api/v1/profiles',headers=headers)
        assert response.status==200,response.status
        profiles=await response.json()
        if not profiles:
            response=await page.request.post(API+'/api/v1/profiles',headers=headers,
                data={'name':'Playback Integration','preferredLanguage':'en','child':False})
            assert response.status==201,await response.text()
            profiles=[await response.json()]
        profile=next((x for x in profiles if not x['child']),None)
        assert profile, 'Need adult profile for streaming fixture'
        search=await page.request.get(API+'/api/v1/catalog?size=48')
        assert search.status==200
        title=next((x for x in (await search.json())['content'] if x['name']==TITLE_NAME),None)
        assert title, 'Run scripts/e2e_processing_live.py first to publish the sample film'
        invalid=await page.request.post(API+'/api/v1/playback/sessions',headers=headers,
            data={'titleId':'00000000-0000-4000-8000-000000000001','profileId':profile['id']})
        assert invalid.status==404,'Demo metadata must not stream'
        result=await page.request.post(API+'/api/v1/playback/sessions',headers=headers,
            data={'profileId':profile['id'],'titleId':title['id']})
        assert result.status==201,(result.status,await result.text())
        session=await result.json()
        manifest=API+session['manifestUrl']
        assert (await page.request.get(manifest)).status==401,'No public HLS manifests'
        master=await page.request.get(manifest,headers=headers)
        assert master.status==200,await master.text()
        text=await master.text()
        assert '#EXTM3U' in text
        relative=next(line.strip() for line in text.splitlines() if line and not line.startswith('#'))
        variant=await page.request.get(manifest.rsplit('/',1)[0]+'/'+relative,headers=headers)
        assert variant.status==200,(variant.status,await variant.text())
        segment=next(line.strip() for line in (await variant.text()).splitlines() if line and not line.startswith('#'))
        segment_url=manifest.rsplit('/',1)[0]+'/'+relative.rsplit('/',1)[0]+'/'+segment
        assert (await page.request.get(segment_url,headers=headers)).status==200
        sid=session['sessionId']
        progress=await page.request.put(API+f'/api/v1/playback/sessions/{sid}/progress',headers=headers,
            data={'positionSeconds':2,'sequence':1})
        assert progress.status==200,await progress.text()
        stale=await page.request.put(API+f'/api/v1/playback/sessions/{sid}/progress',headers=headers,
            data={'positionSeconds':1,'sequence':1})
        assert stale.status==409,'Stale progress events must fail'
        cw=await page.request.get(API+f'/api/v1/playback/continue-watching?profileId={profile["id"]}',headers=headers)
        assert cw.status==200
        assert any(x['titleId']==title['id'] for x in await cw.json())
        wl=API+f'/api/v1/watchlist/{title["id"]}?profileId={profile["id"]}'
        assert (await page.request.put(wl,headers=headers)).status==204
        assert any(x['id']==title['id'] for x in await
            (await page.request.get(API+f'/api/v1/watchlist?profileId={profile["id"]}',headers=headers)).json())
        assert (await page.request.delete(wl,headers=headers)).status==204
        clear=await page.request.delete(API+f'/api/v1/playback/continue-watching/{title["id"]}?profileId={profile["id"]}',headers=headers)
        assert clear.status==204
        after=await page.request.get(API+f'/api/v1/playback/continue-watching?profileId={profile["id"]}',headers=headers)
        assert not any(x['titleId']==title['id'] for x in await after.json())
        assert (await page.request.delete(API+f'/api/v1/playback/sessions/{sid}',headers=headers)).status==204
        assert (await page.request.get(manifest,headers=headers)).status==403
        await browser.close()
    print('PASS: entitlement -> authenticated HLS -> progress -> continue watching -> watchlist -> revoke')

if __name__=='__main__':
    asyncio.run(main())
