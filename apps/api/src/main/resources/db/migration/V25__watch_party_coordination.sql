-- Synchronized metadata only. Media entitlement is NEVER shared by a watch party.
CREATE TABLE watch_parties (
 id UUID PRIMARY KEY,
 host_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 host_profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
 title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
 invitation_digest CHAR(64) NOT NULL UNIQUE,
 state VARCHAR(12) NOT NULL DEFAULT 'ACTIVE' CHECK(state IN('ACTIVE','CLOSED')),
 paused BOOLEAN NOT NULL DEFAULT TRUE,
 position_seconds INT NOT NULL DEFAULT 0 CHECK(position_seconds BETWEEN 0 AND 86400),
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 expires_at TIMESTAMPTZ NOT NULL,
 CHECK(expires_at>created_at)
);
CREATE INDEX watch_party_expiry_idx ON watch_parties(expires_at) WHERE state='ACTIVE';
CREATE TABLE watch_party_members (
 party_id UUID NOT NULL REFERENCES watch_parties(id) ON DELETE CASCADE,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
 joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(party_id,user_id)
);
