-- Build 22: promotional-price PREVIEWS ONLY. Checkout still charges configured provider plan amount.
-- A preview must never unlock content, promise a discount, or issue a refund.
CREATE TABLE promotional_quotes (
 code VARCHAR(16) PRIMARY KEY CHECK (code ~ '^[A-Z0-9]{6,16}$'),
 plan_code VARCHAR(24) NOT NULL REFERENCES subscription_plans(code),
 percent_off SMALLINT NOT NULL CHECK (percent_off BETWEEN 1 AND 50),
 starts_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT promo_time CHECK(expires_at>starts_at)
);
