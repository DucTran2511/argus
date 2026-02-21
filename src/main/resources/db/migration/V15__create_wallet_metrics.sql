-- V15: Create wallet_metrics table for smart money scoring
CREATE TABLE wallet_metrics (
    wallet_address VARCHAR(66) PRIMARY KEY,
    
    -- Classification
    archetype VARCHAR(50),
    is_blacklisted BOOLEAN DEFAULT FALSE,
    
    -- Raw metrics
    avg_position_size_usd DECIMAL(20, 2),
    trade_frequency_per_month DECIMAL(10, 2),
    trade_count_7d INTEGER,
    max_roi_percent DECIMAL(10, 2),
    profit_factor DECIMAL(10, 4),
    avg_hold_time_sec INTEGER,
    buy_vol_usd DECIMAL(20, 2),
    sell_vol_usd DECIMAL(20, 2),
    
    -- Scores (0-100)
    pnl_score DECIMAL(5, 2),
    consistency_score DECIMAL(5, 2),
    conviction_score DECIMAL(5, 2),
    
    -- Activity tracking
    last_trade_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Performance indexes for API queries
CREATE INDEX idx_wallet_metrics_archetype ON wallet_metrics(archetype);
CREATE INDEX idx_wallet_metrics_pnl_score ON wallet_metrics(pnl_score DESC);
CREATE INDEX idx_wallet_metrics_consistency_score ON wallet_metrics(consistency_score DESC);
CREATE INDEX idx_wallet_metrics_conviction_score ON wallet_metrics(conviction_score DESC);
CREATE INDEX idx_wallet_metrics_last_trade ON wallet_metrics(last_trade_at DESC);
CREATE INDEX idx_wallet_metrics_blacklist ON wallet_metrics(is_blacklisted);
