-- Restrict sensitive adult-profile actions; PINs are slow-hashed with a unique salt.
ALTER TABLE profiles ADD COLUMN pin_salt BYTEA;
ALTER TABLE profiles ADD COLUMN pin_hash BYTEA;
ALTER TABLE profiles ADD COLUMN pin_failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK(pin_failed_attempts >= 0);
ALTER TABLE profiles ADD COLUMN pin_locked_until TIMESTAMPTZ;
ALTER TABLE profiles ADD CONSTRAINT profile_pin_pair CHECK ((pin_hash IS NULL) = (pin_salt IS NULL));
CREATE TABLE catalog_import_batches (
 id UUID PRIMARY KEY,
 requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 approved_by UUID REFERENCES users(id) ON DELETE RESTRICT,
 payload TEXT NOT NULL CHECK (octet_length(payload) <= 100000),
 payload_sha256 CHAR(64) NOT NULL,
 base_fingerprint CHAR(64) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
   CHECK(status IN ('PENDING','APPROVED','APPLIED','REJECTED','EXPIRED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 expires_at TIMESTAMPTZ NOT NULL,
 approved_at TIMESTAMPTZ,
 applied_at TIMESTAMPTZ,
 CHECK(approved_by IS NULL OR approved_by <> requested_by)
);
CREATE INDEX idx_catalog_batches_pending ON catalog_import_batches(status,expires_at);
