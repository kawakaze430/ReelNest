-- Build 16: explicit series-to-episode mapping. Each episode is a separately licensed,
-- published playable title; mapping never confers access or copies rights.
CREATE TABLE series_episodes (
  series_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
  episode_title_id UUID NOT NULL UNIQUE REFERENCES titles(id) ON DELETE CASCADE,
  season_number INTEGER NOT NULL CHECK(season_number BETWEEN 1 AND 100),
  episode_number INTEGER NOT NULL CHECK(episode_number BETWEEN 1 AND 500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(series_id,season_number,episode_number),
  CHECK(series_id <> episode_title_id)
);
CREATE INDEX idx_series_episodes_order ON series_episodes(series_id,season_number,episode_number);
ALTER TABLE player_preferences ADD COLUMN autoplay_next BOOLEAN NOT NULL DEFAULT FALSE;
