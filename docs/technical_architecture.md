# Argus - Technical Architecture

## System Overview

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                                  ARGUS SYSTEM                                   │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│  ╔═══════════════════════════════════════════════════════════════════════════╗ │
│  ║                           EXTERNAL SERVICES                                ║ │
│  ║  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   ║ │
│  ║  │   Alchemy    │  │   OpenAI     │  │   Telegram   │  │  CoinGecko   │   ║ │
│  ║  │  (Eth RPC)   │  │   (LLM)      │  │   (Alerts)   │  │  (Prices)    │   ║ │
│  ║  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   ║ │
│  ╚═════════╪═════════════════╪═════════════════╪═════════════════╪═══════════╝ │
│            │                 │                 │                 │             │
│            ▼                 ▼                 ▼                 ▼             │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                         INFRA LAYER (Adapters)                          │  │
│  │  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐               │  │
│  │  │  blockchain/   │ │      ai/       │ │  notification/ │               │  │
│  │  │  Web3jAdapter  │ │ LangChainAdptr │ │ TelegramAdptr  │               │  │
│  │  └───────┬────────┘ └───────┬────────┘ └───────┬────────┘               │  │
│  └──────────┼──────────────────┼──────────────────┼────────────────────────┘  │
│             │                  │                  │                            │
│             │ implements       │ implements       │ implements                 │
│             ▼                  ▼                  ▼                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                         DOMAIN LAYER (Core)                             │  │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │  │
│  │  │                           port/                                   │  │  │
│  │  │  BlockchainPort    AiPort    NotificationPort    PersistencePort │  │  │
│  │  └──────────────────────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐              │  │
│  │  │    model/      │ │   service/     │ │    event/      │              │  │
│  │  │ Wallet, Signal │ │ SignalDetector │ │ SignalCreated  │              │  │
│  │  │ Token, Trade   │ │ WalletAnalyzer │ │ WhaleBuyEvent  │              │  │
│  │  └────────────────┘ └────────────────┘ └────────────────┘              │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│             ▲                  ▲                                              │
│             │                  │                                              │
│  ┌──────────┴──────────────────┴───────────────────────────────────────────┐  │
│  │                         APPLICATION LAYER                               │  │
│  │  ┌────────────────┐                    ┌────────────────┐               │  │
│  │  │      api/      │                    │      job/      │               │  │
│  │  │ WalletController                    │ BlockMonitorJob│               │  │
│  │  │ SignalController                    │ WalletScanJob  │               │  │
│  │  └───────┬────────┘                    └───────┬────────┘               │  │
│  └──────────┼─────────────────────────────────────┼────────────────────────┘  │
│             │                                     │                           │
│             ▼                                     ▼                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                         INFRASTRUCTURE                                  │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │  │
│  │  │  PostgreSQL  │  │    Redis     │  │    Kafka     │                  │  │
│  │  │ +TimescaleDB │  │   (Cache)    │  │  (Events)    │                  │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. Transaction Ingestion

```
Blockchain → BlockMonitorJob → BlockchainPort → Web3jAdapter
                                     │
                                     ▼
                              SignalDetector → Kafka "signals"
```

### 2. Alert Flow

```
Kafka "signals" → AlertProcessor → NotificationPort → TelegramAdapter → User
```

### 3. API Request

```
HTTP → WalletController → WalletService → PersistencePort → PostgreSQL
```

---

## Package Structure

```
com.argus/
├── core/                    # Utilities, exceptions
├── domain/                  # PURE BUSINESS LOGIC
│   ├── model/               # Wallet, Signal, Token
│   ├── service/             # SignalDetector, WalletAnalyzer
│   ├── port/                # Interfaces only
│   └── event/               # Domain events
├── infra/                   # Adapters
│   ├── blockchain/          # Web3jAdapter
│   ├── ai/                  # LangChainAdapter
│   ├── notification/        # TelegramAdapter
│   └── persistence/         # JPA repositories
├── api/                     # REST controllers
├── job/                     # Scheduled jobs
├── config/                  # Spring config
└── ArgusApplication.java
```

---

## Port Interfaces

```java
// domain/port/BlockchainPort.java
public interface BlockchainPort {
    BigInteger getLatestBlockNumber();
    List<Transaction> getBlockTransactions(BigInteger blockNumber);
    List<Transaction> getWalletTransactions(String address, int limit);
}

// domain/port/AiPort.java
public interface AiPort {
    String generateNarrative(Signal signal);
    RiskAssessment assessRisk(Token token);
}

// domain/port/NotificationPort.java
public interface NotificationPort {
    void sendAlert(Alert alert, String channel);
}

// domain/port/PersistencePort.java
public interface PersistencePort {
    Wallet saveWallet(Wallet wallet);
    Optional<Wallet> findByAddress(String address);
    void saveSignal(Signal signal);
}
```

---

## Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL + TimescaleDB | 5432 | Database |
| Redis | 6379 | Cache + Streams |
| Redis Commander | 8081 | Debug Redis |
| Argus API | 8080 | REST API |

---

## Redis Streams

| Stream | Producers | Consumers |
|--------|-----------|-----------|
| `argus:transactions` | BlockMonitorJob | TransactionProcessor |
| `argus:signals` | SignalDetector | AlertProcessor |

**Usage example:**
```bash
# Add to stream
XADD argus:signals * type whale_buy wallet 0x1234 amount 150000

# Read from stream (consumer group)
XREADGROUP GROUP processors worker1 STREAMS argus:signals >
```

---

## Database Schema

```
wallets (id, address, chain, label, type, pnl, win_rate)
    │
    └──▶ transactions (id, wallet_id, tx_hash, type, amount_usd, timestamp)
    │
    └──▶ signals (id, wallet_id, token_address, type, confidence, ai_narrative)

tokens (address, symbol, name, market_cap, liquidity, risk_score)

users (id, email, telegram_id)
    │
    └──▶ alert_rules (id, user_id, conditions, channels, enabled)
```
