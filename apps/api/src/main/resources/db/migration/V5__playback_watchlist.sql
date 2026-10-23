-- Build 04: private entitlement-based playback. No public media object access.
ALTER TABLE titles ADD COLUMN access_tier VARCHAR(12) NOT NULL DEFAULT 'FREE'
  CHECK (access_tier IN ('FREE','PREMIUM'));

CREATE TABLE playback_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
  media_asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT,
  country_code CHAR(2) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ENDED','REVOKED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_heartbeat_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  last_progress_seq BIGINT NOT NULL DEFAULT 0 CHECK (last_progress_seq >= 0),
  CONSTRAINT expiry_after_create CHECK (expires_at > created_at)
);
CREATE INDEX idx_playback_user_active ON playback_sessions(user_id,expires_at)
  WHERE status = 'ACTIVE';
CREATE INDEX idx_playback_profile ON playback_sessions(profile_id,created_at DESC);

ALTER TABLE watch_progress ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE watch_progress ADD COLUMN last_session_id UUID REFERENCES playback_sessions(id) ON DELETE SET NULL;
CREATE INDEX idx_watch_resume ON watch_progress(profile_id, updated_at DESC);

CREATE TABLE watchlists (
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(profile_id,title_id)
);
CREATE INDEX idx_watchlists_recent ON watchlists(profile_id, created_at DESC);
