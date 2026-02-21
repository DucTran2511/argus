# Argus Backend - API Reference for Frontend

> **Generated:** 2026-01-16 | Copy this file to `argus-ui/docs/`

## Base URL
```
http://localhost:8080/api/v1
```

---

## Wallets API

### Create Wallet
```http
POST /wallets
Content-Type: application/json

{
  "address": "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045",
  "label": "Vitalik",
  "type": "WHALE"
}

Response 201:
{
  "id": "uuid",
  "address": "0x...",
  "label": "Vitalik",
  "type": "WHALE",
  "chain": "ethereum",
  "createdAt": "2026-01-16T00:00:00Z"
}
```

### Get Wallet by ID
```http
GET /wallets/{id}
```

### Get Wallet by Address
```http
GET /wallets/address/{address}
```

### List All Wallets
```http
GET /wallets
GET /wallets?type=WHALE
```

### Update Wallet
```http
PUT /wallets/{id}
```

### Delete Wallet
```http
DELETE /wallets/{id}
```

### Get Wallet Timeline (Transactions)
```http
GET /wallets/{address}/timeline?limit=50&order=desc

Response:
{
  "address": "0x...",
  "transfers": [
    {
      "txHash": "0x...",
      "from": "0x...",
      "to": "0x...",
      "category": "erc20",
      "value": 1000.5,
      "assetSymbol": "USDC",
      "tokenAddress": "0x...",
      "usdValue": 1000.50,
      "txTimestamp": "2026-01-15T12:00:00Z"
    }
  ],
  "totalCount": 150
}
```

### Sync Wallet History
```http
POST /wallets/{address}/sync?maxCount=500

Response:
{
  "synced": 245,
  "address": "0x...",
  "status": "completed"
}
```

---

## Transactions API

### Get Transaction
```http
GET /transactions/{txHash}
```

### Save Transaction
```http
POST /transactions/{txHash}
```

---

## Prices API

### Get Token Price
```http
GET /prices/{tokenAddress}

Response:
{
  "tokenAddress": "0x...",
  "priceUsd": 1.0001,
  "source": "dexscreener",
  "timestamp": "2026-01-16T00:00:00Z"
}
```

---

## Signals (NOT YET EXPOSED - future endpoints)

### Data Model
```typescript
interface Signal {
  id: number;
  type: 'WHALE_BUY' | 'WHALE_SELL' | 'MULTI_WHALE' | 'ACCUMULATION';
  walletId?: string;      // UUID, null for MULTI_WHALE
  tokenAddress?: string;
  tokenSymbol?: string;
  chain: string;
  usdValue?: number;
  confidenceScore: number; // 0.70, 0.85, 0.95
  txHash?: string;
  metadata?: object;
  createdAt: string;
}
```

### Signal Types

| Type | Description | Trigger |
|------|-------------|---------|
| `WHALE_BUY` | Whale buying tokens | USD >= $50K, to = tracked wallet |
| `WHALE_SELL` | Whale selling tokens | USD >= $50K, from = tracked wallet |
| `MULTI_WHALE` | 3+ whales buying | 3+ distinct wallets buy same token in 24h |
| `ACCUMULATION` | Wallet accumulating | 3+ buys, 0 sells for same token in 72h |

### Confidence Tiers

| Tier | Value |
|------|-------|
| Low (3 count / $50K-100K) | 0.70 |
| Medium (4 count / $100K-500K) | 0.85 |
| High (5+ count / $500K+) | 0.95 |

---

## TypeScript Types

```typescript
// Wallet
interface Wallet {
  id: string;
  address: string;
  label?: string;
  type: 'WHALE' | 'SMART_MONEY' | 'INFLUENCER' | 'UNKNOWN';
  chain: string;
  pnl?: number;
  winRate?: number;
  createdAt: string;
  updatedAt?: string;
}

// Asset Transfer
interface AssetTransfer {
  id: number;
  walletId: string;
  txHash: string;
  blockNumber: number;
  from: string;
  to: string;
  category: 'external' | 'erc20' | 'erc721' | 'erc1155';
  value: number;
  assetSymbol: string;
  tokenAddress?: string;
  usdValue?: number;
  txTimestamp: string;
}

// API Response wrapper
interface ApiResponse<T> {
  data: T;
  error?: string;
}
```

---

## Error Responses

```typescript
interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
```

Common errors:
- `400` - Invalid request (bad address format)
- `404` - Resource not found
- `409` - Conflict (wallet already exists)
- `500` - Internal server error

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                   Frontend                       │
│  (Next.js + shadcn/ui + TanStack Query)         │
└──────────────────────┬──────────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────────┐
│              Argus Backend (Spring Boot)         │
├─────────────────────────────────────────────────┤
│  API Layer      │ /wallets, /transactions, etc. │
│  Domain Layer   │ Services, Signal Detection    │
│  Infra Layer    │ Alchemy, DexScreener, Redis   │
│  Storage        │ PostgreSQL, Redis Streams     │
└─────────────────────────────────────────────────┘
```

---

## Real-time Features (Future)

The backend uses Redis Streams for real-time processing:
- New blocks monitored every 12 seconds
- Transactions published to `argus:transactions` stream
- Signals generated and stored in PostgreSQL

For real-time frontend updates, consider:
- Polling `/wallets/{id}/signals` every 30s
- WebSocket endpoint (not implemented yet)
- Server-Sent Events (not implemented yet)
