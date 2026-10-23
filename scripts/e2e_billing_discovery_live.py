#!/usr/bin/env python3
"""Build 05+06 genuine API authorization integration test; requires running Compose.

Uses local Keycloak demo/reviewer identities, but deliberately never creates a paid
subscription or simulates a successful payment. Real provider testing needs Razorpay
TEST keys, plans and a webhook-accessible HTTPS endpoint.
"""
import asyncio
import json
from playwright.async_api import async_playwright
API='http://localhost:8080'
WEB='http://localhost:3000'

async def login(page,username,password):
    await page.goto(WEB+'/profiles')
    await page.get_by_role('button',name='Sign in',exact=True).click()
    await page.locator('#username').fill(username)
    await page.locator('#password').fill(password)
    await page.locator('#kc-login').click()
    await page.wait_for_url('**/auth/callback*',timeout=45000)
    await page.wait_for_timeout(1200)
    raw=await page.evaluate('''() => {
      for(const storage of [sessionStorage,localStorage]) {
        for(let i=0;i<storage.length;i++) {
          const key=storage.key(i);
          if(key && key.startsWith('oidc.user:'))return storage.getItem(key)
        }
      }
      return null;
    }''')
    assert raw,'No browser OIDC token available'
    return json.loads(raw)['access_token']

async def run():
    async with async_playwright() as pw:
        browser=await pw.chromium.launch(headless=True)
        viewer=await browser.new_page()
        plans=await viewer.request.get(API+'/api/v1/billing/plans')
        assert plans.status==200,(plans.status,await plans.text())
        codes={plan['code'] for plan in await plans.json()}
        assert {'FREE','STANDARD','PREMIUM'}<=codes
        forged=await viewer.request.post(API+'/api/v1/billing/webhooks/razorpay',
            headers={'X-Razorpay-Signature':'0'*64,'x-razorpay-event-id':'evt_test_forged_2026'},
            data='{"event":"subscription.charged"}')
        assert forged.status==401,('forged webhook unexpectedly accepted',forged.status)
        token=await login(viewer,'demo','DemoOnly123!')
        headers={'Authorization':'Bearer '+token}
        accounts=await viewer.request.get(API+'/api/v1/billing/me',headers=headers)
        assert accounts.status==200
        response=await accounts.json()
        assert isinstance(response['premiumAccess'],bool)
        assert isinstance(response['subscriptions'],list)
        profiles=await viewer.request.get(API+'/api/v1/profiles',headers=headers)
        assert profiles.status==200
        profile_list=await profiles.json()
        if not profile_list:
            created=await viewer.request.post(API+'/api/v1/profiles',headers=headers,
                data={'name':'Discovery Test','preferredLanguage':'en','child':False})
            assert created.status==201,(created.status,await created.text())
            profile_list=[await created.json()]
        pid=profile_list[0]['id']
        personalized=await viewer.request.get(API+f'/api/v1/discovery/home?profileId={pid}',headers=headers)
        assert personalized.status==200,(personalized.status,await personalized.text())
        assert isinstance(await personalized.json(),list)
        forbidden=await viewer.request.get(API+'/api/v1/discovery/admin/overview',headers=headers)
        assert forbidden.status==403,'Viewer must not read admin analytics'
        title_id='00000000-0000-4000-8000-000000000001'
        import uuid
        event={'eventId':str(uuid.uuid4()),'profileId':pid,'titleId':title_id,'eventType':'DETAIL_VIEW'}
        received=await viewer.request.post(API+'/api/v1/discovery/events',headers=headers,data=event)
        assert received.status==204,(received.status,await received.text())
        repeated=await viewer.request.post(API+'/api/v1/discovery/events',headers=headers,data=event)
        assert repeated.status==204,'Duplicate event should be idempotent'
        wrong_profile=await viewer.request.get(API+f'/api/v1/discovery/home?profileId={uuid.uuid4()}',headers=headers)
        assert wrong_profile.status==404
        admin=await browser.new_page()
        admin_token=await login(admin,'reviewer','LocalReviewer123!')
        overview=await admin.request.get(API+'/api/v1/discovery/admin/overview',
            headers={'Authorization':'Bearer '+admin_token})
        assert overview.status==200,(overview.status,await overview.text())
        assert 'viewers' in await overview.json()
        await browser.close()
    print('PASS: public plan list, forged webhook rejection, account billing, profile-scoped discovery, event dedupe, admin isolation')

if __name__=='__main__':asyncio.run(run())
