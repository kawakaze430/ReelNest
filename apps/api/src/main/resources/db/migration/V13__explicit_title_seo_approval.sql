-- Search-engine indexing is opt-in, not inferred from publication or an approved
-- licence. Revoking rights/availability also removes the title from SEO queries.
ALTER TABLE titles ADD COLUMN seo_index_approved BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_titles_seo_approved ON titles(id) WHERE seo_index_approved=TRUE;
