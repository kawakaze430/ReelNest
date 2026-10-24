-- Build 32: editor-approved chapters/scenes. Metadata never grants media entitlement.
-- Rows only attach to the immutable READY asset and are deleted with the asset.
CREATE TABLE media_scene_markers (
  media_asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
  ordinal SMALLINT NOT NULL CHECK (ordinal BETWEEN 1 AND 40),
  label VARCHAR(90) NOT NULL CHECK (length(btrim(label)) BETWEEN 2 AND 90),
  summary VARCHAR(240) NOT NULL DEFAULT '',
  start_seconds INTEGER NOT NULL CHECK (start_seconds BETWEEN 0 AND 86400),
  end_seconds INTEGER NOT NULL CHECK (end_seconds BETWEEN 1 AND 86400),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(media_asset_id,ordinal),
  CONSTRAINT scene_range CHECK (end_seconds>start_seconds),
  CONSTRAINT scene_summary_size CHECK (length(summary)<=240)
);
CREATE INDEX idx_media_scenes_asset_time ON media_scene_markers(media_asset_id,start_seconds);
