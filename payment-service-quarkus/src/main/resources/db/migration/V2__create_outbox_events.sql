-- V2__create_outbox_events.sql
-- Transactional outbox table for guaranteed event delivery

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(64),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_unpublished ON outbox_events(created_at ASC) WHERE published = FALSE;
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
