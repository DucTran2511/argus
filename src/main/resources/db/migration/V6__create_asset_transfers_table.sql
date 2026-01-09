CREATE TABLE asset_transfers (
    id              BIGSERIAL PRIMARY KEY,
    wallet_address  VARCHAR(42) NOT NULL,
    tx_hash         VARCHAR(66) NOT NULL,
    block_number    BIGINT,
    from_address    VARCHAR(42) NOT NULL,
    to_address      VARCHAR(42),
    category        VARCHAR(20) NOT NULL,
    value           NUMERIC(38, 18),
    asset_symbol    VARCHAR(20),
    token_address   VARCHAR(42),
    log_index       INTEGER,
    tx_timestamp    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    
    -- Unique constraint to prevent duplicates
    CONSTRAINT uq_asset_transfer UNIQUE (tx_hash, wallet_address, category, log_index)
);
-- Indexes for performance
CREATE INDEX idx_asset_transfers_wallet ON asset_transfers(wallet_address);
CREATE INDEX idx_asset_transfers_timestamp ON asset_transfers(tx_timestamp DESC);
CREATE INDEX idx_asset_transfers_hash ON asset_transfers(tx_hash);
-- Comments for documentation
COMMENT ON TABLE asset_transfers IS 'Stores historical asset transfers (ETH, ERC20) for tracked wallets';
COMMENT ON COLUMN asset_transfers.category IS 'Transfer type: external, internal, erc20, erc721, erc1155';