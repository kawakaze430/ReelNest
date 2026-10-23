#!/usr/bin/env python3
"""Offline Build 07/08 release contract tests (not a substitute for Docker/AWS integration)."""
import pathlib, re, subprocess, tempfile, json
from pathlib import Path
root = Path(__file__).resolve().parents[1]

def read(path):
    assert (root / path).exists(), f"Missing {path}"
    return (root / path).read_text()

migration=read('apps/api/src/main/resources/db/migration/V8__billing_disputes_and_privacy.sql')
assert 'refunded_paise' in migration and 'privacy_requests' in migration
billing=read('apps/api/src/main/java/com/reelnest/billing/BillingService.java')
assert 'reconcilePayment(' in billing and 'recomputePaidThrough(' in billing
assert 'amount_refunded' in billing and "b.status='CHARGED'" in billing
assert 'WebhookVerifier.verify' in billing
assert 'provider.getPayment' in billing
cdn=read('apps/api/src/main/java/com/reelnest/playback/CdnCookieSigner.java')
assert 'SHA1withRSA' in cdn and 'CloudFront-Signature' in cdn
assert 'HttpHeaders.SET_COOKIE' in cdn and 'SameSite=None' in cdn
playback=read('apps/api/src/main/java/com/reelnest/playback/PlaybackService.java')
assert 'cdnGeoReviewed' in playback and 'requireActiveSession' in playback
security=read('apps/api/src/main/java/com/reelnest/security/SecurityConfig.java')
assert 'setAllowCredentials(true)' in security and 'AdmissionRateLimiter' in security
privacy=read('apps/api/src/main/java/com/reelnest/privacy/PrivacyController.java')
assert 'hasRole(\'ADMIN\')' in privacy and 'WHERE user_id=?' in privacy
assert 'not automated' not in privacy.lower() or 'deletion' in privacy.lower()
worker=read('apps/video-worker/worker.py')
assert 'DB_HOST' in worker and 'boto3.client("s3", **opts)' in worker
web=read('apps/web/src/components/protected-player.tsx')
assert 'xhr.withCredentials=true' in web
assert 'credentials: \'include\'' in read('apps/web/src/lib/api.ts')
assert (root/'apps/web/src/app/account/privacy/page.tsx').exists()
assert 'standalone' in read('apps/web/next.config.ts')
assert 'target: dev' in read('compose.yaml')
aws=root/'infrastructure/aws'
for x in ['versions.tf','variables.tf','network.tf','media.tf','services.tf','outputs.tf']:
    assert (aws/x).exists()
resources=(aws/'services.tf').read_text()+(aws/'media.tf').read_text()
for required in ['aws_ecs_service','aws_db_instance','aws_s3_bucket_public_access_block',
                 'aws_cloudfront_distribution','aws_cloudfront_origin_access_control',
                 'aws_wafv2_web_acl','aws_elasticache_replication_group']:
    assert required in resources, required
assert 'enable_services = false' in read('infrastructure/aws/terraform.tfvars.example')
# Compile and exercise pure Java payment decision logic with no Maven downloads.
policy=root/'apps/api/src/main/java/com/reelnest/billing/ChargePolicy.java'
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'PolicyHarness.java'
    path.write_text('''import com.reelnest.billing.ChargePolicy;
import java.time.Instant;
import java.util.List;
class PolicyHarness {
 public static void main(String[] args){
  var n=Instant.parse("2026-10-21T00:00:00Z");
  var future=n.plusSeconds(86400);
  var paid=new ChargePolicy.Charge("CHARGED",future,14900,0,false);
  var refunded=new ChargePolicy.Charge("REFUNDED",future,14900,14900,false);
  var disputed=new ChargePolicy.Charge("DISPUTED",future,14900,0,true);
  if(!ChargePolicy.counts(paid,n) || ChargePolicy.counts(refunded,n) || ChargePolicy.counts(disputed,n))
    throw new AssertionError("Bad reversible payment policy");
  if(!future.equals(ChargePolicy.paidThrough(List.of(paid,refunded,disputed),n)))
    throw new AssertionError("Bad paid-through calculation");
  System.out.println("Pure Java reversible entitlement test passed");
 }
}''')
    subprocess.run(['javac','-d',tmp,str(policy),str(path)],check=True)
    subprocess.run(['java','-cp',tmp,'PolicyHarness'],check=True)
print('PASS: Build 07+08 security, cloud, privacy and payment source contracts')
