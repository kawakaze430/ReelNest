-- Build 17: creator-visible progress. A percentage represents pipeline milestones,
-- not a guaranteed wall-clock ETA; no private object keys are returned to creators.
ALTER TABLE media_processing_jobs
  ADD COLUMN progress_stage VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
    CHECK (progress_stage IN ('QUEUED','PROBING','ENCODING','UPLOADING','VERIFYING','READY','FAILED')),
  ADD COLUMN progress_percent SMALLINT NOT NULL DEFAULT 0
    CHECK (progress_percent BETWEEN 0 AND 100);
UPDATE media_processing_jobs SET
  progress_stage=CASE WHEN state='SUCCEEDED' THEN 'READY'
                      WHEN state IN ('FAILED','REJECTED') THEN 'FAILED'
                      WHEN state='RUNNING' THEN 'ENCODING'
                      ELSE 'QUEUED' END,
  progress_percent=CASE WHEN state='SUCCEEDED' THEN 100 ELSE 0 END;
