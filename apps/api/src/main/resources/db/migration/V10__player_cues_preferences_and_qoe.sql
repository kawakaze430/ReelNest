-- Build 15: per-profile playback preferences, editorial skip markers, opt-in QoE.
-- Do not store access tokens, IP addresses, request URLs, user-agent strings or free-text telemetry.
CREATE TABLE player_preferences (
  profile_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
  speed NUMERIC(3,2) NOT NULL DEFAULT 1.00 CHECK(speed IN (0.50,0.75,1.00,1.25,1.50,2.00)),
  caption_size VARCHAR(8) NOT NULL DEFAULT 'normal' CHECK(caption_size IN ('small','normal','large')),
  caption_contrast VARCHAR(8) NOT NULL DEFAULT 'standard' CHECK(caption_contrast IN ('standard','high')),
  preferred_audio VARCHAR(16) NOT NULL DEFAULT '',
  preferred_subtitles VARCHAR(16) NOT NULL DEFAULT '',
  data_saver BOOLEAN NOT NULL DEFAULT FALSE,
  qoe_consent BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT player_language_format CHECK (
    preferred_audio ~ '^($|[a-z]{2,3}(-[a-z]{2,8})?)$' AND
    preferred_subtitles ~ '^($|[a-z]{2,3}(-[a-z]{2,8})?)$'
  )
);

CREATE TABLE media_cues (
  media_asset_id UUID PRIMARY KEY REFERENCES media_assets(id) ON DELETE CASCADE,
  intro_start_seconds INTEGER,
  intro_end_seconds INTEGER,
  recap_start_seconds INTEGER,
  recap_end_seconds INTEGER,
  credits_start_seconds INTEGER,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT valid_intro CHECK ((intro_start_seconds IS NULL AND intro_end_seconds IS NULL)
    OR (intro_start_seconds IS NOT NULL AND intro_end_seconds IS NOT NULL AND
        intro_start_seconds >= 0 AND intro_end_seconds > intro_start_seconds)),
  CONSTRAINT valid_recap CHECK ((recap_start_seconds IS NULL AND recap_end_seconds IS NULL)
    OR (recap_start_seconds IS NOT NULL AND recap_end_seconds IS NOT NULL AND
        recap_start_seconds >= 0 AND recap_end_seconds > recap_start_seconds)),
  CONSTRAINT valid_credits CHECK (credits_start_seconds IS NULL OR credits_start_seconds >= 0)
);

CREATE TABLE playback_qoe_events (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES playback_sessions(id) ON DELETE CASCADE,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  event_type VARCHAR(20) NOT NULL CHECK (event_type IN
    ('STARTUP','STALL','QUALITY_SWITCH','PLAYER_ERROR','RECOVERY')),
  elapsed_ms INTEGER CHECK (elapsed_ms BETWEEN 0 AND 3600000),
  quality_height INTEGER CHECK (quality_height BETWEEN 1 AND 4320),
  error_code VARCHAR(32) CHECK (error_code ~ '^[A-Z0-9_]{1,32}$'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qoe_session ON playback_qoe_events(session_id,created_at DESC);
CREATE INDEX idx_qoe_profile ON playback_qoe_events(profile_id,created_at DESC);
CREATE INDEX idx_qoe_retention ON playback_qoe_events(created_at);
