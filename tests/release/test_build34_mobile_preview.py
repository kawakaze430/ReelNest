"""Read-only native app source contracts; requires Android/Xcode to prove compilation."""
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[2]

def test_android_client_only_fetches_public_catalog_without_credentials():
    folder=ROOT/"apps/mobile/android"
    gradle=(folder/"app/build.gradle.kts").read_text()
    src=(folder/"app/src/main/java/com/reelnest/catalogpreview/CatalogActivity.kt").read_text()
    manifest=(folder/"app/src/main/AndroidManifest.xml").read_text()
    assert "https://" in gradle and "reelnestApiOrigin" in gradle
    assert 'usesCleartextTraffic="false"' in manifest
    assert "android.permission.INTERNET" in manifest
    assert "LEANBACK_LAUNCHER" in manifest
    assert "HttpsURLConnection" in src
    assert "/api/v1/catalog?page=0&size=24" in src
    assert "instanceFollowRedirects=false" in src
    assert "Bearer" not in src and "/api/v1/playback" not in src

def test_ios_catalog_and_xcodegen_targets_do_not_enable_protected_media():
    root=ROOT/"apps/mobile/ios"
    project=yaml.safe_load((root/"project.yml").read_text())
    assert project["targets"]["ReelNestCatalogPreview"]["platform"]=="iOS"
    assert project["targets"]["ReelNestTVPreview"]["platform"]=="tvOS"
    src=(root/"Shared/CatalogScreen.swift").read_text()
    assert "URLSession.shared.data(for:request)" in src
    assert "/api/v1/catalog?page=0&size=24" in src
    assert "Streaming is disabled" in src
    assert "AVPlayer(" not in src
    assert "/api/v1/playback" not in src
