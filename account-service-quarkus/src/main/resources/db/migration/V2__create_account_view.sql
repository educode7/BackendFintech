-- V2__create_account_view.sql
CREATE TABLE account_view (
    account_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    balance_amount NUMERIC(19,4) NOT NULL,
    balance_currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_view_user_id ON account_view(user_id);
