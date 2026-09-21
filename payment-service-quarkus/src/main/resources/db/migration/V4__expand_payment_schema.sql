-- V4__expand_payment_schema.sql
-- Expand payments table with new fields for production-grade payments.

-- Payment type
ALTER TABLE payments ADD COLUMN payment_type VARCHAR(30);

-- Beneficiary info
ALTER TABLE payments ADD COLUMN beneficiary_name VARCHAR(120);
ALTER TABLE payments ADD COLUMN beneficiary_document_type VARCHAR(30);
ALTER TABLE payments ADD COLUMN beneficiary_document_number VARCHAR(20);
ALTER TABLE payments ADD COLUMN beneficiary_account_number VARCHAR(20);
ALTER TABLE payments ADD COLUMN beneficiary_bank_code VARCHAR(10);
ALTER TABLE payments ADD COLUMN beneficiary_bank_name VARCHAR(100);

-- Sender info
ALTER TABLE payments ADD COLUMN sender_name VARCHAR(120);
ALTER TABLE payments ADD COLUMN sender_document_type VARCHAR(30);
ALTER TABLE payments ADD COLUMN sender_document_number VARCHAR(20);

-- Reference
ALTER TABLE payments ADD COLUMN reference VARCHAR(255);
ALTER TABLE payments ADD COLUMN external_reference VARCHAR(255);

-- Routing
ALTER TABLE payments ADD COLUMN channel VARCHAR(20);
ALTER TABLE payments ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE payments ADD COLUMN user_agent VARCHAR(500);

-- Processing
ALTER TABLE payments ADD COLUMN processed_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN failed_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(500);
ALTER TABLE payments ADD COLUMN retry_count INT DEFAULT 0;

-- Fees
ALTER TABLE payments ADD COLUMN fee_amount DECIMAL(19,4);
ALTER TABLE payments ADD COLUMN fee_currency VARCHAR(3);
