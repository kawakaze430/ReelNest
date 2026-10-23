#!/usr/bin/env python3
from pathlib import Path
from collections import Counter
import csv
root=Path(__file__).resolve().parents[1]
rows=list(csv.DictReader((root/'docs/BUILD22_282_TASKS.csv').open()))
assert len(rows)==282 and len({r['task_id'] for r in rows})==282
counts=Counter(r['build22_status'] for r in rows)
assert counts['Source implemented — runtime unverified']==66
assert counts['Partially implemented']==116
assert counts['Blocked — external execution / operator verification']==40
assert counts['Blocked — external approval / account']==17
assert counts['Not started / no audited implementation evidence']==43
for r in rows:
    if r['build22_evidence']:
        for p in r['build22_evidence'].split(';'):
            assert (root/p).exists(), (r['task_id'],p)
for p in ['apps/api/src/main/resources/db/migration/V21__notifications_reporting.sql',
          'apps/api/src/main/resources/db/migration/V22__promotion_quotes.sql',
          'apps/recommendation-api/semantic_search.py',
          'apps/web/src/components/tv-remote-mode.tsx']:
    assert (root/p).is_file()
notifications=(root/'apps/api/src/main/java/com/reelnest/notifications/NotificationController.java').read_text()
assert 'AND user_id=?' in notifications and 'users.resolve(jwt)' in notifications
analytics=(root/'apps/api/src/main/java/com/reelnest/analytics/EngagementAnalyticsController.java').read_text()
assert "hasRole('ADMIN')" in analytics and 'COUNT(DISTINCT' in analytics
search=(root/'apps/api/src/main/java/com/reelnest/discovery/PrivateRecommendationClient.java').read_text()
assert 'allowed.containsKey(rank.id())' in search and 'X-ReelNest-Internal-Token' in search
promo=(root/'apps/api/src/main/java/com/reelnest/billing/PromotionPreviewController.java').read_text()
assert 'Illustrative quote ONLY' in promo and 'hasPremium' not in promo
print('Build22 source-contract/status checks passed; 282 unique task IDs')
