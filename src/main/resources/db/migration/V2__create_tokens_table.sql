CREATE TABLE tokens (
    address VARCHAR(66) PRIMARY KEY,
    chain VARCHAR(20) NOT NULL DEFAULT 'ethereum',
    symbol VARCHAR(20),
    name VARCHAR(100),
    decimals INTEGER DEFAULT 18,
    market_cap DECIMAL(30, 2),
    liquidity DECIMAL(30, 2),
    risk_score DECIMAL(3, 2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tokens_symbol ON tokens(symbol);
