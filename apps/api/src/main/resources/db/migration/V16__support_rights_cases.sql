-- Authenticated customer support, rights complaints and linked appeals.
CREATE TABLE support_cases (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 title_id UUID REFERENCES titles(id) ON DELETE RESTRICT,
 parent_case_id UUID REFERENCES support_cases(id) ON DELETE RESTRICT,
 category VARCHAR(24) NOT NULL CHECK(category IN ('TECHNICAL','BILLING','ACCOUNT','CREATOR','RIGHTS_COMPLAINT','APPEAL','OTHER')),
 subject VARCHAR(160) NOT NULL,
 description VARCHAR(4000) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','IN_REVIEW','RESOLVED','REJECTED')),
 resolution VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 closed_at TIMESTAMPTZ,
 CHECK ((category='APPEAL' AND parent_case_id IS NOT NULL) OR (category<>'APPEAL' AND parent_case_id IS NULL)),
 CHECK (category NOT IN ('RIGHTS_COMPLAINT') OR title_id IS NOT NULL)
);
CREATE UNIQUE INDEX idx_support_unique_appeal ON support_cases(parent_case_id) WHERE parent_case_id IS NOT NULL;
CREATE INDEX idx_support_owner_recent ON support_cases(user_id,created_at DESC);
CREATE INDEX idx_support_status_recent ON support_cases(status,created_at DESC);
CREATE INDEX idx_support_title ON support_cases(title_id,created_at DESC) WHERE title_id IS NOT NULL;
CREATE TABLE support_case_events (
 id UUID PRIMARY KEY,
 case_id UUID NOT NULL REFERENCES support_cases(id) ON DELETE CASCADE,
 actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 event_type VARCHAR(32) NOT NULL CHECK(event_type IN ('CREATED','REVIEW_STARTED','RESOLVED','REJECTED','TITLE_UNPUBLISHED')),
 note VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_support_case_events ON support_case_events(case_id,created_at,id);
