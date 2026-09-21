-- V5__expand_account_schema.sql
-- Expand account_view and account_snapshot tables with new fields.

-- account_view: identification
ALTER TABLE account_view ADD COLUMN account_number VARCHAR(20);
ALTER TABLE account_view ADD COLUMN account_type VARCHAR(30) DEFAULT 'SAVINGS';
ALTER TABLE account_view ADD COLUMN cci VARCHAR(20);
ALTER TABLE account_view ADD COLUMN iban VARCHAR(34);
ALTER TABLE account_view ADD COLUMN swift_bic VARCHAR(11);

-- account_view: holder info
ALTER TABLE account_view ADD COLUMN holder_name VARCHAR(120);
ALTER TABLE account_view ADD COLUMN holder_document_type VARCHAR(30);
ALTER TABLE account_view ADD COLUMN holder_document_number VARCHAR(20);
ALTER TABLE account_view ADD COLUMN holder_email VARCHAR(255);
ALTER TABLE account_view ADD COLUMN holder_phone VARCHAR(20);

-- account_view: banking
ALTER TABLE account_view ADD COLUMN bank_code VARCHAR(10);
ALTER TABLE account_view ADD COLUMN bank_name VARCHAR(100);
ALTER TABLE account_view ADD COLUMN account_currency VARCHAR(3);
ALTER TABLE account_view ADD COLUMN country VARCHAR(2);

-- account_view: balance details
ALTER TABLE account_view ADD COLUMN available_amount DECIMAL(19,4) DEFAULT 0;
ALTER TABLE account_view ADD COLUMN available_amount_currency VARCHAR(3);
ALTER TABLE account_view ADD COLUMN hold_amount DECIMAL(19,4) DEFAULT 0;
ALTER TABLE account_view ADD COLUMN hold_amount_currency VARCHAR(3);
ALTER TABLE account_view ADD COLUMN overdraft_limit DECIMAL(19,4) DEFAULT 0;
ALTER TABLE account_view ADD COLUMN overdraft_limit_currency VARCHAR(3);

-- account_view: limits
ALTER TABLE account_view ADD COLUMN daily_limit DECIMAL(19,4);
ALTER TABLE account_view ADD COLUMN daily_limit_currency VARCHAR(3);
ALTER TABLE account_view ADD COLUMN monthly_limit DECIMAL(19,4);
ALTER TABLE account_view ADD COLUMN monthly_limit_currency VARCHAR(3);
ALTER TABLE account_view ADD COLUMN single_transaction_limit DECIMAL(19,4);
ALTER TABLE account_view ADD COLUMN single_transaction_limit_currency VARCHAR(3);

-- account_view: timestamps
ALTER TABLE account_view ADD COLUMN activated_at TIMESTAMP;
ALTER TABLE account_view ADD COLUMN closed_at TIMESTAMP;

-- account_snapshot: identification
ALTER TABLE account_snapshot ADD COLUMN account_number VARCHAR(20);
ALTER TABLE account_snapshot ADD COLUMN account_type VARCHAR(30) DEFAULT 'SAVINGS';

-- account_snapshot: holder info
ALTER TABLE account_snapshot ADD COLUMN holder_name VARCHAR(120);
ALTER TABLE account_snapshot ADD COLUMN holder_document_type VARCHAR(30);
ALTER TABLE account_snapshot ADD COLUMN holder_document_number VARCHAR(20);

-- account_snapshot: banking
ALTER TABLE account_snapshot ADD COLUMN currency VARCHAR(3);
ALTER TABLE account_snapshot ADD COLUMN country VARCHAR(2);

-- account_snapshot: timestamps
ALTER TABLE account_snapshot ADD COLUMN activated_at TIMESTAMP;
ALTER TABLE account_snapshot ADD COLUMN closed_at TIMESTAMP;
