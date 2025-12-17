# On-Chain Alpha Hunter 🐋

A crypto intelligence platform that tracks smart money movements on-chain and extracts trading signals using your distributed job scheduler as the pipeline engine.

---

## Overview

**Goal**: Build a platform that monitors blockchain activity (wallet movements, DEX trades, token flows) and uses AI to identify profitable opportunities before they become mainstream.

**Your Stack Integration**:
- **Distributed Job Scheduler**: Orchestrates all data collection, analysis, and alerting jobs
- **Spring Boot**: Core API layer
- **LangChain4j / Spring AI**: AI-powered narrative generation and pattern recognition
- **React**: Real-time dashboard

---

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ARGUS                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐         ┌─────────────┐                                  │
│   │     api/    │         │    job/     │                                  │
│   │ Controllers │         │  Scheduler  │                                  │
│   └──────┬──────┘         └──────┬──────┘                                  │
│          │                       │                                          │
│          └───────────┬───────────┘                                          │
│                      ▼                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                         domain/                                      │  │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │  │
│   │  │   model/    │  │  service/   │  │    port/    │                  │  │
│   │  │   Wallet    │  │  Detector   │  │ Interfaces  │                  │  │
│   │  └─────────────┘  └─────────────┘  └──────┬──────┘                  │  │
│   └───────────────────────────────────────────┼─────────────────────────┘  │
│                                               │                             │
│          ┌────────────────────────────────────┘                             │
│          ▼                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                         infra/                                       │  │
│   │  ┌────────────┐ ┌────────────┐ ┌────────────────┐                   │  │
│   │  │ blockchain │ │     ai     │ │  notification  │                   │  │
│   │  │   Web3j    │ │ LangChain  │ │   Telegram     │                   │  │
│   │  └────────────┘ └────────────┘ └────────────────┘                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Project Structure (Package-Based Modular Monolith)

```
argus/
├── pom.xml                           # Single Maven file
├── src/main/java/com/argus/
│   │
│   ├── core/                         # Shared utilities
│   │   ├── exception/                # Custom exceptions
│   │   └── util/                     # DateUtils, etc.
│   │
│   ├── domain/                       # PURE BUSINESS LOGIC (THE CORE)
│   │   ├── model/                    # Wallet, Signal, Token entities
│   │   ├── service/                  # SignalDetector, WalletAnalyzer
│   │   └── port/                     # Interfaces ONLY
│   │       ├── BlockchainPort.java
│   │       ├── AiPort.java
│   │       ├── NotificationPort.java
│   │       └── PersistencePort.java
│   │
│   ├── infra/                        # External system adapters
│   │   ├── blockchain/               # Web3j implementation
│   │   │   └── Web3jBlockchainAdapter.java
│   │   ├── ai/                       # LangChain4j implementation
│   │   │   └── LangChainAiAdapter.java
│   │   ├── notification/             # Telegram, Email
│   │   │   └── TelegramNotificationAdapter.java
│   │   └── persistence/              # JPA repositories
│   │       └── JpaWalletRepository.java
│   │
│   ├── api/                          # REST controllers
│   │   ├── WalletController.java
│   │   └── SignalController.java
│   │
│   ├── job/                          # Scheduled jobs
│   │   ├── BlockMonitorJob.java
│   │   └── WalletScannerJob.java
│   │
│   └── ArgusApplication.java         # Main entry point
│
├── src/main/resources/
│   └── application.yml
│
└── src/test/java/com/argus/
    ├── domain/                       # Unit tests (mock ports)
    └── integration/                  # Integration tests
```

### Dependency Rules (Strict)

```
IMPORT RULES - YOU ENFORCE THESE:
───────────────────────────────────────────────────────────────
domain/     → NEVER imports from infra/, api/, job/
infra/      → imports domain/port/ (to implement interfaces)
api/        → imports domain/service/
job/        → imports domain/service/
───────────────────────────────────────────────────────────────
```

**Key Principle**: The `domain/` package has NO dependencies on external libraries like Web3j or LangChain4j. It only defines interfaces (`Port`s) that `infra/` packages implement.

---

## Tech Stack

| Layer | Technology | Package |
|-------|------------|---------|
| **Job Orchestration** | Your Distributed Scheduler | job/ |
| **Backend Framework** | Spring Boot 3.5.x | root |
| **Core Business Logic** | Pure Java | domain/ |
| **Blockchain** | Web3j | infra/blockchain/ |
| **AI/LLM** | LangChain4j + OpenAI/Ollama | infra/ai/ |
| **Notifications** | Telegram Bot API, JavaMail | infra/notification/ |
| **Persistence** | Spring Data JPA | infra/persistence/ |
| **Message Queue** | Apache Kafka | job/ |
| **Database** | PostgreSQL + TimescaleDB | - |
| **Cache** | Redis | - |
| **Frontend** | React + TypeScript | separate project |


---

## Core Pipeline Jobs

### 1. Ingestion Jobs (Your Scheduler)

| Job Name | Frequency | Description |
|----------|-----------|-------------|
| `dex-trade-monitor` | Real-time (WebSocket) | Subscribe to DEX trades via Alchemy/QuickNode |
| `whale-wallet-scanner` | Every 1 min | Check tracked wallets for new transactions |
| `new-token-detector` | Every 5 min | Detect new token deployments |
| `cex-flow-tracker` | Every 5 min | Track major exchange inflows/outflows |
| `liquidity-monitor` | Every 15 min | Track liquidity additions/removals |
| `price-sync` | Every 1 min | Sync prices from DEX aggregators |

### 2. Analysis Jobs

| Job Name | Trigger | Description |
|----------|---------|-------------|
| `wallet-classifier` | On new wallet discovery | Classify wallet type (VC, whale, bot, retail) |
| `pnl-calculator` | Daily + on-demand | Calculate wallet P&L and win rate |
| `cluster-detector` | Weekly | Identify related wallets (same entity) |
| `accumulation-detector` | On trade events | Detect accumulation/distribution patterns |
| `copy-trade-signal` | On whale trade | Generate copy trade opportunities |

### 3. AI Jobs

| Job Name | Trigger | Description |
|----------|---------|-------------|
| `narrative-generator` | On significant event | "Why is this whale buying X?" |
| `pattern-matcher` | On new signal | "This looks like the PEPE setup in March" |
| `risk-scorer` | On token analysis | Rug pull probability, liquidity risk |
| `weekly-digest` | Weekly (Sunday) | AI-generated weekly alpha report |

### 4. Alert Jobs

| Job Name | Trigger | Description |
|----------|---------|-------------|
| `telegram-alerter` | On signal match | Send to user's Telegram |
| `email-digest` | Configurable | Email summary of alerts |
| `webhook-dispatcher` | On signal | Call user's webhook |

---

## Database Schema (Simplified)

### Core Tables

```sql
-- Tracked wallets with labels
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    address VARCHAR(66) NOT NULL UNIQUE,
    chain VARCHAR(20) NOT NULL,
    label VARCHAR(100),
    type VARCHAR(50), -- 'whale', 'vc', 'insider', 'mev_bot', 'retail'
    total_pnl DECIMAL(20, 8),
    win_rate DECIMAL(5, 4),
    first_seen_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    metadata JSONB
);

-- Wallet transactions
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID REFERENCES wallets(id),
    tx_hash VARCHAR(66) NOT NULL,
    chain VARCHAR(20) NOT NULL,
    type VARCHAR(20), -- 'swap', 'transfer', 'lp_add', 'lp_remove'
    token_in VARCHAR(66),
    token_out VARCHAR(66),
    amount_in DECIMAL(30, 18),
    amount_out DECIMAL(30, 18),
    usd_value DECIMAL(20, 2),
    block_number BIGINT,
    timestamp TIMESTAMP NOT NULL
);

-- Trading signals
CREATE TABLE signals (
    id UUID PRIMARY KEY,
    type VARCHAR(50), -- 'whale_buy', 'accumulation', 'new_token', 'vc_activity'
    wallet_id UUID REFERENCES wallets(id),
    token_address VARCHAR(66),
    token_symbol VARCHAR(20),
    chain VARCHAR(20),
    confidence_score DECIMAL(3, 2),
    ai_narrative TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- User watchlists
CREATE TABLE watchlists (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    name VARCHAR(100),
    wallets UUID[], -- Array of wallet IDs
    tokens VARCHAR(66)[], -- Array of token addresses
    created_at TIMESTAMP DEFAULT NOW()
);

-- Alert rules
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    name VARCHAR(100),
    conditions JSONB, -- {"wallet_type": "whale", "min_usd": 100000, "tokens": [...]}
    channels JSONB, -- {"telegram": "chat_id", "webhook": "url"}
    enabled BOOLEAN DEFAULT true
);
```

### TimescaleDB (Time-Series)

```sql
-- Token prices (hypertable)
CREATE TABLE token_prices (
    time TIMESTAMPTZ NOT NULL,
    token_address VARCHAR(66) NOT NULL,
    chain VARCHAR(20) NOT NULL,
    price_usd DECIMAL(30, 18),
    volume_24h DECIMAL(30, 2),
    liquidity DECIMAL(30, 2)
);
SELECT create_hypertable('token_prices', 'time');

-- Wallet activity aggregates
CREATE TABLE wallet_daily_stats (
    time TIMESTAMPTZ NOT NULL,
    wallet_id UUID NOT NULL,
    trade_count INTEGER,
    volume_usd DECIMAL(30, 2),
    realized_pnl DECIMAL(30, 2)
);
SELECT create_hypertable('wallet_daily_stats', 'time');
```

---

## MVP Feature Set (Phase 1)

### Must Have (4-6 weeks)
1. **Wallet Tracking**
   - Add/remove wallets to watchlist
   - Real-time transaction feed for tracked wallets
   - Basic wallet labels (manual)

2. **Signal Detection**
   - Whale buy/sell alerts (> $50k trades)
   - New token detection
   - Large liquidity movements

3. **Dashboard**
   - Live signal feed
   - Wallet explorer (view any wallet's transactions)
   - Basic charts (price, volume)

4. **Alerts**
   - Telegram bot integration
   - Simple alert rules (wallet + min amount)

### Nice to Have (Phase 2)
- AI wallet classification
- AI narrative generation
- Copy trade simulation
- Multi-chain support (Solana, Base)
- Advanced pattern detection

---

## Development Phases

### Phase 1: Foundation (Weeks 1-2)
- [ ] Project setup (Spring Boot, PostgreSQL, Redis)
- [ ] Integrate your job scheduler
- [ ] Basic blockchain data ingestion (1 chain - Ethereum)
- [ ] Wallet CRUD API
- [ ] Simple React dashboard skeleton

### Phase 2: Core Pipeline (Weeks 3-4)
- [ ] DEX trade monitoring job
- [ ] Whale wallet scanning job
- [ ] Transaction storage and indexing
- [ ] Signal detection logic
- [ ] Real-time WebSocket updates

### Phase 3: User Features (Weeks 5-6)
- [ ] User authentication
- [ ] Watchlist management
- [ ] Alert rule configuration
- [ ] Telegram bot integration
- [ ] Dashboard polish

### Phase 4: AI Integration (Weeks 7-8)
- [ ] LangChain4j setup
- [ ] AI narrative generation
- [ ] Pattern matching
- [ ] Risk scoring

---

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Primary Chain** | Ethereum first | Most whale activity, mature tools (Web3j) |
| **RPC Provider** | Alchemy / QuickNode | Reliable, good WebSocket support |
| **Real-time Strategy** | Kafka + WebSocket | Scale + browser updates |
| **AI Model** | GPT-4o via OpenAI | Best reasoning, switch to Ollama later |
| **Time-series** | TimescaleDB | PostgreSQL compatible, mature |

---

## Verification Plan

Since this is a **new project**, verification will be:

### During Development
1. **Unit Tests**: JUnit 5 for services
2. **Integration Tests**: Testcontainers for DB + Kafka
3. **Manual Testing**: 
   - Track a known whale wallet (e.g., Vitalik's wallet)
   - Verify transactions appear in real-time
   - Test Telegram alerts

### Acceptance Criteria for MVP
- [ ] Can add a wallet and see its transactions within 2 minutes
- [ ] Whale trades > $50k generate alerts
- [ ] Telegram bot sends alerts correctly
- [ ] Dashboard shows live signal feed

---

## Questions for You

1. **Which chain to start with?** Ethereum (more whales) or Solana (faster, memecoin action)?

2. **RPC Provider**: Do you have any existing provider (Alchemy, Infura, QuickNode)?

3. **Telegram vs Discord**: Which platform for alerts?

4. **AI Depth in MVP**: Include AI narratives in Phase 1 or defer to Phase 2?

5. **Self-hosted or Cloud**: Planning to deploy on your own server or cloud (AWS/GCP)?
