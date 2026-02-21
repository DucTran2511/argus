-- Add user_id to address_labels
ALTER TABLE address_labels ADD COLUMN user_id UUID REFERENCES users(id);

-- Drop old unique constraint (address, label)
ALTER TABLE address_labels DROP CONSTRAINT address_labels_address_label_key;

-- Add new unique constraint (user_id, address, label)
ALTER TABLE address_labels ADD CONSTRAINT address_labels_user_address_label_key UNIQUE(user_id, address, label);

-- Index for fast lookup
CREATE INDEX idx_address_labels_user_id ON address_labels(user_id);
