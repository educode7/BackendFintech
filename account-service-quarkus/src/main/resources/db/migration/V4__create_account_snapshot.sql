CREATE TABLE account_snapshot (
    account_id       VARCHAR(64) PRIMARY KEY,
    user_id          VARCHAR(64) NOT NULL,
    balance_amount   NUMERIC(19,4) NOT NULL,
    balance_currency CHAR(3) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0
);
