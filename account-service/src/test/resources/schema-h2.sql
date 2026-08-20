CREATE TABLE IF NOT EXISTS event_store (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_data TEXT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS account_view (
    account_id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    balance_amount NUMERIC(19,4) NOT NULL,
    balance_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_id ON event_store(aggregate_id);
