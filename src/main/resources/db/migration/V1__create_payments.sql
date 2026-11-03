CREATE TABLE payments (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'BRL'),
    status VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
ALTER TABLE payments ADD CONSTRAINT chk_payments_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FAILED'));
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created ON payments(created_at);
