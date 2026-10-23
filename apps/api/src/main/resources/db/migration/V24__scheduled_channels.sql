-- FAST-style editorial schedule, not a live broadcast or automatic playback entitlement.
CREATE TABLE programmed_channels (
 id UUID PRIMARY KEY,
 slug VARCHAR(90) NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
 name VARCHAR(120) NOT NULL,
 description VARCHAR(500) NOT NULL DEFAULT '',
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),
 created_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE channel_programs (
 id UUID PRIMARY KEY,
 channel_id UUID NOT NULL REFERENCES programmed_channels(id) ON DELETE CASCADE,
 title_id UUID NOT NULL REFERENCES titles(id) ON DELETE RESTRICT,
 starts_at TIMESTAMPTZ NOT NULL,
 ends_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT positive_program_window CHECK (starts_at < ends_at),
 CONSTRAINT program_duration CHECK (ends_at <= starts_at + INTERVAL '24 hours')
);
CREATE INDEX channel_guide_window ON channel_programs(channel_id,starts_at,ends_at);
