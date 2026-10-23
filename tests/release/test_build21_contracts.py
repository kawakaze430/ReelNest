"""Build 21 offline contracts. These cannot replace Spring/Postgres integration."""
from pathlib import Path
import csv
ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/'apps/api/src/main/java/com/reelnest'
MIG=ROOT/'apps/api/src/main/resources/db/migration'

def source(path):return (JAVA/path).read_text()

def test_migrations_contiguous_and_no_sql_drop():
    versions=sorted(int(p.name.split('__')[0][1:]) for p in MIG.glob('V*__*.sql'))
    assert len(versions)>=20 and versions==list(range(1,max(versions)+1))
    for p in MIG.glob('V1[7-9]__*.sql'):
        assert 'DROP TABLE' not in p.read_text().upper()

def test_pin_is_slow_hashed_and_delete_requires_server_challenge():
    policy=source('profiles/ProfilePinPolicy.java')
    assert 'PBKDF2WithHmacSHA256' in policy and '210_000' in policy
    assert 'pin_failed_attempts' in source('profiles/ProfilePinController.java')
    assert 'requireForSensitiveAction' in source('profiles/ProfileController.java')

def test_bulk_import_requires_distinct_approver_and_checks_row_snapshot():
    batch=source('admin/CatalogBatchController.java')
    assert 'CatalogBatchPolicy.canApprove' in batch
    assert 'CatalogBatchPolicy.canApply' in batch
    assert 'snapshot(parsed.rows()).equals(b.fingerprint())' in batch
    assert "status='DRAFT'" in batch
    assert '@PreAuthorize("hasRole(\'ADMIN\')")' in batch

def test_scheduling_checks_media_and_revokes_sessions_on_expiry():
    worker=source('admin/PublicationScheduler.java')
    assert "state='SCHEDULED'" in worker
    assert "processing_status='READY'" in worker
    assert "status='REVOKED'" in worker
    assert 'system_publication_events' in worker

def test_refund_review_never_calls_payment_provider():
    code=source('support/RefundReviewController.java')
    assert 'RefundReviewPolicy.mayApprove' in code
    assert 'APPROVED_FOR_MANUAL_REVIEW' in code
    assert 'RazorpayGateway' not in code

def test_aliases_enforce_server_side_rights_territory():
    code=source('catalog/TitleAliasController.java')
    assert 'trustedCountry' in code
    assert 'rt.country_code=?' in code
    assert "status='APPROVED'" in code
    assert "processing_status='READY'" in code

def test_personalization_opt_out_disables_ml_and_deletes_view_events():
    controller=source('profiles/PersonalizationController.java')
    discovery=source('discovery/DiscoveryService.java')
    assert 'DELETE FROM viewer_events WHERE profile_id=?' in controller
    assert 'profile.personalized()?ml.rerank' in discovery
    assert 'if(!p.personalized())return' in discovery

def test_retention_holds_exempt_events_and_erasure():
    retention=source('discovery/EventRetentionJob.java')
    erasure=source('privacy/PrivacyErasureService.java')
    assert retention.count('retention_legal_holds')>=2
    assert 'retention_legal_holds' in erasure

def test_282_unique_tasks_all_classified():
    with (ROOT/'docs/BUILD21_282_TASKS.csv').open(newline='') as f:rows=list(csv.DictReader(f))
    assert len(rows)==282 and len({r['task_id'] for r in rows})==282
    assert all(r['acceptance_verified']=='NO' for r in rows)
