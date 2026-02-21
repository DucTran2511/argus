# External Services Integration

> **Last Updated**: 2026-01-19 | **Status**: Source of Truth

This document defines how Argus integrates with third-party APIs, including architectural decisions, rate limiting strategies, and failure handling patterns.

---

## Data Architecture

Argus separates its data sources into two categories:

| Category | Service | Provides | Does NOT Provide |
|----------|---------|----------|------------------|
| **The Ledger** | Alchemy | Transactions, blocks, wallet history | Token prices |
| **The Market** | DexScreener | Real-time prices, liquidity | Historical prices |
| **The Market** | CoinGecko | Historical prices, metadata | Real-time data |

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DATA FLOW                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ALCHEMY (Ledger)              DEXSCREENER / COINGECKO (Market)                │
│   ────────────────              ────────────────────────────────                │
│   "WHO sent WHAT, WHEN"         "HOW MUCH was it worth"                         │
│                                                                                  │
│   ┌─────────────────┐           ┌─────────────────┐  ┌─────────────────┐        │
│   │ Asset Transfers │           │ Real-time Price │  │ Historical Price│        │
│   │ • tx_hash       │           │ (DexScreener)   │  │ (CoinGecko)     │        │
│   │ • from/to       │           │                 │  │                 │        │
│   │ • value         │           │ For: Alerts     │  │ For: PnL        │        │
│   │ • timestamp     │           └────────┬────────┘  └────────┬────────┘        │
│   └────────┬────────┘                    │                    │                 │
│            │                             └──────────┬─────────┘                 │
│            │                                        │                           │
│            └────────────────────┬───────────────────┘                           │
│                                 │                                               │
│                                 ▼                                               │
│                    ┌───────────────────────────────┐                            │
│                    │      ENRICHED TRANSFER        │                            │
│                    │      value + price = usd_value│                            │
│                    └───────────────────────────────┘                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Alchemy

| Property | Value |
|----------|-------|
| **Role** | Primary Node Provider & Ingestion Engine |
| **Base URL** | `https://eth-mainnet.g.alchemy.com/v2/` |
| **Env Variable** | `ALCHEMY_API_KEY` |
| **Free Tier** | 330 Compute Units/second |

### Endpoints Used

| Endpoint | Used By | Purpose |
|----------|---------|---------|
| `alchemy_getAssetTransfers` | BlockMonitorJob, HistoricalImportService | Fetch wallet transaction history |
| `eth_getBlockByNumber` | Web3jBlockchainAdapter | Map block numbers to timestamps |

### Rate Limiting

```
┌───────────────────────────────────────────────────────────────────┐
│  BlockMonitorJob runs every 12 seconds                            │
│                                                                   │
│  0s        12s        24s        36s                              │
│  │          │          │          │                               │
│  ├──────────┼──────────┼──────────┼─────────▶                     │
│  │ POLL     │ POLL     │ POLL     │ POLL                          │
│  │ ~2s      │ ~2s      │ ~2s      │ ~2s                           │
│                                                                   │
│  Max 100 blocks processed per cycle                               │
└───────────────────────────────────────────────────────────────────┘
```

### Failure Handling

| Scenario | Action |
|----------|--------|
| API timeout | Do NOT update `last_scanned_block`, retry next cycle |
| Rate limited | Log warning, resume from same block |
| Network error | Same as timeout |

**Result**: No data loss - just delayed ingestion.

---

## DexScreener

| Property | Value |
|----------|-------|
| **Role** | Real-time token prices & liquidity |
| **Base URL** | `https://api.dexscreener.com/latest/dex/` |
| **Env Variable** | None required |
| **Rate Limit** | Fair use (no documented limit) |

### Endpoints Used

| Endpoint | Used By | Purpose |
|----------|---------|---------|
| `/tokens/{address}` | DexScreenerPriceAdapter | Get current price with liquidity filter |

### Implementation

```java
// DexScreenerPriceAdapter filters:
// 1. Trusted DEXes only: uniswap_v2, uniswap_v3, sushiswap
// 2. Minimum liquidity: $1,000 USD
// 3. Select highest liquidity pair
```

### When to Use

| Use Case | Use DexScreener? |
|----------|------------------|
| Real-time whale alerts | ✅ Yes |
| Historical PnL calculations | ❌ No (use CoinGecko) |
| Token metadata | ❌ No (use CoinGecko) |

---

## CoinGecko

| Property | Value |
|----------|-------|
| **Role** | Historical prices & token metadata |
| **Base URL** | `https://api.coingecko.com/api/v3/` |
| **Env Variable** | `COINGECKO_API_KEY` (optional for free tier) |
| **Chain ID** | `ethereum` (hardcoded) |
| **Free Tier** | 10-30 calls/minute |

### Endpoints Used

| Endpoint | Used By | Purpose |
|----------|---------|---------|
| `/simple/price` | CoinGeckoPriceAdapter | Get current ETH price |
| `/coins/ethereum/contract/{addr}/market_chart/range` | CoinGeckoPriceAdapter | Get 30 days of hourly prices |

### The Range Strategy

> [!IMPORTANT]
> We make **1 API call per token** (fetching 30 days), NOT 1 call per transaction.

```
50 transactions with 8 unique tokens:
├── Wrong: 50 API calls (1 per tx) = 5 minutes
└── Correct: 8 API calls (1 per token) = 16 seconds
```

### Rate Limiting

```java
private static final long RATE_LIMIT_DELAY_MS = 2000;

// Every method calls this before API request
private void rateLimitDelay() {
    Thread.sleep(RATE_LIMIT_DELAY_MS);
}
```

### Failure Handling

> [!CAUTION]
> **NEVER** use current price as fallback for historical data — this corrupts PnL calculations.

| Scenario | Action |
|----------|--------|
| API timeout | Retry 3x with backoff |
| Rate limited (429) | Set `price_source = 'missing'` |
| Token not found (404) | Set `price_source = 'unsupported'` |
| Other errors | Set `price_source = 'error'` |

### Price Retry Job

Records with missing prices are retried nightly:

```java
@Scheduled(cron = "0 0 3 * * ?")  // 3 AM daily
public void retryMissingPrices() {
    List<AssetTransfer> missing = transactionPort.findByPriceSourceIn(
        List.of("missing", "error"));
    
    // Group by token for efficient API usage
    Map<String, List<AssetTransfer>> byToken = missing.stream()
        .collect(Collectors.groupingBy(AssetTransfer::getTokenAddress));
    
    for (var entry : byToken.entrySet()) {
        Optional<TokenPriceRange> range = pricePort.getTokenPriceRange(...);
        // Enrich all transfers from in-memory cache
        // 1 API call can fix many transfers
        Thread.sleep(2000);
    }
}
```

---

## Price Source Values

The `price_source` column in `asset_transfers` indicates data origin:

| Value | Meaning |
|-------|---------|
| `coingecko` | Historical price successfully fetched |
| `dexscreener` | Real-time price from DexScreener |
| `current` | Native ETH using current price |
| `missing` | Token exists but price data gap |
| `unsupported` | Token not found on CoinGecko |
| `error` | API error during fetch |

---

## Failure Modes Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              FAILURE HANDLING                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   SERVICE DOWN              RECOVERY STRATEGY                                   │
│   ────────────              ─────────────────                                   │
│                                                                                  │
│   Alchemy                   last_scanned_block NOT updated                      │
│                             → Next job resumes from same block                  │
│                             → NO DATA LOSS                                      │
│                                                                                  │
│   DexScreener               Return cached price or NOT_FOUND                    │
│                             → Whale detection skipped for this tx               │
│                                                                                  │
│   CoinGecko                 Set price_source = 'missing'                        │
│                             → PriceRetryJob picks up at 3 AM                    │
│                             → NO INCORRECT DATA                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Testing Standards

> [!WARNING]
> **STRICTLY FORBIDDEN**: Calling real external APIs in unit tests.

### Required Practices

1. Mock all external calls with Mockito
2. Use JSON fixtures from `src/test/resources/fixtures/`
3. Fixtures:
   - `alchemy_asset_transfers.json`
   - `coingecko_price_response.json`
   - `coingecko_market_chart.json`
   - `dexscreener_response.json`

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ALCHEMY_API_KEY` | ✅ Yes | Blockchain access |
| `COINGECKO_API_KEY` | ❌ No | Optional, higher limits |

```properties
# .env
ALCHEMY_API_KEY=your_alchemy_key_here
COINGECKO_API_KEY=  # Leave empty for free tier
```

---

## Quick Reference

| Service | Free Tier | Rate Strategy | Failure Mode |
|---------|-----------|---------------|--------------|
| Alchemy | 330 CU/s | 12s job intervals | Resume from last block |
| DexScreener | Fair use | No delay needed | Return NOT_FOUND |
| CoinGecko | 10-30/min | 2s delay, range queries | Mark 'missing', retry at 3 AM |
