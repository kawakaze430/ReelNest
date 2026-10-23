# Operations incident response playbooks — ReelNest

**Environment:** never put raw access tokens, credit-card numbers, CloudFront private keys or payment secrets into incident tickets/logs. An incident owner must be able to revoke credentials and access rapidly.

## 1. Playback stopped / 403

1. Collect request ID, session ID (not bearer token), title ID, timestamp, viewer region and failing CDN path, with appropriate data minimization.
2. Verify creator status, title publication, `media_assets.processing_status='READY'`, approved rights dates/territories and profile age restriction.
3. Inspect `subscriptions`, transaction refund/dispute hold, premium entitlement; do not manually change paid-through without verified provider reconciliation.
4. Verify CloudFront key group, signed cookies time skew, authorized path prefix and OAC bucket policy. A 403 can be expected after a cookie expires or territory is invalid.
5. Stop playback / rotate signing key if media access is exposed. Note up to 120 seconds of previously issued cookie access under current policy.

## 2. Missing revenue or duplicate webhook

1. Record Razorpay event ID and provider subscription/payment IDs with operator access only.
2. Confirm signed webhook and dedup records. Cross-check provider charge/refund/dispute status using official TEST/live merchant dashboard as appropriate.
3. Do not overwrite `billing_transactions` or `paid_through` with fabricated values.
4. Investigate `payment_webhook_events.processed_at` and `processing_error`; a rollback means the event should be retried by provider. Replay/reconcile only with verified payload and idempotency safeguards.
5. In cases of dispute/partial refund, preserve conservative access hold pending investigation. Notify support and incident owner.

## 3. Video transcoding backlog

1. Inspect job states (`QUEUED`,`RUNNING`,`RETRY_WAIT`,`FAILED`) and worker logs with media asset ID.
2. Check FFprobe container validation, FFmpeg timeouts, CPU/memory, scratch space, S3 access and DB connectivity.
3. Retry only failed jobs per the existing bounded retry and lease rules; ensure no duplicate published assets.
4. A compromised upload must remain quarantined; do not publish while validating the source.

## 4. Database problems

1. Freeze schema deployments and take a verified snapshot; determine failed Flyway version before any repair.
2. Restore into a separate environment and test consistency/entitlements/creator rights. Never run `flyway clean` against real customer data.
3. Escalate if recovery point objective/time target cannot be met. Track incident timestamps and users affected.

## 5. Potential data breach / account incident

1. Restrict affected endpoint, revoke exposed keys/tokens, check access logs and audit role changes.
2. Scope access through request IDs; preserve evidence securely, avoid downloading unrelated personal records.
3. Engage incident/privacy counsel for statutory notification and user remediation as applicable.
4. Implement patch, security tests, rotate credentials and document root cause before lifting restrictions.

## 6. Surge and abuse

1. Inspect WAF, API rate limits, CDN hit ratio, edge errors and unusual checkout/session request rates.
2. Tune WAF rules with false-positive review; avoid bypassing paid-access or other security controls to relieve load.
3. Add capacity only after confirming bottleneck and billing cost; throttle new uploads or encoding jobs when over quota.

## 7. Rights expiry or copyright complaint

1. Place title into non-published or suspended state through authorized admin workflow.
2. Prevent new playback sessions and account for up to 120-second CDN cookies already issued.
3. Preserve relevant licence evidence and audit data, inform creator, follow lawful takedown/appeal procedures.
4. Do not remove records required for investigations without legal review.
