from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[2]
API=ROOT/'apps/api/src/main/java/com/reelnest'

def test_support_policy_java():
    done=subprocess.run(['bash','scripts/test_build20_case_policy_java.sh'],cwd=ROOT,capture_output=True,text=True)
    assert done.returncode==0,done.stdout+'\n'+done.stderr
    assert '17 assertions passed' in done.stdout

def test_case_migration_constraints():
    text=(ROOT/'apps/api/src/main/resources/db/migration/V16__support_rights_cases.sql').read_text()
    assert 'CREATE TABLE support_cases' in text
    assert 'CHECK(category IN' in text
    assert 'CREATE UNIQUE INDEX idx_support_unique_appeal' in text
    assert 'parent_case_id UUID REFERENCES support_cases(id) ON DELETE RESTRICT' in text
    assert 'CREATE TABLE support_case_events' in text

def test_case_scoping_review_and_rights():
    user=(API/'support/SupportCaseController.java').read_text()
    admin=(API/'support/AdminSupportController.java').read_text()
    assert 'WHERE id=? AND user_id=?' in user
    assert 'WHERE user_id=? ORDER BY created_at DESC' in user
    assert "hasAnyRole('ADMIN','MODERATOR')" in admin
    assert 'FOR UPDATE' in admin
    assert "status='UNPUBLISHED'" in admin
    assert "status='REVOKED'" in admin
    assert 'hasAdminRole(jwt)' in admin
    assert 'audit_logs' in admin

def test_case_privacy_and_ui():
    erasure=(API/'privacy/PrivacyErasureService.java').read_text()
    assert 'SELECT COUNT(*) FROM support_cases WHERE user_id=?' in erasure
    assert 'SELECT COUNT(*) FROM support_case_events WHERE actor_user_id=?' in erasure
    export=(API/'privacy/PrivacyController.java').read_text()
    assert '"supportCases"' in export
    assert (ROOT/'apps/web/src/components/support-case-center.tsx').exists()
    assert (ROOT/'apps/web/src/app/admin/support/page.tsx').exists()

def test_support_rate_limits():
    source=(API/'security/AdmissionRateLimiter.java').read_text()
    assert 'path.equals("/api/v1/support/cases")' in source
    assert '/appeal' in source


def test_catalog_export_scoping_and_sanitization():
    export=(API/'admin/CatalogExportController.java').read_text()
    assert "hasRole('ADMIN')" in export
    assert 'LIMIT 10000' in export
    assert 'CACHE_CONTROL' in export
    assert 'CatalogCsvPolicy.cell' in export
    policy=(API/'admin/CatalogCsvPolicy.java').read_text()
    assert 'stripLeading()' in policy
    assert 'value.replace' in policy
