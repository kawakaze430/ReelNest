-- Creator-supplied SRT captions. Draft-only and owner-authorized at the API.
-- Source text is retained privately for packaging/retries, never publicly exposed.
CREATE TABLE title_caption_sources (
  title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
  language VARCHAR(16) NOT NULL CHECK(language ~ '^[a-z]{2,3}(-[a-z]{2,8})?$'),
  srt TEXT NOT NULL CHECK(octet_length(srt)<=200000 AND length(srt)>0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(title_id,language)
);
