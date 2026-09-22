-- V5__widen_bank_code_column.sql
-- Widen beneficiary_bank_code to support SWIFT (8-11), routing numbers (9+), and other bank codes.

ALTER TABLE payments ALTER COLUMN beneficiary_bank_code TYPE VARCHAR(20);
