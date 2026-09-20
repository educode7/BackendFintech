-- V3__add_account_id_to_payments.sql
-- Add account_id column to payments table (missing from V1)

ALTER TABLE payments ADD COLUMN account_id VARCHAR(64) NOT NULL DEFAULT '';
