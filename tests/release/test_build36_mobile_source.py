"""Build36 mobile preview remains incapable of bypassing PKCE/DRM payments."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/"apps/mobile/android/app/src/main"
def test_deep_link_cannot_be_oidc_callback_or_secret_url():
    link=(BASE/"java/com/reelnest/catalogpreview/SafeCatalogLink.kt").read_text()
    activity=(BASE/"java/com/reelnest/catalogpreview/CatalogActivity.kt").read_text()
    manifest=(BASE/"AndroidManifest.xml").read_text()
    assert '"reelnest-preview"' in link and 'uri.host != "catalog"' in link
    assert "uri.rawQuery != null" in link and "uri.rawFragment != null" in link
    assert "SafeCatalogLink.titleId" in activity
    assert 'android:host="catalog"' in manifest
    assert 'android:scheme="reelnest-preview"' in manifest
    assert "oidc" not in link.lower() or "require" in link.lower()
def test_offline_preview_cannot_become_licensed_media():
    policy=(BASE/"java/com/reelnest/catalogpreview/OfflineRights.kt").read_text()
    assert "return false" in policy
    assert "nowMillis >= licenseUntil || nowMillis >= rightsUntil" in policy
    assert "isSymbolicLink" in policy and "purgePrivatePreviewFiles" in policy
    activity=(BASE/"java/com/reelnest/catalogpreview/CatalogActivity.kt").read_text()
    assert "Clear private offline preview files" in activity
    assert "requestPermissions" in activity and "setOnClickListener" in activity
