-- Build 06: recommendation signals and bounded, deduplicated product events.
CREATE TABLE viewer_events (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
 title_id UUID REFERENCES titles(id) ON DELETE SET NULL,
 event_type VARCHAR(30) NOT NULL CHECK(event_type IN ('IMPRESSION','DETAIL_VIEW','PLAY_START','PLAY_COMPLETE','SEARCH','WATCHLIST_ADD')),
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_viewer_events_profile ON viewer_events(profile_id,occurred_at DESC);
CREATE INDEX idx_viewer_events_title_type ON viewer_events(title_id,event_type,occurred_at DESC);
CREATE INDEX idx_viewer_events_type_recent ON viewer_events(event_type,occurred_at DESC);
CREATE TABLE recommendation_runs (
 id UUID PRIMARY KEY,
 model_version VARCHAR(80) NOT NULL,
 evaluated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 metrics JSONB NOT NULL DEFAULT '{}'::jsonb
);
