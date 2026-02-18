
CREATE INDEX IF NOT EXISTS idx_signals_token_created 
ON signals(token_address, created_at DESC);
