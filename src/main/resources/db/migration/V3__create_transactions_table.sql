CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID REFERENCES wallets(id) ON DELETE CASCADE,
    tx_hash VARCHAR(66) NOT NULL,
    chain VARCHAR(20) NOT NULL DEFAULT 'ethereum',
    type VARCHAR(20),
    token_in VARCHAR(66),
    token_out VARCHAR(66),
    amount_in DECIMAL(30, 18),
    amount_out DECIMAL(30, 18),
    usd_value DECIMAL(20, 2),
    block_number BIGINT,
    gas_used BIGINT,
    gas_price DECIMAL(30, 18),
    tx_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_transactions_wallet ON transactions(wallet_id);
CREATE INDEX idx_transactions_tx_hash ON transactions(tx_hash);
CREATE INDEX idx_transactions_timestamp ON transactions(tx_timestamp);
CREATE INDEX idx_transactions_type ON transactions(type);
