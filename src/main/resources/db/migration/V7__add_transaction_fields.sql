-- Add new columns to transactions table for blockchain transaction data
ALTER TABLE transactions
ADD COLUMN from_address VARCHAR(42),
ADD COLUMN to_address VARCHAR(42),
ADD COLUMN value_eth DECIMAL(30, 18),
ADD COLUMN input TEXT;

-- Add comments for documentation
COMMENT ON COLUMN transactions.from_address IS 'Sender Ethereum address';
COMMENT ON COLUMN transactions.to_address IS 'Recipient Ethereum address (null for contract creation)';
COMMENT ON COLUMN transactions.value_eth IS 'ETH value transferred (converted from Wei)';
COMMENT ON COLUMN transactions.input IS 'Transaction input data (contract call data)';
