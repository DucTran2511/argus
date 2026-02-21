
ALTER TABLE asset_transfers 
ADD COLUMN price_at_tx DECIMAL(20,8),
ADD COLUMN price_source VARCHAR(20) DEFAULT 'current';
COMMENT ON COLUMN asset_transfers.price_at_tx IS 'Token price in USD at transaction time';
COMMENT ON COLUMN asset_transfers.price_source IS 'Source: current, coingecko, estimated';