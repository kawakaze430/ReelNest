-- A human-facing support decision is NOT a payment provider refund.
CREATE TABLE refund_review_requests (
 id UUID PRIMARY KEY,
 support_case_id UUID NOT NULL UNIQUE REFERENCES support_cases(id) ON DELETE RESTRICT,
 requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 approved_by UUID REFERENCES users(id) ON DELETE RESTRICT,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
   CHECK(status IN ('PENDING','APPROVED_FOR_MANUAL_REVIEW','REJECTED')),
 reason VARCHAR(1000) NOT NULL,
 reviewed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CHECK(approved_by IS NULL OR approved_by <> requested_by)
);
CREATE TABLE retention_legal_holds (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 reason VARCHAR(1000) NOT NULL CHECK(length(btrim(reason)) >= 20),
 expires_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 released_at TIMESTAMPTZ
);
CREATE INDEX idx_retention_holds_active ON retention_legal_holds(user_id,expires_at)
 WHERE released_at IS NULL;
