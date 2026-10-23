-- Explicit per-profile recommendation consent. Existing profiles default to OFF.
ALTER TABLE profiles ADD COLUMN personalization_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE profiles ADD COLUMN personalization_updated_at TIMESTAMPTZ;
