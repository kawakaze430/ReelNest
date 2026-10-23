-- Build 07: every paid cycle is tied to a specific verified charge; reversible entitlement.
ALTER TABLE billing_transactions
  ADD COLUMN entitled_until TIMESTAMPTZ,
  ADD COLUMN refunded_paise INTEGER NOT NULL DEFAULT 0 CHECK (refunded_paise >= 0),
  ADD COLUMN dispute_open BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN last_reconciled_at TIMESTAMPTZ;
CREATE INDEX idx_billing_transaction_reconcile
  ON billing_transactions(last_reconciled_at,recorded_at) WHERE status='CHARGED';

-- Backfill is intentionally not guessed: previously captured payments must be reconciled
-- against provider records before their entitlements can be considered trustworthy.
UPDATE subscriptions SET paid_through=NULL WHERE paid_through IS NOT NULL;

-- Build 08: privacy requests require authenticated customer submission and audited handling.
CREATE TABLE privacy_requests (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  request_type VARCHAR(16) NOT NULL CHECK (request_type IN ('EXPORT','DELETE')),
  status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
    CHECK (status IN ('REQUESTED','IN_REVIEW','FULFILLED','REJECTED')),
  requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMPTZ,
  admin_note VARCHAR(500)
);
CREATE INDEX idx_privacy_requests_user ON privacy_requests(user_id,requested_at DESC);
CREATE UNIQUE INDEX idx_privacy_pending_delete ON privacy_requests(user_id)
  WHERE request_type='DELETE' AND status IN ('REQUESTED','IN_REVIEW');
