-- Build 22: private notification inbox. No external email/push is sent by this migration.
CREATE TABLE notification_preferences (
 user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 email_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
 product_updates_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
 changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE user_notifications (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 kind VARCHAR(40) NOT NULL CHECK (kind IN ('RIGHTS_EXPIRY','CREATOR_REVIEW','SYSTEM')),
 title VARCHAR(150) NOT NULL,
 body VARCHAR(500) NOT NULL,
 dedupe_key VARCHAR(220) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 read_at TIMESTAMPTZ,
 UNIQUE(user_id,dedupe_key)
);
CREATE INDEX idx_user_notifications_inbox ON user_notifications(user_id,created_at DESC);
CREATE INDEX idx_user_notifications_unread ON user_notifications(user_id,created_at DESC) WHERE read_at IS NULL;
-- Short-retained operational sampling snapshots (not per-user analytics exports).
CREATE TABLE analytics_quality_checks (
 id UUID PRIMARY KEY,
 check_name VARCHAR(60) NOT NULL,
 severity VARCHAR(12) NOT NULL CHECK (severity IN ('INFO','WARN','ERROR')),
 observed_count BIGINT NOT NULL CHECK (observed_count >= 0),
 checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_analytics_quality_time ON analytics_quality_checks(checked_at DESC);
