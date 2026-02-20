-- Add Supabase UID column (Supabase manages auth, so we link via their user ID)
ALTER TABLE users ADD COLUMN supabase_uid VARCHAR(36) UNIQUE;

-- Remove password_hash since Supabase owns authentication
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;

-- Index for fast lookup by Supabase UID
CREATE INDEX idx_users_supabase_uid ON users(supabase_uid);
