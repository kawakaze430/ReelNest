-- Build 12: preserve privacy request evidence without keeping the erased account.
-- Only previously unpaid, non-creator users are eligible for automated deletion.
ALTER TABLE privacy_requests DROP CONSTRAINT privacy_requests_user_id_fkey;
ALTER TABLE privacy_requests ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE privacy_requests ADD CONSTRAINT privacy_requests_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE identity_tombstones (
  subject_digest CHAR(64) PRIMARY KEY,
  deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_privacy_requests_unresolved
  ON privacy_requests(status,requested_at) WHERE status IN ('REQUESTED','IN_REVIEW');
