-- V3__add_published_to_event_store.sql
-- Add published flag for transactional outbox pattern

ALTER TABLE event_store ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE event_store ADD COLUMN published_at TIMESTAMPTZ;

CREATE INDEX idx_event_store_unpublished ON event_store(created_at ASC) WHERE published = FALSE;
