CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address VARCHAR(66) NOT NULL UNIQUE,
    chain VARCHAR(20) NOT NULL DEFAULT 'ethereum',
    label VARCHAR(100),
    type VARCHAR(50),
    total_pnl DECIMAL(20, 8) DEFAULT 0,
    win_rate DECIMAL(5, 4) DEFAULT 0,
    first_seen_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_wallets_address ON wallets(address);
CREATE INDEX idx_wallets_type ON wallets(type);
