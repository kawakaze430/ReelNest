#!/usr/bin/env python3
"""Offline security/source regression for cumulative Builds 05+06.

This cannot replace live provider callbacks, migrations, full Maven compilation,
or controlled load tests. Every assertion checks an actual source invariant.
"""
from pathlib import Path
import re
import yaml
root=Path(__file__).resolve().parents[1]
read=lambda p:(root/p).read_text(encoding='utf8')
migration=read('apps/api/src/main/resources/db/migration/V6__subscription_billing.sql')
for phrase in ('subscription_plans','subscriptions','payment_webhook_events',
               'provider_event_id VARCHAR(140) PRIMARY KEY','paid_through TIMESTAMPTZ',
               'billing_transactions','provider_payment_id VARCHAR(120) UNIQUE'):
    assert phrase in migration,phrase
billing=read('apps/api/src/main/java/com/reelnest/billing/BillingService.java')
for phrase in ('WebhookVerifier.verify(raw,signature,webhookSecret)',
               'ON CONFLICT DO NOTHING','provider.get(subId)',
               'recordCharge(event,subId)','paid_through>NOW()',
               'current_period_end>NOW()','SELECT id FROM users WHERE id=? FOR UPDATE',
               'provider.getPlan(planId)','amount_paise','status IN (\'ACTIVE\',\'CANCEL_SCHEDULED\')'):
    assert phrase in billing,phrase
assert 'setPremium(true)' not in read('apps/web/src/components/billing-panel.tsx')
assert 'handler:()=>{' in read('apps/web/src/components/billing-panel.tsx')
assert 'void refresh()' in read('apps/web/src/components/billing-panel.tsx')
policy=read('apps/api/src/main/java/com/reelnest/billing/WebhookVerifier.java')
assert 'MessageDigest.isEqual' in policy and 'HmacSHA256' in policy
security=read('apps/api/src/main/java/com/reelnest/security/SecurityConfig.java')
assert '.requestMatchers(HttpMethod.POST, "/api/v1/billing/webhooks/razorpay").permitAll()' in security
assert '.anyRequest().authenticated()' in security
playback=read('apps/api/src/main/java/com/reelnest/playback/PlaybackService.java')
assert 'billing.hasPremium(userId)' in playback
assert 'eligibleAsset(s.titleId(),profile.child(),user)' in playback
assert 'premium' in playback.lower()
recommend=read('apps/api/src/main/java/com/reelnest/discovery/DiscoveryService.java')
for phrase in ('rights_territories','valid_until>=CURRENT_DATE','processing_status=\'READY\'',
               'owner(jwt,profileId)','PlaybackPolicy.allowedAge','recommendation','viewer_events'):
    assert phrase.lower() in recommend.lower(),phrase
analytics=read('apps/api/src/main/java/com/reelnest/discovery/DiscoveryController.java')
assert analytics.count('hasRole(\'ADMIN\')')==2
assert 'ON CONFLICT(id) DO NOTHING' in recommend
compose=yaml.safe_load(read('compose.yaml'))
for key in ('RAZORPAY_KEY_ID','RAZORPAY_KEY_SECRET','RAZORPAY_WEBHOOK_SECRET',
            'RAZORPAY_STANDARD_PLAN_ID','RAZORPAY_PREMIUM_PLAN_ID'):
    assert key in compose['services']['api']['environment']
    assert str(compose['services']['api']['environment'][key]).startswith('${')
for path in ('apps/web/src/app/pricing/page.tsx',
             'apps/web/src/app/discover/page.tsx','apps/web/src/app/admin/analytics/page.tsx',
             'apps/recommendation-lab/model.py','apps/recommendation-lab/tests/test_model.py'):
    assert (root/path).is_file(),path
print('PASS: Build 05-06 migrations, signature and paid gating, price plan validation, discovery and admin auth source gates')
