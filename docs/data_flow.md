# Argus - Data Flow Documentation

> **Last Updated**: 2026-01-19 | **Status**: Living Document

This document describes the current data flows in the Argus system. Keep this updated as new features are added.

---

## System Overview

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                                 EXTERNAL SERVICES                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│  │   Alchemy   │    │ DexScreener │    │  Telegram   │    │  Ethereum   │            │
│  │  (RPC/API)  │    │  (Prices)   │    │   (future)  │    │ Blockchain  │            │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘            │
└─────────┼──────────────────┼──────────────────┼──────────────────┼───────────────────┘
          │                  │                  │                  │
          ▼                  ▼                  ▼                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                                   ARGUS SYSTEM                                        │
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                              API LAYER (api/)                                 │    │
│  │                                                                               │    │
│  │   WalletController        TransactionController       PriceController         │    │
│  │   /api/v1/wallets/*       /api/transactions/*         /api/v1/prices/*        │    │
│  │   AddressBookController   SmartMoneyController                                 │    │
│  │   /api/v1/address-book/*  /api/v1/smart-money/*                                │    │
│  └────────────────────────────────────┬──────────────────────────────────────────┘    │
│                                       │                                               │
│                                       ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                           DOMAIN LAYER (domain/)                              │    │
│  │                                                                               │    │
│  │   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │    │
│  │   │ WalletService   │  │TransactionSvc   │  │  PriceService   │              │    │
│  │   │ • CRUD wallets  │  │• Fetch & decode │  │• Token prices   │              │    │
│  │   │ • Auto-sync     │  │• Sync history   │  │• USD enrichment │              │    │
│  │   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │    │
│  │            │                    │                    │                        │    │
│  │   ┌────────┴────────┐  ┌────────┴────────┐  ┌────────┴────────┐              │    │
│  │   │WalletStatsSvc   │  │WhaleDetectorSvc │  │HistoricalImport │              │    │
│  │   │• PnL calculate  │  │• $50K threshold │  │• Price enrichment│              │    │
│  │   │• Win rate, ROI  │  │• BUY/SELL signal│  │• CoinGecko range │              │    │
│  │   │• Avg Cost Method│  │• Confidence tier│  │                  │              │    │
│  │   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │    │
│  │            │                    │                    │                        │    │
│  │   ┌────────┴────────────────────┴────────────────────┴────────────────────┐   │    │
│  │   │                              PORTS                                     │   │    │
│  │   │  BlockChainPort   DexDecoderPort   PricePort   StreamPublisher        │   │    │
│  │   │  WalletPersistencePort   TransactionPersistencePort                   │   │    │
│  │   │  SignalPersistencePort   TokenPersistencePort   BlockTrackingPort     │   │    │
│  │   │  AddressBookPersistencePort   WalletMetricsPersistencePort            │   │    │
│  │   │  WalletStatsPersistencePort   CachePort<K,V>                          │   │    │
│  │   └───────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                               │    │
│  │   ┌─────────────────────────────────────────────────────────────────────┐     │    │
│  │   │                          MODELS                                      │     │    │
│  │   │  Wallet  Transaction  Signal  Token  AssetTransfer  DecodedSwap     │     │    │
│  │   │  TokenPrice  WalletStats  WalletStatsSummary  SignalType            │     │    │
│  │   │  WalletMetrics  SmartMoneyArchetype                                  │     │    │
│  │   └─────────────────────────────────────────────────────────────────────┘     │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                       │                                               │
│                                       ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                            INFRA LAYER (infra/)                               │    │
│  │                                                                               │    │
│  │   blockchain/            cache/                persistence/                   │    │
│  │   ├─ Web3jAdapter        ├─ RedisBlockTracking ├─ WalletAdapter              │    │
│  │   ├─ DexDecoder          └─ RedisPriceCache    ├─ TransactionAdapter         │    │
│  │   └─ dto/ (Alchemy)                            ├─ SignalAdapter              │    │
│  │                                                ├─ TokenAdapter               │    │
│  │   price/                 stream/               └─ entity/ + repository/      │    │
│  │   └─ DexScreenerAdapter  ├─ RedisStreamPublisher                             │    │
│  │                          └─ consumer/                                         │    │
│  │                             └─ TransactionStreamConsumer                      │    │
│  │                                (→ WhaleDetectorService)                       │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                       │                                               │
│                                       ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                            JOB LAYER (job/)                                   │    │
│  │                                                                               │    │
│  │   BlockMonitorJob (@Scheduled every 12s)                                      │    │
│  │   • Fetch new blocks from Alchemy                                             │    │
│  │   • Filter transactions for tracked wallets                                   │    │
│  │   • Publish to Redis Stream → WhaleDetector                                   │    │
│  │   • Update block cursor in Redis                                              │    │
│  │                                                                               │    │
│  │   SmartMoneyRefreshJob (@Scheduled daily 2AM UTC)                             │    │
│  │   • Refresh all wallet metrics in batches                                     │    │
│  │   • Recalculate scores and archetypes                                         │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                       │                                               │
│                                       ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                           STORAGE LAYER                                       │    │
│  │                                                                               │    │
│  │   ┌─────────────────────────────┐    ┌─────────────────────────────────┐     │    │
│  │   │       PostgreSQL            │    │           Redis                  │     │    │
│  │   │                             │    │                                  │     │    │
│  │   │  • wallets                  │    │  • argus:block:cursor            │     │    │
│  │   │  • transactions             │    │  • argus:wallets:addresses       │     │    │
│  │   │  • asset_transfers          │    │  • argus:price:{tokenAddr}       │     │    │
│  │   │  • signals (WHALE_BUY/SELL) │    │  • argus:transactions (stream)   │     │    │
│  │   │  • tokens                   │    │  • argus:signals (stream-future) │     │    │
│  │   │  • address_labels (Day 24)  │    │                                  │     │    │
│  │   │  • wallet_metrics (Day 25)  │    │                                  │     │    │
│  │   │  • users (future)           │    │                                  │     │    │
│  │   │  • alert_rules (future)     │    │                                  │     │    │
│  │   └─────────────────────────────┘    └─────────────────────────────────┘     │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                       │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

### Architecture Summary

| Layer | Components | Responsibility |
|-------|------------|----------------|
| **API** | WalletController, TransactionController, PriceController | HTTP endpoints, request/response handling |
| **Domain** | WalletService, TransactionService, PriceService, WhaleDetectorService, **WalletStatsService** | Business logic, orchestration |
| **Ports** | BlockChainPort, *PersistencePort, WalletStatsPersistencePort, CachePort, StreamPublisher | Interfaces for external dependencies |
| **Infra** | Web3jAdapter, DexDecoder, Redis*, JPA Adapters | Implementations of ports |
| **Job** | BlockMonitorJob, PriceRetryJob, **SmartMoneyRefreshJob** | Scheduled blockchain monitoring, daily scoring |
| **Storage** | PostgreSQL (wallets, transactions, signals, wallet_stats, **wallet_metrics**), Redis | Data persistence, caching, event streaming |


### Key Data Flows (Current)

| # | Flow | Description |
|---|------|-------------|
| 1 | Wallet CRUD | API → WalletService → WalletPersistencePort → PostgreSQL |
| 2 | Transaction Lookup | API → TransactionService → BlockChainPort → Alchemy RPC |
| 3 | History Sync | API → TransactionService → Alchemy → enrich USD → PostgreSQL |
| 4 | Block Monitoring | Job → BlockChainPort → filter wallets → Redis Stream |
| 5 | Token Prices | API/Service → CachePort (Redis) → PricePort (DexScreener) |
| 6 | Redis Streams | BlockMonitorJob → publish → TransactionStreamConsumer |
| 7 | Whale Detection | Stream Consumer → WhaleDetectorService → SignalPersistencePort → signals table |
| 8 | Historical Import | API → HistoricalImportService → Alchemy + CoinGecko → enrich price_at_tx → PostgreSQL |
| 9 | Price Retry Job | PriceRetryJob (3 AM) → findByPriceSourceIn('missing') → CoinGecko → update DB |
| 10 | **Wallet Stats** | API → WalletStatsService → asset_transfers → calculate PnL → wallet_stats table |
| 11 | **Address Book** | API → AddressBookService → address_labels table (max 5 labels per address) |
| 12 | **Smart Money Scoring** | API → SmartMoneyScoringService → wallet_stats + transfers → wallet_metrics table |
| 13 | **Smart Money Refresh Job** | @Scheduled 2AM UTC → SmartMoneyScoringService.refreshAllMetrics() → batch update |


---

## Current Data Flows

### Flow 1: Wallet CRUD Operations

```
User Request → WalletController → WalletService → WalletPersistencePort
                                                          │
                                                          ▼
                                               WalletPersistenceAdapter
                                                          │
                                                          ▼
                                                WalletRepository (JPA)
                                                          │
                                                          ▼
                                                     PostgreSQL
```

**Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/wallets` | Create new wallet |
| `GET` | `/api/v1/wallets/{id}` | Get wallet by UUID |
| `GET` | `/api/v1/wallets/address/{address}` | Get wallet by address |
| `GET` | `/api/v1/wallets` | List all wallets (optional type filter) |
| `PUT` | `/api/v1/wallets/{id}` | Update wallet |
| `DELETE` | `/api/v1/wallets/{id}` | Delete wallet |
| `GET` | `/api/v1/wallets/exists/{address}` | Check if wallet exists |

---

### Flow 2: Transaction Lookup & Decode

```
User Request → TransactionController → TransactionService
                                              │
                          ┌───────────────────┼───────────────────┐
                          ▼                   ▼                   ▼
                   BlockChainPort      DexDecoderPort    TransactionPersistencePort
                          │                   │                   │
                          ▼                   ▼                   ▼
               Web3jBlockchainAdapter    DexDecoder    TransactionPersistenceAdapter
                          │                   │                   │
                          ▼                   │                   ▼
                  Alchemy RPC API             │              PostgreSQL
                                              │
                                              ▼
                                      Uniswap V2 Decoder
                                      (swapExactTokensForTokens, etc.)
```

**Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/transactions/{txHash}` | Fetch & decode transaction |
| `POST` | `/api/transactions/{txHash}` | Fetch, decode & save transaction |

**Decoded Data:**
- Function name (e.g., `swapExactTokensForTokens`)
- Token path (`tokenIn → ... → tokenOut`)
- Amounts (`amountIn`, `amountOut`, `amountOutMin`)
- Recipient, Deadline

---

### Flow 3: Wallet Transaction History Sync (Alchemy)

```
User Request → WalletController.syncWalletHistory()
                        │
                        ▼
               TransactionService.syncWalletHistory()
                        │
                        ▼
               BlockChainPort.getWalletTransactions()
                        │
                        ▼
               Web3jBlockchainAdapter
                        │
        ┌───────────────┴───────────────┐
        ▼                               ▼
  Outgoing Transfers            Incoming Transfers
  (fromAddress=wallet)          (toAddress=wallet)
        │                               │
        └───────────┬───────────────────┘
                    ▼
         Merge & Deduplicate
         (key: txHash|category|logIndex)
                    │
                    ▼
         Normalize → AssetTransfer
                    │
                    ▼
    TransactionPersistencePort.saveAssetTransfers()
                    │
                    ▼
      AssetTransferRepository → PostgreSQL
```

**Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/wallets/{address}/sync-history` | Sync wallet history from Alchemy |
| `GET` | `/api/v1/wallets/{address}/transactions` | Get stored transaction timeline |

**Alchemy API Used:**
- `alchemy_getAssetTransfers` (JSON-RPC)
- Categories: `external`, `erc20`
- Parallel fetch for incoming + outgoing
- Pagination with `pageKey`

**Day 14 Enhancement - USD Value Enrichment:**
```
For each AssetTransfer:
    │
    ├── if tokenAddress == null (Native ETH)
    │       → priceService.getEthPrice()
    │       → usdValue = value × ethPrice
    │
    └── else (ERC20 token)
            → priceService.calculateUsdValue(tokenAddress, value)
            → Checks Redis cache (60s TTL)
            → Falls back to DexScreener API
            → usdValue = value × tokenPrice
```

**Zero Dollar Bug Fix:**
- Native ETH transfers now correctly use `getEthPrice()` instead of returning $0

---

### Flow 4: Block Monitoring Job (Scheduled)

```
JobSchedulerConfig (@Scheduled every 12s)
                    │
                    ▼
           BlockMonitorJob.execute()
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
Redis            Postgres       Blockchain
(cursor)         (wallets)       (RPC)
    │               │               │
    ▼               ▼               ▼
Get last        Get tracked     Get blocks
processed       addresses       [cursor+1..current]
    │               │               │
    │      Cache in Redis           │
    │      (5-min TTL)              │
    │               │               │
    └───────────────┴───────────────┘
                    │
                    ▼
        Filter: tx.from OR tx.to
           IN trackedWallets
                    │
                    ▼
     TransactionPersistencePort.save()
      (UNIQUE constraint prevents duplicates)
                    │
                    ▼
         Update cursor per block
                    │
                    ▼
              PostgreSQL
```

**Features (Day 12):**
- ✅ Redis block cursor tracking (avoid re-processing)
- ✅ Wallet address caching (5-min TTL, avoids N+1)
- ✅ Tracked wallet filtering (from/to matching)
- ✅ Max catchup limit (100 blocks)
- ✅ Per-block cursor updates (atomic/safe)
- ✅ DB unique constraint (ON CONFLICT safe)

---

## Database Tables

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `wallets` | Tracked wallet addresses | `address`, `type`, `label`, `total_pnl`, `win_rate` |
| `transactions` | Raw blockchain transactions | `tx_hash`, `from`, `to`, `value`, `input` |
| `asset_transfers` | Normalized transfers (Alchemy) | `wallet_address`, `tx_hash`, `category`, `value`, `usd_value`, `asset_symbol`, `token_address` |
| `tokens` | Token metadata | `address`, `symbol`, `name` |
| `signals` | Trading signals | `type`, `wallet_id`, `token_address`, `confidence_score` |
| `address_labels` | Address Book (Day 24) | `address`, `label`, `category`, `source`, max 5 per address |
| `users` | User accounts | `email`, `telegram_id` |
| `alert_rules` | Alert configurations | `user_id`, `conditions`, `channels` |

---

## Port Interfaces

### BlockChainPort
```java
long getLatestBlockNumber()
Optional<Transaction> getTransactionByHash(String txHash)
List<Transaction> getTransactionsByBlock(long blockNumber)
List<AssetTransfer> getWalletTransactions(String address, List<TransferCategory> categories, int maxCount)
```

### DexDecoderPort
```java
DecodedSwap decodeSwap(String to, String input, BigDecimal txValue)
```

### TransactionPersistencePort
```java
Transaction save(Transaction transaction)
List<AssetTransfer> saveAssetTransfers(List<AssetTransfer> transfers)
List<AssetTransfer> findByWalletAddress(String address, int limit, String order)
List<AssetTransfer> findByWalletAddressAndDateRange(String address, LocalDateTime from, LocalDateTime to)
long countByWalletAddress(String address)
```

### WalletPersistencePort
```java
Wallet save(Wallet wallet)
Optional<Wallet> findById(UUID id)
Optional<Wallet> findByAddress(String address)
List<Wallet> findAll()
List<Wallet> findByType(WalletType type)
void deleteById(UUID id)
boolean existsByAddress(String address)
Set<String> getAllAddresses()  // NEW: For wallet caching
```

### BlockTrackingPort (Day 12)
```java
Optional<Long> getLastProcessedBlock()
void setLastProcessedBlock(long blockNumber)
Set<String> getTrackedWalletAddresses()
void cacheTrackedWalletAddresses(Set<String> addresses)
void invalidateWalletCache()
```

### PricePort (Day 11)
```java
Optional<TokenPrice> getTokenPrice(String tokenAddress, String chain)
BigDecimal getEthPrice()  // Used for Native ETH USD calculation
```

### CachePort<K, V> (Day 11)
```java
Optional<V> get(K key)
void put(K key, V value, Duration ttl)
void evict(K key)
```

---

### Flow 5: Token Price Lookup (Day 11) ✅ IMPLEMENTED
```
PriceController → PriceService → CachePort (Redis, 60s TTL)
                        │              │
                        │         Cache miss
                        │              │
                        ▼              ▼
                    PricePort → DexScreenerPriceAdapter
                                       │
                                       ▼
                              DexScreener API
                              (filter: trusted DEXes)
                              (sort: by liquidity)
```

**Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/prices/{tokenAddress}` | Get token price in USD |

---

## Flow 6: Redis Streams Event Processing (Day 15-16) ✅ IMPLEMENTED

```
BlockMonitorJob (real-time only)
          │
          ▼
   RedisStreamPublisher.publishEvent()
          │
          ▼
   Redis Stream "argus:transactions"
          │
          ├── Consumer Group: tx-processor-group
          │         │
          │         ▼
          │   TransactionStreamConsumer.onMessage()
          │         │
          │         ├── Build WhaleDetectionRequest from fields
          │         ├── Call WhaleDetectorService.detectAndSaveWhaleSignal()
          │         └── ACK message
          │
          └── (Future: Additional consumer groups for other detectors)
```

> [!IMPORTANT]
> **History Spam Prevention (Day 17 Fix)**
> - Only `BlockMonitorJob` publishes to the stream (real-time transactions)
> - `TransactionService.syncWalletHistory()` does NOT publish to stream
> - This prevents flooding the system with old signals when adding wallets

**Stream Keys:**
| Key | Purpose |
|-----|---------|
| `argus:transactions` | Real-time transactions from BlockMonitorJob |
| `argus:signals` | Detected trading signals (future) |

**Consumer Groups:**
| Group | Consumer | Purpose |
|-------|----------|---------|
| `tx-processor-group` | `tx-processor-{uuid}` | Whale detection, signal generation |

**Key Files:**
- `config/RedisStreamsConfig.java` - Stream container and subscription
- `core/constant/StreamKeys.java` - Stream key constants
- `infra/stream/StreamPublisher.java` - Publisher interface
- `infra/stream/RedisStreamPublisher.java` - Redis implementation
- `infra/stream/consumer/TransactionStreamConsumer.java` - Consumer implementation
- `infra/stream/dto/TransactionEvent.java` - Event payload

---

## Flow 7: Whale Detection & Signal Generation (Day 17) ✅ IMPLEMENTED

```
Redis Stream "argus:transactions"
          │
          ▼
TransactionStreamConsumer.detectSignals()
          │
          ▼
Build WhaleDetectionRequest from stream message
          │
          ├── txHash, from, to, walletAddress
          ├── tokenAddress, tokenSymbol
          ├── usdValue, timestamp
          │
          ▼
WhaleDetectorService.detectAndSaveWhaleSignal()
          │
          ├── Step 1: Validate input
          │     └── Skip if null or missing usdValue
          │
          ├── Step 2: Stale Transaction Check
          │     └── Skip if timestamp > 10 minutes old
          │
          ├── Step 3: Whale Threshold Check
          │     └── Skip if usdValue < $50,000
          │
          ├── Step 4: Idempotency Check
          │     └── Skip if signal already exists (txHash + type)
          │
          ├── Step 5: Determine Direction
          │     ├── if (to == walletAddress) → WHALE_BUY
          │     └── if (from == walletAddress) → WHALE_SELL
          │
          ├── Step 6: Calculate Confidence Score
          │     ├── $50K-$100K  → 0.70
          │     ├── $100K-$500K → 0.85
          │     └── $500K+      → 0.95
          │
          └── Step 7: Save Signal
                    │
                    ▼
          SignalPersistencePort.save()
                    │
                    ▼
          SignalPersistenceAdapter
                    │
                    ▼
          SignalRepository → PostgreSQL (signals table)
```

### Whale Detection Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         WHALE DETECTION DATA FLOW                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌───────────────────┐         ┌─────────────────────┐                         │
│  │ BlockMonitorJob   │────────▶│  Redis Stream       │                         │
│  │ (real-time only)  │ publish │ argus:transactions  │                         │
│  └───────────────────┘         └──────────┬──────────┘                         │
│                                           │ consume                            │
│  ┌───────────────────┐                    ▼                                    │
│  │ syncWalletHistory │    ┌─────────────────────────────────┐                  │
│  │ (NO stream!)      │    │ TransactionStreamConsumer       │                  │
│  └───────────────────┘    │                                 │                  │
│           │               │  ┌─────────────────────────┐    │                  │
│           ▼               │  │ WhaleDetectionRequest   │    │                  │
│    Save to DB only        │  │ ┌─────────────────────┐ │    │                  │
│    (backfill data)        │  │ │ txHash              │ │    │                  │
│                           │  │ │ from, to            │ │    │                  │
│                           │  │ │ walletAddress       │ │    │                  │
│                           │  │ │ tokenAddress/Symbol │ │    │                  │
│                           │  │ │ usdValue            │ │    │                  │
│                           │  │ │ timestamp           │ │    │                  │
│                           │  │ └─────────────────────┘ │    │                  │
│                           │  └───────────┬─────────────┘    │                  │
│                           └──────────────┼──────────────────┘                  │
│                                          │                                     │
│                                          ▼                                     │
│                           ┌─────────────────────────────────┐                  │
│                           │     WhaleDetectorService        │                  │
│                           │                                 │                  │
│                           │  ┌───────────────────────────┐  │                  │
│                           │  │ DETECTION LOGIC           │  │                  │
│                           │  │                           │  │                  │
│                           │  │ 1. Stale check (10 min)   │  │                  │
│                           │  │ 2. Threshold ($50K)       │  │                  │
│                           │  │ 3. Idempotency check      │  │                  │
│                           │  │ 4. Direction (BUY/SELL)   │  │                  │
│                           │  │ 5. Confidence scoring     │  │                  │
│                           │  └───────────────────────────┘  │                  │
│                           └───────────────┬─────────────────┘                  │
│                                           │                                    │
│                                           ▼                                    │
│                           ┌─────────────────────────────────┐                  │
│                           │     SignalPersistencePort       │                  │
│                           │     save(Signal)                │                  │
│                           │     existsByTxHashAndType()     │                  │
│                           └───────────────┬─────────────────┘                  │
│                                           │                                    │
│                                           ▼                                    │
│                           ┌─────────────────────────────────┐                  │
│                           │     PostgreSQL: signals         │                  │
│                           │                                 │                  │
│                           │  tx_hash + type = UNIQUE        │                  │
│                           └─────────────────────────────────┘                  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Signal Types (SignalType.java)

| Type | Description | Condition |
|------|-------------|-----------|
| `WHALE_BUY` | Whale buying tokens | `to == walletAddress` AND `usdValue >= $50K` |
| `WHALE_SELL` | Whale selling tokens | `from == walletAddress` AND `usdValue >= $50K` |
| `MULTI_WHALE` | 3+ whales buying same token | 3+ distinct wallets with WHALE_BUY in 24h |
| `ACCUMULATION` | Wallet accumulating token | 3+ WHALE_BUY, 0 WHALE_SELL in 72h |

### Confidence Scoring Tiers

**WHALE_BUY/SELL:**
| USD Value Range | Confidence Score |
|-----------------|------------------|
| $50K - $100K | 0.70 |
| $100K - $500K | 0.85 |
| $500K+ | 0.95 |

**MULTI_WHALE / ACCUMULATION:**
| Count | Confidence Score |
|-------|------------------|
| 3 | 0.70 |
| 4 | 0.85 |
| 5+ | 0.95 |

### Multi-Whale Detection Flow (Day 19)

```
WHALE_BUY saved → checkAndCreateMultiWhaleSignal(tokenAddress)
      │
      ├── Check if MULTI_WHALE exists in 24h → Skip if yes
      ├── Count distinct walletIds with WHALE_BUY
      └── If count >= 3 → Create MULTI_WHALE signal
```

### Accumulation Pattern Flow (Day 20)

```
WHALE_BUY saved → checkAccumulationPattern(walletId, tokenAddress)
      │
      ├── Check if ACCUMULATION exists in 72h → Skip if yes
      ├── Count WHALE_BUY for (wallet, token) in 72h
      ├── Count WHALE_SELL for (wallet, token) in 72h
      └── If buys >= 3 AND sells == 0 → Create ACCUMULATION signal
                                             │
                                             └── metadata: {buyCount, totalPositionUsd, windowHours: 72}
```

### Key Design Decisions

1. **10-Minute Stale Window**: Transactions older than 10 minutes are ignored.
2. **Idempotency via DB Constraint**: `UNIQUE INDEX idx_signals_tx_hash_type`.
3. **Direction Detection**: Wallet address comparison for BUY vs SELL.
4. **No History Spam**: Only real-time BlockMonitorJob triggers detection.
5. **Multi-Whale 24h Window**: Token-level signal, walletId = null.
6. **Accumulation 72h Window**: Wallet-level signal, tracks total position.

### Key Files (Day 17 + 19 + 20)

| File | Purpose |
|------|---------|
| `domain/service/WhaleDetectorService.java` | All signal detection logic |
| `domain/port/persistence/SignalPersistencePort.java` | Signal persistence interface |
| `infra/persistence/repository/SignalRepository.java` | JPQL queries for counting |
| `domain/model/dto/WhaleDetectionRequest.java` | DTO with walletId for accumulation |

### SignalPersistencePort Interface

```java
public interface SignalPersistencePort {
    Signal save(Signal signal);
    boolean existsByTxHashAndType(String txHash, String type);
    // Multi-whale
    long countDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);
    List<UUID> findDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);
    boolean multiWhaleSignalExistsForToken(String tokenAddress, LocalDateTime since);
    // Accumulation
    long countBuysByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);
    long countSellsByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);
    BigDecimal sumBuyValueByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);
    boolean accumulationSignalExists(UUID walletId, String tokenAddress, LocalDateTime since);
}
```

---

## Flow 8: Historical Import with Price Enrichment (Day 22) ✅ IMPLEMENTED

```
API: POST /api/v1/wallets/{id}/sync-history?days=30
           │
           ▼
HistoricalImportService.importWalletHistory(address, days)
           │
           ├── Step 1: Fetch transfers from Alchemy
           │     └── BlockChainPort.getWalletTransactions()
           │     └── Up to 500 transfers (EXTERNAL + ERC20)
           │
           ├── Step 2: Filter by date range
           │     └── Only keep transfers in last N days
           │
           ├── Step 3: Extract unique token addresses
           │     └── Set<String> uniqueTokens
           │
           ├── Step 4: Fetch price ranges (1 API call per token!)
           │     ├── PricePort.getTokenPriceRange(tokenAddr, from, to)
           │     └── CoinGecko /market_chart/range → 30 days hourly data
           │     └── Store in Map<tokenAddr, TokenPriceRange>
           │
           ├── Step 5: Enrich all transfers (in-memory, zero API calls)
           │     ├── For each transfer:
           │     │   ├── Look up TokenPriceRange from map
           │     │   ├── Call range.getPriceAtTimestamp(txTimestamp)
           │     │   ├── Set priceAtTx, priceSource, usdValue
           │     │   └── priceSource = "coingecko" | "missing" | "unsupported"
           │     └── Native ETH: use current price, priceSource = "current"
           │
           └── Step 6: Batch save to database
                 └── TransactionPersistencePort.saveAssetTransfers()
```

### Key Files
| File | Purpose |
|------|---------|
| `HistoricalImportService.java` | Orchestrates import flow |
| `CoinGeckoPriceAdapter.java` | Fetches price ranges from CoinGecko |
| `TokenPriceRange.java` | In-memory cache for 30-day hourly prices |

### Price Source Values
| Value | Meaning |
|-------|---------|
| `coingecko` | Historical price successfully fetched |
| `missing` | Token exists but price data gap at timestamp |
| `unsupported` | Token not found on CoinGecko |
| `current` | Native ETH using current price |
| `error` | API error during fetch |

---

## Flow 9: Price Retry Job (Day 22) ✅ IMPLEMENTED

```
PriceRetryJob (@Scheduled "0 0 3 * * ?")  ←── Runs at 3 AM daily
           │
           ├── Step 1: Query transfers with missing prices
           │     └── findByPriceSourceIn(["missing", "error"])
           │
           ├── Step 2: Group by token address
           │     └── Map<tokenAddr, List<AssetTransfer>>
           │
           └── For each token group:
                 │
                 ├── Step 3: Calculate date range from transactions
                 │     └── min/max of getTxTimestamp()
                 │
                 ├── Step 4: Fetch price range (1 API call per token)
                 │     └── PricePort.getTokenPriceRange(tokenAddr, min, max)
                 │
                 ├── Step 5: Enrich transfers from range
                 │     ├── range.getPriceAtTimestamp(txTimestamp)
                 │     └── Update priceAtTx, priceSource, usdValue
                 │
                 ├── Step 6: Save updated transfers
                 │     └── TransactionPersistencePort.saveAssetTransfers()
                 │
                 └── Rate limit: Thread.sleep(2000)
```

### Key Points
- **Never uses current price** for historical data (prevents PnL corruption)
- Groups by token to minimize API calls
- Efficient retry strategy: 1 API call can fix many transfers

---

## Flow 10: Wallet Stats Calculation (Day 23)

```
API: POST /api/v1/wallets/{id}/calculate-stats
           │
           ▼
WalletController.calculateStats(UUID id)
           │
           ├── Step 1: Get wallet by ID
           │     └── WalletService.getWalletById(id)
           │     └── Returns wallet.getAddress()
           │
           ▼
WalletStatsService.calculateStats(walletAddress)
           │
           ├── Step 2: Fetch transfers from DB
           │     └── TransactionPersistencePort.findByWalletAddress(addr, 10000, "asc")
           │     └── Warn if limit hit (stats may be incomplete)
           │
           ├── Step 3: Filter priced transfers
           │     └── Only keep transfers with usdValue != null && tokenAddress != null
           │
           ├── Step 4: Group by token address
           │     └── Map<tokenAddr, List<AssetTransfer>>
           │
           ├── Step 5: For each token → calculateTokenStats()
           │     │
           │     ├── Accumulate totalBought, totalSold, costBasisUsd, proceedsUsd
           │     ├── Determine BUY (to == wallet) or SELL (from == wallet)
           │     ├── Calculate avgBuyPrice = costBasisUsd / totalBought
           │     ├── Calculate avgSellPrice = proceedsUsd / totalSold
           │     ├── Calculate costOfSold = avgBuyPrice × totalSold (Average Cost Method)
           │     ├── Calculate realizedPnl = proceedsUsd - costOfSold
           │     ├── Calculate roiPercent = (realizedPnl / costOfSold) × 100
           │     └── Return WalletStats object
           │
           ├── Step 6: Persist all stats
           │     └── WalletStatsPersistencePort.saveAll(tokenStats)
           │
           └── Step 7: Build summary
                 │
                 ├── Sum totalPnl across all tokens
                 ├── Count totalTrades (# of unique tokens)
                 ├── Count profitableTrades (where isProfitable = true)
                 ├── Calculate winRate = profitableTrades / totalTrades × 100
                 ├── Calculate avgRoi for closed positions only
                 └── Return WalletStatsSummary
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/wallets/{id}/calculate-stats` | Recalculate stats from transaction history |
| `GET` | `/api/v1/wallets/{id}/stats` | Retrieve previously calculated stats |

### PnL Calculation Method: Average Cost

```
                    cost_basis_usd
avg_buy_price = ────────────────────
                    total_bought

cost_of_sold = avg_buy_price × total_sold

realized_pnl = proceeds_usd - cost_of_sold

                   realized_pnl
roi_percent = ──────────────────── × 100
                   cost_of_sold
```

### Key Files

| File | Purpose |
|------|---------|
| `domain/service/WalletStatsService.java` | Stats calculation logic |
| `domain/model/WalletStats.java` | Per-token stats model |
| `domain/model/WalletStatsSummary.java` | Aggregated stats DTO |
| `domain/port/persistence/WalletStatsPersistencePort.java` | Port interface |
| `infra/persistence/adapter/WalletStatsPersistenceAdapter.java` | JPA implementation |
| `infra/persistence/entity/WalletStatsEntity.java` | JPA entity |
| `api/dto/response/WalletStatsResponse.java` | API response DTO |

### Design Decisions

1. **Average Cost Method**: Industry standard for PnL calculation
2. **Realized PnL Only**: Only counts profits/losses from actual sales
3. **Token-based Trades**: Each unique token = 1 "trade" for win rate
4. **10K Transaction Limit**: Capped to prevent memory issues, warning logged
5. **Case-insensitive**: All addresses lowercased for consistent lookup

---


## Flow 11: Address Book / Wallet Labeling (Day 24) ✅ IMPLEMENTED

```
AddressBookController
       │
       ├── POST /api/v1/address-book          → Add label to address
       ├── GET  /api/v1/address-book/{address} → Get labels for address
       ├── DELETE /api/v1/address-book/{addr}/labels/{label} → Remove label
       ├── GET  /api/v1/address-book?label=|q=|category= → Search labels
       └── POST /api/v1/address-book/import    → Bulk import
               │
               ▼
       AddressBookService
               │
               ├── Validate input (label length, address format)
               ├── Enforce max 5 labels per address
               ├── Normalize address to lowercase
               └── Handle exceptions:
                       ├── MaxLabelsExceededException (400)
                       ├── LabelAlreadyExistsException (409)
                       └── LabelNotFoundException (404)
               │
               ▼
       AddressBookPersistencePort
               │
               ▼
       address_labels table (PostgreSQL)
               │
               └── UNIQUE(address, label) constraint
               └── Indexes: address, lower(label), category
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/address-book` | Add label (max 5 per address) |
| `GET` | `/api/v1/address-book/{address}` | Get all labels for address |
| `DELETE` | `/api/v1/address-book/{address}/labels/{label}` | Remove specific label |
| `GET` | `/api/v1/address-book?label=X` | Search by exact label |
| `GET` | `/api/v1/address-book?q=X` | Search labels containing text |
| `GET` | `/api/v1/address-book?category=X` | Filter by category |
| `POST` | `/api/v1/address-book/import` | Bulk import labels (JSON array) |

### Key Design: Address Book Pattern

**Why not use `wallets.id` for labels?**
- Need to label addresses NOT being tracked (e.g., known scammers, CEX hot wallets)
- Avoid creating dummy wallets just for labeling
- Address Book = label ANY Ethereum address

### Key Files

| File | Purpose |
|------|---------|
| `domain/model/AddressLabel.java` | Model with max 5 labels constant |
| `domain/service/AddressBookService.java` | CRUD + validation + bulk import |
| `api/AddressBookController.java` | REST controller |
| `api/spec/AddressBookApi.java` | OpenAPI specification |
| `core/exception/LabelNotFoundException.java` | 404 exception |
| `core/exception/MaxLabelsExceededException.java` | 400 exception |
| `core/exception/LabelAlreadyExistsException.java` | 409 exception |

### Bulk Import Performance

- **Batch fetch**: 1 query to get all existing labels for import addresses
- **In-memory filtering**: Check duplicates and max labels without DB calls
- **Batch save**: 1 `saveAll()` call
- **Total**: 2 queries regardless of import size

---


## Flow 12: Smart Money Scoring & API (Day 25-26) ✅ IMPLEMENTED

```
SmartMoneyController
       │
       ├── GET  /api/v1/smart-money              → List all wallets (paginated, filter by archetype)
       ├── GET  /api/v1/smart-money/{address}    → Get metrics for specific wallet
       ├── GET  /api/v1/smart-money/top          → Top wallets by score (sortBy: pnl/consistency/conviction)
       └── POST /api/v1/smart-money/{address}/refresh → Manually recalculate metrics
               │
               ▼
       SmartMoneyScoringService.calculateMetrics(address)
               │
               ├── Step 1: Aggregate wallet stats
               │     └── WalletStatsAggregator.aggregateStats(address)
               │     └── Return WalletStatsSummary (totalPnl, winRate, avgRoi, etc.)
               │
               ├── Step 2: Fetch transaction history
               │     └── TransactionPersistencePort.findByWalletAddress(addr, 10000, "asc")
               │
               ├── Step 3: Calculate raw metrics
               │     ├── avgHoldTimeSec (from buy→sell round trips)
               │     ├── tradeCount7d (trades in last 7 days)
               │     ├── avgPositionSizeUsd
               │     ├── maxRoiPercent, profitFactor
               │     └── buyVolUsd, sellVolUsd
               │
               ├── Step 4: Classify archetype (waterfall)
               │     ├── MEV_BOT   → holdTime<120s, winRate>80%, avgROI<5%, 50+ trades/7d → BLACKLISTED
               │     ├── WHALE     → avgPosition>$10K, netPnl>$100K
               │     ├── ACCUMULATOR → holdTime>14d, sellRatio<25%
               │     ├── HOME_RUN  → winRate<40%, maxROI>500%, profitFactor>2
               │     ├── SNIPER    → winRate>70%, 5+ trades
               │     └── UNKNOWN   → default
               │
               ├── Step 5: Calculate scores (0-100)
               │     ├── pnlScore = log10(totalPnl) × 15, capped 0-100
               │     ├── consistencyScore = winRate×0.7 + log(trades)×20×0.3
               │     └── convictionScore = buyDominance × holdTimeBoost
               │
               ├── Step 6: Calculate total score & tier
               │     ├── totalScore = pnl×0.5 + consistency×0.3 + conviction×0.2
               │     └── tier = S(≥85), A(≥70), B(≥50), C(<50)
               │
               └── Step 7: Persist
                     │
                     ▼
               WalletMetricsPersistencePort.save()
                     │
                     ▼
               wallet_metrics table (PostgreSQL)
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/smart-money?page=0&size=20&archetype=SNIPER` | List wallets, filter by archetype |
| `GET` | `/api/v1/smart-money/{address}` | Get wallet metrics (404 if not found) |
| `GET` | `/api/v1/smart-money/top?sortBy=pnl&limit=10` | Top wallets by score dimension |
| `POST` | `/api/v1/smart-money/{address}/refresh` | Recalculate metrics for address |

### Archetype Classification

| Archetype | Criteria | Notes |
|-----------|----------|-------|
| `MEV_BOT` | holdTime<2min, winRate>80%, avgROI<5%, 50+ trades/week | Auto-blacklisted |
| `WHALE` | avgPosition>$10K, netPnl>$100K | Large capital traders |
| `ACCUMULATOR` | holdTime>14d, sellRatio<25%, buyVol>$100 | Long-term holders |
| `HOME_RUN` | winRate<40%, maxROI>500%, profitFactor>2 | High-risk, high-reward |
| `SNIPER` | winRate>70%, 5+ trades | Consistent performers |
| `UNKNOWN` | None of the above | Default classification |

### Scoring Formula

```
PnL Score (50% weight):
  pnlScore = min(100, max(0, log10(totalPnl) × 15))

Consistency Score (30% weight):
  winRateComponent = winRate × 0.7
  tradeComponent = min(100, log10(totalTrades) × 20) × 0.3
  consistencyScore = winRateComponent + tradeComponent

Conviction Score (20% weight):
  buyDominance = buyVolUsd / (buyVolUsd + sellVolUsd)
  holdTimeBoost = min(100, holdTimeDays × 2)
  convictionScore = buyDominance × holdTimeBoost

Total Score:
  totalScore = pnlScore×0.5 + consistencyScore×0.3 + convictionScore×0.2

Tier Assignment:
  S = totalScore ≥ 85
  A = totalScore ≥ 70
  B = totalScore ≥ 50
  C = totalScore < 50
```

### SmartMoneyRefreshJob (Daily @ 2AM UTC)

```
@Scheduled(cron = "0 0 2 * * *", zone = "UTC")
       │
       ├── Step 1: Fetch active wallet addresses in batches
       │     └── WalletStatsPersistencePort.findActiveWalletAddresses(page, batchSize)
       │
       ├── Step 2: For each wallet → calculateMetrics(address)
       │     └── Catch exceptions per wallet (don't fail batch)
       │
       └── Step 3: Log summary
             └── "Completed metric refresh. Total wallets updated: X, Time taken: Ys"
```

### Key Files

| File | Purpose |
|------|---------|
| `domain/service/SmartMoneyScoringService.java` | Scoring logic, archetype classification |
| `domain/service/WalletStatsAggregator.java` | Aggregates wallet_stats into summary |
| `domain/model/WalletMetrics.java` | Metrics domain model |
| `domain/model/SmartMoneyArchetype.java` | Archetype enum |
| `domain/port/persistence/WalletMetricsPersistencePort.java` | Port interface |
| `infra/persistence/adapter/WalletMetricsPersistenceAdapter.java` | JPA implementation |
| `api/SmartMoneyController.java` | REST endpoints |
| `api/spec/SmartMoneyApi.java` | OpenAPI specification |
| `api/dto/response/SmartMoneyResponse.java` | API response DTO |
| `job/SmartMoneyRefreshJob.java` | Daily refresh job |

### Design Decisions

1. **Waterfall Classification**: Archetypes checked in priority order (MEV first to blacklist bots)
2. **Logarithmic PnL Scoring**: Prevents outliers from dominating scores
3. **MEV Auto-Blacklist**: MEV bots marked `isBlacklisted=true`, excluded from leaderboards
4. **UTC Timezone**: Job runs at 2AM UTC consistently regardless of server location
5. **Batch Processing**: 100 wallets per batch to prevent memory issues
6. **Lowercase Normalization**: All addresses normalized to lowercase for consistency

---


## Quick Reference

### Package Structure
```
com.argus/
├── api/                          # REST Controllers
│   ├── WalletController          # /api/v1/wallets/*
│   ├── TransactionController     # /api/v1/transactions/*
│   ├── PriceController           # /api/v1/prices/*
│   └── dto/
│       ├── WalletRequest, WalletResponse
│       ├── TransactionResponse
│       ├── ErrorResponse
│       └── response/
│           ├── PriceResponse
│           ├── AssetTransferResponse
│           ├── SyncResponse
│           └── WalletTimelineResponse
├── domain/
│   ├── model/
│   │   ├── Wallet, Transaction, Token, Signal
│   │   ├── AssetTransfer, DecodedSwap
│   │   ├── TokenPrice, TransactionWithSwap
│   │   ├── SignalType                    # Day 17: WHALE_BUY, WHALE_SELL, etc.
│   │   ├── TokenPriceRange               # Day 22: In-memory price lookup
│   │   └── dto/
│   │       └── WhaleDetectionRequest     # Day 17: Input for whale detection
│   ├── service/
│   │   ├── WalletService
│   │   ├── TransactionService
│   │   ├── PriceService
│   │   ├── WhaleDetectorService          # Day 17: Whale detection logic
│   │   └── HistoricalImportService       # Day 22: Historical import with pricing
│   └── port/
│       ├── blockchain/
│       │   ├── BlockChainPort
│       │   ├── DexDecoderPort
│       │   └── PricePort
│       ├── cache/
│       │   ├── CachePort<K,V>
│       │   └── BlockTrackingPort
│       └── persistence/
│           ├── WalletPersistencePort
│           ├── TransactionPersistencePort
│           ├── TokenPersistencePort
│           └── SignalPersistencePort     # Day 17: save(), existsByTxHashAndType()
├── infra/
│   ├── blockchain/
│   │   ├── Web3jBlockchainAdapter
│   │   ├── Erc20TransferEvent
│   │   ├── decoder/
│   │   │   ├── DexDecoder
│   │   │   └── RouterDefinitions
│   │   └── dto/
│   │       ├── AlchemyAssetTransfersRequest
│   │       ├── AlchemyTransferDto
│   │       └── response/
│   │           ├── AlchemyAssetTransfersResponse
│   │           └── DexScreenerResponse
│   ├── cache/
│   │   ├── RedisBlockTrackingAdapter
│   │   └── RedisPriceCacheAdapter
│   ├── persistence/
│   │   ├── adapter/
│   │   │   ├── WalletPersistenceAdapter
│   │   │   ├── TransactionPersistenceAdapter
│   │   │   ├── TokenPersistenceAdapter
│   │   │   └── SignalPersistenceAdapter
│   │   ├── entity/
│   │   │   ├── WalletEntity, TransactionEntity
│   │   │   ├── TokenEntity, SignalEntity
│   │   │   └── AssetTransferEntity
│   │   └── repository/
│   │       ├── WalletRepository, TransactionRepository
│   │       ├── TokenRepository, SignalRepository
│   │       └── AssetTransferRepository
│   └── price/
│       └── adapter/
│           ├── DexScreenerPriceAdapter
│           └── CoinGeckoPriceAdapter      
├── job/
│   ├── BlockMonitorJob
│   ├── PriceRetryJob                 
│   └── config/
│       └── JobSchedulerConfig
├── config/
│   ├── DomainServiceConfig
│   └── RedisConfig
└── core/
    └── exception/
        ├── GlobalExceptionHandler
        ├── DomainException, BlockchainException
        ├── WalletNotFoundException
        └── TransactionNotFoundException
```

### Environment Variables
| Variable | Description |
|----------|-------------|
| `ARGUS_BLOCKCHAIN_RPC_URL` | Alchemy/Infura RPC endpoint |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string |
| `REDIS_HOST` | Redis server host |
| `REDIS_PORT` | Redis server port (default: 6379) |

