-- Make tx_timestamp nullable since we don't always have block timestamp immediately
ALTER TABLE transactions ALTER COLUMN tx_timestamp DROP NOT NULL;

-- Add comment
COMMENT ON COLUMN transactions.tx_timestamp IS 'Transaction timestamp from block (nullable until block data is fetched)';
