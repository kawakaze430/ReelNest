-- Build 02: rights-first creator ingestion. Existing seeded titles intentionally retain NULL creator_id.
CREATE TABLE creators (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
  display_name VARCHAR(100) NOT NULL,
  legal_name VARCHAR(160) NOT NULL,
  contact_email VARCHAR(320) NOT NULL,
  portfolio_url VARCHAR(500),
  application_statement VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED')),
  review_note VARCHAR(1000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reviewed_at TIMESTAMPTZ
);
CREATE INDEX idx_creators_status ON creators(status, created_at);

ALTER TABLE titles ADD COLUMN creator_id UUID REFERENCES creators(id) ON DELETE RESTRICT;
CREATE INDEX idx_titles_creator ON titles(creator_id, created_at DESC);

CREATE TABLE content_rights (
  id UUID PRIMARY KEY,
  title_id UUID NOT NULL UNIQUE REFERENCES titles(id) ON DELETE CASCADE,
  creator_id UUID NOT NULL REFERENCES creators(id) ON DELETE RESTRICT,
  rights_basis VARCHAR(30) NOT NULL
    CHECK (rights_basis IN ('OWNER','LICENSED_DISTRIBUTOR')),
  proof_reference VARCHAR(1000) NOT NULL,
  agreement_notes VARCHAR(2000) NOT NULL,
  valid_from DATE NOT NULL,
  valid_until DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  review_note VARCHAR(1000),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reviewed_at TIMESTAMPTZ,
  CONSTRAINT rights_valid_interval CHECK (valid_until > valid_from)
);
CREATE TABLE rights_territories (
  rights_id UUID NOT NULL REFERENCES content_rights(id) ON DELETE CASCADE,
  country_code CHAR(2) NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
  PRIMARY KEY (rights_id, country_code)
);

CREATE TABLE upload_intents (
  id UUID PRIMARY KEY,
  title_id UUID NOT NULL REFERENCES titles(id) ON DELETE RESTRICT,
  creator_id UUID NOT NULL REFERENCES creators(id) ON DELETE RESTRICT,
  object_key TEXT NOT NULL UNIQUE,
  provider_upload_id TEXT NOT NULL,
  original_filename VARCHAR(240) NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  expected_bytes BIGINT NOT NULL CHECK (expected_bytes BETWEEN 1 AND 5368709120),
  part_size_bytes BIGINT NOT NULL,
  part_count INTEGER NOT NULL CHECK (part_count BETWEEN 1 AND 10000),
  status VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
    CHECK (status IN ('INITIATED','COMPLETING','COMPLETED','ABORTED','FAILED')),
  expires_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_upload_intents_active ON upload_intents(title_id) WHERE status IN ('INITIATED','COMPLETING');
CREATE INDEX idx_upload_intents_title ON upload_intents(title_id, created_at DESC);
CREATE INDEX idx_upload_intents_creator ON upload_intents(creator_id, created_at DESC);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  actor_user_id UUID NOT NULL REFERENCES users(id),
  action VARCHAR(100) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id UUID NOT NULL,
  detail VARCHAR(1000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
