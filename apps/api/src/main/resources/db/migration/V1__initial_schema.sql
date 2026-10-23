CREATE TABLE users (
  id UUID PRIMARY KEY,
  identity_subject VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(320) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE profiles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(40) NOT NULL,
  preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
  is_child BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_profiles_user ON profiles(user_id);
CREATE TABLE titles (
  id UUID PRIMARY KEY,
  slug VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  synopsis TEXT,
  type VARCHAR(20) NOT NULL CHECK(type IN ('MOVIE','SERIES')),
  genre VARCHAR(80) NOT NULL,
  language VARCHAR(12) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','IN_REVIEW','PUBLISHED','REJECTED','UNPUBLISHED')),
  age_rating VARCHAR(20) NOT NULL,
  runtime_minutes INTEGER CHECK (runtime_minutes > 0),
  release_year INTEGER,
  art_tone VARCHAR(40) NOT NULL,
  featured BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_titles_public ON titles(status, featured DESC, created_at DESC);
CREATE TABLE media_assets (
  id UUID PRIMARY KEY,
  title_id UUID NOT NULL REFERENCES titles(id),
  source_key TEXT NOT NULL,
  manifest_key TEXT,
  processing_status VARCHAR(30) NOT NULL,
  duration_seconds INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_media_title ON media_assets(title_id);
CREATE TABLE watch_progress (
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  media_asset_id UUID NOT NULL REFERENCES media_assets(id),
  position_seconds INTEGER NOT NULL DEFAULT 0 CHECK(position_seconds >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(profile_id, media_asset_id)
);
-- Rights, payment, and playback-session schema are deliberately deferred to later milestones.
-- Do not use the demo catalog as evidence of licensed content availability.
