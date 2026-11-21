CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    transaction_date DATE NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transactions_category ON transactions (category);
CREATE INDEX idx_transactions_date ON transactions (transaction_date);
CREATE INDEX idx_transactions_type ON transactions (transaction_type);
