# Argus - Database Schema

## Entity Relationship Diagram

```mermaid
erDiagram
    wallets ||--o{ transactions : has
    wallets ||--o{ signals : generates
    users ||--o{ alert_rules : owns

    wallets {
        uuid id PK
        varchar address UK
        varchar chain
        varchar label
        varchar type
        decimal total_pnl
        decimal win_rate
        timestamp first_seen_at
        timestamp last_activity_at
        timestamp created_at
    }

    tokens {
        varchar address PK
        varchar chain
        varchar symbol
        varchar name
        int decimals
        decimal market_cap
        decimal liquidity
        decimal risk_score
        timestamp created_at
    }

    transactions {
        uuid id PK
        uuid wallet_id FK
        varchar tx_hash
        varchar chain
        varchar type
        varchar token_in
        varchar token_out
        decimal amount_in
        decimal amount_out
        decimal usd_value
        bigint block_number
        timestamp tx_timestamp
    }

    signals {
        uuid id PK
        uuid wallet_id FK
        varchar type
        varchar token_address
        varchar token_symbol
        varchar chain
        decimal usd_value
        decimal confidence_score
        text ai_narrative
        jsonb metadata
        timestamp created_at
    }

    users {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar telegram_chat_id
        timestamp created_at
    }

    alert_rules {
        uuid id PK
        uuid user_id FK
        varchar name
        jsonb conditions
        jsonb channels
        boolean enabled
        timestamp created_at
    }
```

---

## Tables

| Table | Purpose |
|-------|---------|
| `wallets` | Tracked blockchain addresses |
| `tokens` | Token metadata (symbol, liquidity) |
| `transactions` | Wallet activity history |
| `signals` | Detected trading signals |
| `users` | Application users |
| `alert_rules` | User notification rules |

---

## Relationships

| From | To | Type |
|------|----|------|
| `wallets` | `transactions` | 1:N |
| `wallets` | `signals` | 1:N |
| `users` | `alert_rules` | 1:N |

---

## Indexes

| Table | Index | Columns |
|-------|-------|---------|
| wallets | idx_wallets_address | address |
| wallets | idx_wallets_type | type |
| transactions | idx_transactions_wallet | wallet_id |
| transactions | idx_transactions_timestamp | tx_timestamp |
| signals | idx_signals_type | type |
| signals | idx_signals_created | created_at |
