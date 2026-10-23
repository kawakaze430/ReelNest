-- Build 05: Razorpay subscriptions. Never derive paid access from frontend callbacks.
CREATE TABLE subscription_plans (
  code VARCHAR(24) PRIMARY KEY,
  label VARCHAR(80) NOT NULL,
  amount_paise INTEGER NOT NULL CHECK (amount_paise >= 0),
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  interval_months INTEGER NOT NULL DEFAULT 1 CHECK (interval_months>0),
  provider_plan_id VARCHAR(120) UNIQUE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO subscription_plans(code,label,amount_paise,currency,interval_months) VALUES
  ('FREE','Free',0,'INR',1),('STANDARD','Standard',14900,'INR',1),('PREMIUM','Premium',29900,'INR',1);
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  plan_code VARCHAR(24) NOT NULL REFERENCES subscription_plans(code),
  provider VARCHAR(30) NOT NULL DEFAULT 'RAZORPAY',
  provider_subscription_id VARCHAR(120) UNIQUE,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  current_period_end TIMESTAMPTZ,
  paid_through TIMESTAMPTZ, -- advances ONLY on verified successful subscription.charged event
  cancel_at_cycle_end BOOLEAN NOT NULL DEFAULT FALSE,
  provider_status VARCHAR(32),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id,created_at DESC);
CREATE INDEX idx_subscriptions_entitlement ON subscriptions(user_id,paid_through DESC);
CREATE TABLE payment_webhook_events (
  provider_event_id VARCHAR(140) PRIMARY KEY,
  event_name VARCHAR(120) NOT NULL,
  provider_subscription_id VARCHAR(120),
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  processed_at TIMESTAMPTZ,
  processing_error VARCHAR(400)
);
CREATE TABLE billing_transactions (
  id UUID PRIMARY KEY,
  subscription_id UUID NOT NULL REFERENCES subscriptions(id),
  provider_payment_id VARCHAR(120) UNIQUE,
  amount_paise INTEGER NOT NULL CHECK (amount_paise>=0),
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  status VARCHAR(24) NOT NULL,
  recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_billing_transactions_subscription ON billing_transactions(subscription_id,recorded_at DESC);
