-- Add user_id to wallets
ALTER TABLE wallets ADD COLUMN user_id UUID REFERENCES users(id);

-- Drop unique constraint on address and make it unique per user
ALTER TABLE wallets DROP CONSTRAINT IF EXISTS wallets_address_key;
ALTER TABLE wallets ADD CONSTRAINT wallets_user_address_key UNIQUE(user_id, address);

-- Index for fast lookup
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
