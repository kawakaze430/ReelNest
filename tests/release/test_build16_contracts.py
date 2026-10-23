"""Source/migration contracts only; these do not replace live PostgreSQL/Keycloak tests."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def read(path):
    return (ROOT/path).read_text(encoding='utf-8')


def test_series_mapping_is_unique_and_restricts_positions():
    sql=read('apps/api/src/main/resources/db/migration/V11__series_and_autoplay.sql')
    assert 'episode_title_id UUID NOT NULL UNIQUE' in sql
    assert 'PRIMARY KEY(series_id,season_number,episode_number)' in sql
    assert 'CHECK(series_id <> episode_title_id)' in sql
    assert 'autoplay_next BOOLEAN NOT NULL DEFAULT FALSE' in sql


def test_episode_link_admin_only_and_same_creator():
    java=read('apps/api/src/main/java/com/reelnest/catalog/SeriesController.java')
    assert '@PreAuthorize("hasRole(\'ADMIN\')")' in java
    assert 'series.getCreatorId().equals(episode.getCreatorId())' in java
    assert '"PUBLISHED".equals(episode.getStatus())' in java
    assert 'DuplicateKeyException' in java


def test_next_episode_rechecks_entitlement():
    service=read('apps/api/src/main/java/com/reelnest/playback/PlaybackService.java')
    assert 'eligibleAsset(next.titleId(),profile.child(),user)' in service
    assert 'Profile profile=requireProfile(user,profileId)' in service
    assert 'ORDER BY n.season_number,n.episode_number LIMIT 1' in service


def test_previews_use_private_session_media_not_public_bucket():
    player=read('apps/web/src/components/protected-player.tsx')
    assert 'Authorization:`Bearer ${token}`' in player
    assert 'URL.revokeObjectURL' in player
    assert 'previewUrl(created.manifestUrl' in player
    assert 'prefs.autoplayNext' in player


def test_caption_converter_not_misrepresented_as_ingest():
    converter=read('apps/video-worker/timed_text.py')
    assert 'MAX_BYTES = 200_000' in converter
    assert 'start < previous_end' in converter
