CREATE TABLE wallet_stats (
    id              BIGSERIAL PRIMARY KEY,
    wallet_address  VARCHAR(42) NOT NULL,
    token_address   VARCHAR(42) NOT NULL,
    token_symbol    VARCHAR(20),

    total_bought    DECIMAL(38, 18) DEFAULT 0,
    total_sold      DECIMAL(38, 18) DEFAULT 0,
    cost_basis_usd  DECIMAL(20, 2) DEFAULT 0,
    proceeds_usd    DECIMAL(20, 2) DEFAULT 0,

    realized_pnl    DECIMAL(20, 2) DEFAULT 0,
    avg_buy_price   DECIMAL(20, 8) DEFAULT 0,
    avg_sell_price  DECIMAL(20, 8) DEFAULT 0,
    roi_percent     DECIMAL(10, 4) DEFAULT 0,
    is_profitable   BOOLEAN DEFAULT FALSE,

    first_tx_at     TIMESTAMP,
    last_tx_at      TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(wallet_address, token_address)
);
CREATE INDEX idx_wallet_stats_wallet ON wallet_stats(wallet_address);
CREATE INDEX idx_wallet_stats_profitable ON wallet_stats(wallet_address, is_profitable);
COMMENT ON TABLE wallet_stats IS 'Per-token trading statistics for each wallet';
COMMENT ON COLUMN wallet_stats.realized_pnl IS 'Profit/loss from closed positions only';
COMMENT ON COLUMN wallet_stats.roi_percent IS '(realized_pnl / cost_of_sold) * 100';