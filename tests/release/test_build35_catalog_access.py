"""Catalog public access must not disagree with actual playback rights checks."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/"apps/api/src/main/java/com/reelnest"
def test_every_public_catalog_entrypoint_requires_current_rights():
    repo=(BASE/"catalog/TitleRepository.java").read_text()
    ctl=(BASE/"catalog/CatalogController.java").read_text()
    for v in ["t.status='PUBLISHED'","c.status='APPROVED'",
              "r.status='APPROVED'","r.valid_from<=CURRENT_DATE",
              "r.valid_until>=CURRENT_DATE","rt.country_code=:country",
              "m.processing_status='READY'","m.manifest_key IS NOT NULL"]:
        assert v in repo,v
    for v in ["findPublicById","findPublicBySlug","searchPublic",
              "SearchTextPolicy.likeLiteral(term)"]:
        assert v in ctl,v
    assert "findByIdAndStatus" not in ctl
    assert "findBySlugAndStatus" not in ctl
    assert "PageRequest.of(page,size)" in ctl
def test_album_and_alias_suggestion_do_not_return_private_media_paths():
    search=(BASE/"catalog/TitleAliasController.java").read_text()
    assert "SearchTextPolicy.likeLiteral" in search
    assert "EXISTS(SELECT 1 FROM creators" in search
    assert "m.manifest_key IS NOT NULL" in search
