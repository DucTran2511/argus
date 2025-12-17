# Argus Database - Deep Guide

## Database Overview

Your database has **6 business tables** + 1 Flyway tracking table:

```
┌─────────────────────────────────────────────────────────────────┐
│                      ARGUS DATABASE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CORE DOMAIN                        USER DOMAIN                 │
│  ┌──────────────┐                  ┌──────────────┐            │
│  │   wallets    │                  │    users     │            │
│  └──────┬───────┘                  └──────┬───────┘            │
│         │                                 │                     │
│    ┌────┴────┐                           │                     │
│    │         │                           │                     │
│    ▼         ▼                           ▼                     │
│ ┌──────┐ ┌──────┐                  ┌──────────┐               │
│ │trans │ │signal│                  │alert_rule│               │
│ └──────┘ └──────┘                  └──────────┘               │
│                                                                 │
│  REFERENCE DATA                    SYSTEM                       │
│  ┌──────────────┐                  ┌──────────────────────┐    │
│  │    tokens    │                  │ flyway_schema_history│    │
│  └──────────────┘                  └──────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Table-by-Table Deep Dive

### 1. `wallets` - The Core Entity

**Purpose:** Stores blockchain wallet addresses you're tracking (whales, VCs, smart money)

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Unique identifier |
| address | VARCHAR(66) | Wallet address (0x... for ETH) |
| chain | VARCHAR(20) | 'ethereum', 'bsc', 'arbitrum' |
| label | VARCHAR(100) | Human name: "Wintermute", "Jump Trading" |
| type | VARCHAR(50) | 'whale', 'vc', 'dex_trader', 'influencer' |
| total_pnl | DECIMAL | Cumulative profit/loss in USD |
| win_rate | DECIMAL | Success rate (0.00 - 1.00) |
| first_seen_at | TIMESTAMP | When first tracked |
| last_activity_at | TIMESTAMP | Most recent transaction |

**Why it matters:**
- This is the **heart of Argus** - everything revolves around wallets
- You track wallets, their transactions, and signals they generate

---

### 2. `transactions` - Activity Log

**Purpose:** Records every trade/transfer made by tracked wallets

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Unique identifier |
| wallet_id | UUID (FK) | Links to wallets.id |
| tx_hash | VARCHAR(66) | Blockchain transaction hash |
| chain | VARCHAR(20) | Which blockchain |
| type | VARCHAR(20) | 'swap', 'transfer', 'mint', 'bridge' |
| token_in | VARCHAR(66) | Token address received |
| token_out | VARCHAR(66) | Token address sent |
| amount_in | DECIMAL | Amount received |
| amount_out | DECIMAL | Amount sent |
| usd_value | DECIMAL | Transaction value in USD |
| block_number | BIGINT | Block when tx was mined |
| tx_timestamp | TIMESTAMP | When it happened |

**Example:**
```
wallet_id: abc123 (Jump Trading)
type: swap
token_in: 0xUSDC...
token_out: 0xPEPE...
usd_value: 500000  → Jump bought $500K of PEPE
```

---

### 3. `signals` - Trading Intelligence

**Purpose:** Detected trading signals (the value Argus creates)

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Unique identifier |
| type | VARCHAR(50) | Signal type |
| wallet_id | UUID (FK) | Which wallet triggered it |
| token_address | VARCHAR(66) | Which token |
| token_symbol | VARCHAR(20) | 'PEPE', 'ARB', etc. |
| usd_value | DECIMAL | Transaction size |
| confidence_score | DECIMAL | 0.0 - 1.0 confidence |
| ai_narrative | TEXT | AI-generated explanation |
| metadata | JSONB | Extra data |

**Signal types:**

| Type | Meaning |
|------|---------|
| `whale_buy` | Large wallet bought token |
| `whale_sell` | Large wallet sold token |
| `accumulation` | Multiple wallets buying same token |
| `distribution` | Smart money exiting |
| `new_token_entry` | Wallet bought newly launched token |
| `unusual_volume` | Abnormal trading activity |

---

### 4. `tokens` - Token Reference Data

**Purpose:** Metadata about tokens (cached to avoid repeated API calls)

| Column | Type | Description |
|--------|------|-------------|
| address | VARCHAR(66) | Token contract address (PK) |
| chain | VARCHAR(20) | Which blockchain |
| symbol | VARCHAR(20) | 'ETH', 'PEPE' |
| name | VARCHAR(100) | 'Ethereum', 'Pepe' |
| decimals | INTEGER | Token decimals (usually 18) |
| market_cap | DECIMAL | Current market cap |
| liquidity | DECIMAL | DEX liquidity |
| risk_score | DECIMAL | 0.0 (safe) - 1.0 (risky) |

---

### 5. `users` - Application Users

**Purpose:** People who use Argus and receive alerts

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Unique identifier |
| email | VARCHAR(255) | Email (unique) |
| password_hash | VARCHAR(255) | Hashed password |
| telegram_chat_id | VARCHAR(50) | For Telegram alerts |

---

### 6. `alert_rules` - User Notification Preferences

**Purpose:** Custom rules for when to notify users

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Unique identifier |
| user_id | UUID (FK) | Links to users.id |
| name | VARCHAR(100) | "My Whale Alerts" |
| conditions | JSONB | Rule conditions |
| channels | JSONB | Where to send |
| enabled | BOOLEAN | On/Off toggle |

**Example `conditions`:**
```json
{
  "signal_types": ["whale_buy", "accumulation"],
  "min_usd_value": 50000,
  "wallet_types": ["whale", "vc"]
}
```

**Example `channels`:**
```json
{
  "telegram": true,
  "email": false
}
```

---

## Relationships

```
wallets (1) ──────> (N) transactions
   │                     "A wallet has many transactions"
   │
   └────────────> (N) signals
                       "A wallet generates many signals"

users (1) ────────> (N) alert_rules
                       "A user has many alert rules"

tokens (standalone)
   "Referenced by address, not foreign key"
```

---

## Data Flow

```
1. BlockMonitorJob detects transaction
   └─▶ INSERT INTO transactions

2. SignalDetector analyzes transaction
   └─▶ INSERT INTO signals (if pattern detected)

3. AlertProcessor matches signal to rules
   └─▶ Query alert_rules WHERE conditions MATCH

4. NotificationService sends alert
   └─▶ Telegram message to user
```

---

## Key Design Decisions

| Decision | Reason |
|----------|--------|
| UUID primary keys | Globally unique, no collisions |
| JSONB for conditions | Flexible, queryable, extensible |
| Separate tokens table | Cache frequently used data |
| Soft relationships | tokens isn't FK'd - addresses can exist before we know the token |
