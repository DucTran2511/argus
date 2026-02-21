CREATE TABLE address_labels (
    id              BIGSERIAL PRIMARY KEY,
    address         VARCHAR(42) NOT NULL,
    label           VARCHAR(100) NOT NULL,
    category        VARCHAR(50),
    source          VARCHAR(50) DEFAULT 'manual',
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(address, label)
);
CREATE INDEX idx_address_labels_address ON address_labels(address);
CREATE INDEX idx_address_labels_label ON address_labels(lower(label));
CREATE INDEX idx_address_labels_category ON address_labels(category);