CREATE TABLE event_store (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(64),
    UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_event_store_aggregate ON event_store(aggregate_id, version);
