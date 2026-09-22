-- V6__widen_account_columns.sql
-- Widen columns that are too short for real-world data.

-- bank_code: SWIFT 8-11, routing numbers 9+, was VARCHAR(10)
ALTER TABLE account_view ALTER COLUMN bank_code TYPE VARCHAR(20);

-- bank_name: some bank names are long
ALTER TABLE account_view ALTER COLUMN bank_name TYPE VARCHAR(150);
