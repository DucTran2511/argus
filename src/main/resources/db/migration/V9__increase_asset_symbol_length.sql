ALTER TABLE asset_transfers 
ALTER COLUMN asset_symbol TYPE VARCHAR(100);

COMMENT ON COLUMN asset_transfers.asset_symbol IS 'Token symbol (e.g., ETH, USDC, or custom long names up to 100 chars)';
