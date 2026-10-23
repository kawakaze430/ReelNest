-- A server-side cutoff invalidates earlier self-contained JWTs even while their exp remains valid.
-- This does not terminate the browser SSO session at Keycloak: that is an external provider action.
ALTER TABLE users ADD COLUMN token_valid_after TIMESTAMPTZ;
CREATE INDEX idx_playback_sessions_account_recent ON playback_sessions(user_id, created_at DESC);
