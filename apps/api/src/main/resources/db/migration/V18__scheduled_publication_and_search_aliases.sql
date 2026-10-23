-- Delayed publishing is a separate reviewed state, never an early PUBLISHED title.
CREATE TABLE title_publication_schedules (
 title_id UUID PRIMARY KEY REFERENCES titles(id) ON DELETE CASCADE,
 requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 publish_at TIMESTAMPTZ NOT NULL,
 state VARCHAR(15) NOT NULL DEFAULT 'SCHEDULED'
   CHECK(state IN ('SCHEDULED','PUBLISHED','CANCELLED','BLOCKED')),
 last_error VARCHAR(600),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_due_publication ON title_publication_schedules(publish_at) WHERE state='SCHEDULED';
-- Localized aliases are reviewable text, not an alternative rights or entitlement source.
CREATE TABLE title_search_aliases (
 title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
 language VARCHAR(12) NOT NULL,
 alias VARCHAR(160) NOT NULL CHECK(length(btrim(alias)) BETWEEN 2 AND 160),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(title_id,language,alias)
);
CREATE INDEX idx_title_alias_title ON title_search_aliases(title_id);
CREATE INDEX idx_title_alias_search ON title_search_aliases(lower(alias) text_pattern_ops);

CREATE TABLE system_publication_events (
 id UUID PRIMARY KEY,
 title_id UUID NOT NULL REFERENCES titles(id) ON DELETE RESTRICT,
 event_type VARCHAR(40) NOT NULL,
 detail VARCHAR(600),
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_system_publication_events ON system_publication_events(title_id,occurred_at DESC);
