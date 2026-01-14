ALTER TABLE signals ADD COLUMN tx_hash VARCHAR(66);
CREATE UNIQUE INDEX idx_signals_tx_hash_type ON signals(tx_hash, type);