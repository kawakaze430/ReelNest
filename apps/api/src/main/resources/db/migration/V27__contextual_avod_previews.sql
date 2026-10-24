-- Build36: consent-gated contextual advertisement planning, never a billable ad system.
CREATE TABLE avod_profile_preferences (
 profile_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
 contextual_previews_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
 anonymous_measurement_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE avod_creatives (
 id UUID PRIMARY KEY,
 advertiser VARCHAR(100) NOT NULL CHECK(length(btrim(advertiser)) BETWEEN 2 AND 100),
 message VARCHAR(180) NOT NULL CHECK(length(btrim(message)) BETWEEN 2 AND 180),
 genre VARCHAR(80),
 country_code CHAR(2) NOT NULL CHECK(country_code ~ '^[A-Z]{2}$'),
 starts_at TIMESTAMPTZ NOT NULL,
 ends_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
   CHECK(status IN ('DRAFT','REVIEWED','ARCHIVED')),
 submitted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT avod_window CHECK(ends_at>starts_at),
 CONSTRAINT avod_two_person_review CHECK(reviewed_by IS NULL OR reviewed_by<>submitted_by)
);
CREATE INDEX idx_avod_preview_active ON avod_creatives(country_code,starts_at,ends_at)
 WHERE status='REVIEWED';
-- Deliberately non-billable: no advertiser invoices or event-to-revenue join.
CREATE TABLE avod_preview_events (
 id UUID PRIMARY KEY,
 profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
 creative_id UUID NOT NULL REFERENCES avod_creatives(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT avod_preview_event_only_unique UNIQUE(profile_id,id)
);
CREATE INDEX idx_avod_preview_expiry ON avod_preview_events(created_at);
