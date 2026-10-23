-- Curated catalog rails: editorial metadata only, never an entitlement or playback grant.
CREATE TABLE editorial_collections (
 id UUID PRIMARY KEY,
 slug VARCHAR(90) NOT NULL UNIQUE CHECK(slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
 heading VARCHAR(100) NOT NULL,
 description VARCHAR(500) NOT NULL DEFAULT '',
 language VARCHAR(8),
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK(status IN('DRAFT','PUBLISHED','ARCHIVED')),
 sort_position INT NOT NULL DEFAULT 100 CHECK(sort_position BETWEEN 0 AND 10000),
 starts_at TIMESTAMPTZ,
 ends_at TIMESTAMPTZ,
 created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT collection_dates CHECK(ends_at IS NULL OR starts_at IS NULL OR ends_at>starts_at)
);
CREATE TABLE editorial_collection_items (
 collection_id UUID NOT NULL REFERENCES editorial_collections(id) ON DELETE CASCADE,
 title_id UUID NOT NULL REFERENCES titles(id) ON DELETE RESTRICT,
 sort_position INT NOT NULL DEFAULT 100 CHECK(sort_position BETWEEN 0 AND 10000),
 PRIMARY KEY(collection_id,title_id)
);
CREATE INDEX editorial_collection_active_idx ON editorial_collections(status,sort_position) WHERE status='PUBLISHED';
CREATE INDEX editorial_collection_items_order_idx ON editorial_collection_items(collection_id,sort_position);
