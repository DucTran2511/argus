# Argus - Data Flow Documentation

> **Last Updated**: 2026-01-09 | **Status**: Living Document

This document describes the current data flows in the Argus system. Keep this updated as new features are added.

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL SERVICES                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   Alchemy   │    │ DexScreener │    │  Telegram   │                  │
│  │  (RPC/API)  │    │  (Prices)   │    │   (Alerts)  │                  │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                  │
└─────────┼──────────────────┼──────────────────┼─────────────────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              ARGUS SYSTEM                                │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                        API LAYER (api/)                            │ │
│  │  WalletController          TransactionController                   │ │
│  │  └─/wallets/*              └─/transactions/*                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                                    ▼                                     │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                      DOMAIN LAYER (domain/)                        │ │
│  │  ┌──────────────────┐    ┌──────────────────┐                      │ │
│  │  │  WalletService   │    │TransactionService│                      │ │
│  │  └────────┬─────────┘    └────────┬─────────┘                      │ │
│  │           │                       │                                 │ │
│  │  ┌────────┴───────────────────────┴────────┐                       │ │
│  │  │                  PORTS                   │                       │ │
│  │  │  BlockChainPort  DexDecoderPort         │                       │ │
│  │  │  WalletPersistencePort                  │                       │ │
│  │  │  TransactionPersistencePort             │                       │ │
│  │  └─────────────────────────────────────────┘                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                                    ▼                                     │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                       INFRA LAYER (infra/)                         │ │
│  │  Web3jBlockchainAdapter    DexDecoder    PersistenceAdapters       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                                    ▼                                     │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                         STORAGE LAYER                              │ │
│  │  PostgreSQL (wallets, transactions, signals, asset_transfers)      │ │
│  │  Redis (block cursor, wallet cache, price cache)                   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

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

## Future Flows (Planned)

### Flow 6: Redis Streams Event Processing (Day 15-16)
```
BlockMonitorJob → Redis Stream "argus:transactions"
                              │
                              ▼
                    TransactionProcessor (consumer)
                              │
                              ▼
                    SignalDetector → Redis Stream "argus:signals"
                                              │
                                              ▼
                                    AlertProcessor → Telegram
```

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
│   ├── service/
│   │   ├── WalletService
│   │   ├── TransactionService
│   │   └── PriceService
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
│           └── SignalPersistencePort
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
│           └── DexScreenerPriceAdapter
├── job/
│   ├── BlockMonitorJob
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

