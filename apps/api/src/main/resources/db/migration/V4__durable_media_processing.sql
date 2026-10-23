-- Build 03: completed source ingestion atomically schedules durable processing.
-- Retry state lives in PostgreSQL and is polled with SKIP LOCKED by a separate worker.
ALTER TABLE media_assets ADD COLUMN processing_details JSONB;
CREATE UNIQUE INDEX idx_media_assets_source_key_unique ON media_assets(source_key);

CREATE TABLE media_processing_jobs (
  id UUID PRIMARY KEY,
  media_asset_id UUID NOT NULL UNIQUE REFERENCES media_assets(id) ON DELETE RESTRICT,
  state VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
      CHECK (state IN ('QUEUED','RUNNING','RETRY_WAIT','SUCCEEDED','FAILED','REJECTED')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  max_attempts INTEGER NOT NULL DEFAULT 3 CHECK (max_attempts BETWEEN 1 AND 10),
  worker_id VARCHAR(150),
  lease_until TIMESTAMPTZ,
  available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_error VARCHAR(800),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT lease_consistency CHECK (state <> 'RUNNING' OR (worker_id IS NOT NULL AND lease_until IS NOT NULL))
);
CREATE INDEX idx_media_jobs_claim ON media_processing_jobs(state, available_at, created_at)
  WHERE state IN ('QUEUED','RETRY_WAIT','RUNNING');

-- Upgrade existing Build 02 databases without losing pending creator uploads.
UPDATE media_assets SET processing_status='QUEUED'
WHERE processing_status='AWAITING_INSPECTION';
INSERT INTO media_processing_jobs(id,media_asset_id,state,available_at)
SELECT gen_random_uuid(),m.id,'QUEUED',NOW()
FROM media_assets m
JOIN upload_intents u ON u.object_key=m.source_key AND u.status='COMPLETED'
WHERE m.processing_status='QUEUED'
ON CONFLICT (media_asset_id) DO NOTHING;
