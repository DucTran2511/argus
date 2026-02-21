ALTER TABLE wallet_metrics ADD COLUMN total_score DECIMAL(5, 2);
ALTER TABLE wallet_metrics ADD COLUMN tier VARCHAR(2);

CREATE INDEX idx_wallet_metrics_tier ON wallet_metrics(tier);
CREATE INDEX idx_wallet_metrics_total_score ON wallet_metrics(total_score DESC);
