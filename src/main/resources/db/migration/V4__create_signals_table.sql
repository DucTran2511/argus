CREATE TABLE signals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    wallet_id UUID REFERENCES wallets(id) ON DELETE SET NULL,
    token_address VARCHAR(66),
    token_symbol VARCHAR(20),
    chain VARCHAR(20) NOT NULL DEFAULT 'ethereum',
    usd_value DECIMAL(20, 2),
    confidence_score DECIMAL(3, 2),
    ai_narrative TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_signals_type ON signals(type);
CREATE INDEX idx_signals_wallet ON signals(wallet_id);
CREATE INDEX idx_signals_token ON signals(token_address);
CREATE INDEX idx_signals_created ON signals(created_at);
