-- V047: Add slug column to spaces table for human-readable URLs
-- Backfills slugs from existing space names and enforces org-level uniqueness

ALTER TABLE spaces ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- Backfill slug from name for existing spaces
UPDATE spaces
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL;

-- Strip leading/trailing hyphens (edge case: names like "!hello!")
UPDATE spaces
SET slug = REGEXP_REPLACE(REGEXP_REPLACE(slug, '^-+', ''), '-+$', '')
WHERE slug ~ '^-|--' OR slug ~ '-$';

-- Ensure slug is not empty (fallback to id prefix for degenerate names)
UPDATE spaces
SET slug = CONCAT('space-', SUBSTRING(id::text, 1, 8))
WHERE slug IS NULL OR slug = '';

-- Make not null
ALTER TABLE spaces ALTER COLUMN slug SET NOT NULL;

-- Enforce uniqueness per org
ALTER TABLE spaces
    ADD CONSTRAINT spaces_slug_org_unique UNIQUE (org_id, slug);
