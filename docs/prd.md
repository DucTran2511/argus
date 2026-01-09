# On-Chain Alpha Hunter - Product Requirements Document

## Table of Contents
1. [Vision & Goals](#vision--goals)
2. [Problem Statement](#problem-statement)
3. [Target Users](#target-users)
4. [Domain Concepts](#domain-concepts)
5. [Feature Specifications](#feature-specifications)
6. [User Journeys](#user-journeys)
7. [Success Metrics](#success-metrics)
8. [Risks & Mitigations](#risks--mitigations)

---

## Vision & Goals

### Vision Statement
> Democratize crypto alpha by making smart money movements visible to everyone in real-time.

### Project Goals

| Goal | Description | Success Criteria |
|------|-------------|------------------|
| **Information Edge** | Give users early signals before mainstream awareness | Signals arrive 10+ minutes before Twitter |
| **Automation** | Eliminate manual wallet checking | 24/7 monitoring without user intervention |
| **Actionable Insights** | Not just data, but recommendations | Each signal includes context & risk assessment |
| **Learning Tool** | Help users understand smart money behavior | Users can explain why whales make certain moves |

### What This Project IS vs IS NOT

| IS ✅ | IS NOT ❌ |
|-------|----------|
| Monitoring & alerting tool | Trading bot (no auto-trading) |
| Intelligence platform | Financial advice service |
| Educational resource | Get-rich-quick scheme |
| Data aggregator | Blockchain node/infrastructure |

---

## Problem Statement

### The Information Asymmetry Problem

```
┌─────────────────────────────────────────────────────────────────┐
│                    CRYPTO MARKET INFORMATION FLOW               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  INSIDERS          VCs           WHALES         RETAIL          │
│     │               │              │               │            │
│     │  (minutes)    │  (hours)     │  (hours)      │  (days)    │
│     ▼               ▼              ▼               ▼            │
│  ╔═════╗        ╔═════╗       ╔═════╗        ╔═════╗           │
│  ║ Buy ║───────▶║ Buy ║──────▶║ Buy ║───────▶║ Buy ║           │
│  ╚═════╝        ╚═════╝       ╚═════╝        ╚═════╝           │
│                                                  │              │
│                                                  ▼              │
│                                          Price already up       │
│                                          Opportunity missed     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**The Core Problem**: By the time regular traders hear about opportunities through Twitter, Discord, or news, the "smart money" has already bought in. The information flows from insiders → VCs → whales → retail with significant delays.

### Why This Matters

| Without Early Information | With Early Information |
|---------------------------|------------------------|
| Buy after 100% pump | Buy during accumulation phase |
| Exit during crash (reactive) | Exit when whales start selling (proactive) |
| Follow influencer shills | Follow actual money movement |
| Emotional decisions | Data-driven decisions |

### Existing Solutions & Gaps

| Tool | What It Does | Gap |
|------|--------------|-----|
| **Etherscan** | View transactions | No alerts, manual checking |
| **Nansen** | Wallet labels, smart money | $150+/month, complex UI |
| **Arkham** | Entity intelligence | Focus on investigation, not trading |
| **DeBank** | Portfolio tracking | No signal generation |
| **Twitter/CT** | Social alpha | Delayed, noisy, biased |

**Your Opportunity**: Build a focused, affordable tool that specifically tracks whale movements and generates actionable trading signals.

---

## Target Users

### Primary Persona: "Active Crypto Trader"

```
Name: Alex
Age: 25-40
Experience: 1-3 years in crypto
Portfolio: $10k - $100k
Time: Can check phone/computer multiple times per day
Goal: Beat the market by getting information early
Pain: Spends hours checking wallets manually, misses opportunities
```

**Alex's Day**:
1. Wake up, check Twitter for alpha
2. Manually check 5-10 whale wallets on Etherscan
3. See a whale bought something, but it's already up 50%
4. Feel frustrated, repeat next day

**What Alex Wants**:
- "Just tell me when smart money moves"
- "Alert me on Telegram so I don't miss it"
- "Give me context, not just raw data"

### Secondary Personas

| Persona | Description | Key Need |
|---------|-------------|----------|
| **Research Analyst** | Studies market trends | Historical data, patterns |
| **DeFi Yield Farmer** | Optimizes yield strategies | Track where TVL is flowing |
| **NFT Trader** | Trades NFTs | Track whale NFT wallets |
| **Beginner** | Learning crypto | Education, simple UI |

---

## Domain Concepts

> **Read this section carefully** - these are concepts you'll encounter while building.

### Blockchain Basics

| Concept | Explanation | Why It Matters |
|---------|-------------|----------------|
| **Wallet** | Like a bank account address (e.g., `0x1234...abcd`) | We track these |
| **Transaction (TX)** | A record of action on blockchain | What we monitor |
| **Block** | A batch of transactions (every ~12 sec on Ethereum) | Timing reference |
| **Gas** | Fee paid to process transactions | Affects profitability |
| **Token** | Digital asset on blockchain | What whales buy/sell |
| **Smart Contract** | Code that runs on blockchain | Tokens, DEXes are contracts |

### Trading Concepts

| Concept | Explanation | Example |
|---------|-------------|---------|
| **DEX** | Decentralized Exchange (no middleman) | Uniswap, SushiSwap |
| **CEX** | Centralized Exchange (company runs it) | Binance, Coinbase |
| **Swap** | Exchange one token for another | ETH → USDC |
| **Liquidity** | How easily you can buy/sell | High = easy, Low = risky |
| **Slippage** | Price change during trade | High slippage = bad |

### Alpha Hunting Concepts

| Concept | Explanation | Signal Type |
|---------|-------------|-------------|
| **Whale** | Wallet with large holdings (>$1M) | Whale buy = bullish |
| **Smart Money** | Wallets with consistent profits | Follow their trades |
| **Accumulation** | Buying slowly over time | Strong conviction signal |
| **Distribution** | Selling slowly over time | Exit signal |
| **Fresh Wallet** | New wallet receiving tokens | Often insider |
| **Rug Pull** | Dev abandons project, price crashes | Risk to detect |

### Data Sources

```
┌─────────────────────────────────────────────────────────────────┐
│                    WHERE DATA COMES FROM                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │  Blockchain │     │   DEX APIs  │     │ Price APIs  │       │
│  │   (RPC)     │     │             │     │             │       │
│  └──────┬──────┘     └──────┬──────┘     └──────┬──────┘       │
│         │                   │                   │               │
│  • Transactions      • Trade history    • Token prices         │
│  • Token transfers   • Liquidity data   • Volume               │
│  • Contract events   • Pool info        • Market cap           │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                   │
│                             ▼                                   │
│                   ┌─────────────────┐                          │
│                   │   YOUR SYSTEM   │                          │
│                   └─────────────────┘                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

RPC Providers: Alchemy, Infura, QuickNode (give you blockchain access)
DEX APIs: Uniswap Subgraph, 0x API
Price APIs: CoinGecko, DexScreener, GeckoTerminal
```

---

## Feature Specifications

### Feature 1: Wallet Tracking

**Goal**: Let users monitor any blockchain wallet and see all its activity.

**User Stories**:
- As a user, I want to add a wallet address so I can track its transactions
- As a user, I want to label wallets (e.g., "Vitalik", "Whale #1") so I can remember who they are
- As a user, I want to see a wallet's transaction history so I can understand their trading patterns
- As a user, I want to see a wallet's current holdings so I know their portfolio

**Detailed Requirements**:

| Requirement | Details |
|-------------|---------|
| Add wallet | Input address, auto-detect chain (ETH/Base/etc.), add label |
| View transactions | Show: time, type (buy/sell/transfer), token, amount, USD value |
| View holdings | Current tokens held, USD value, % portfolio |
| Wallet stats | Total PnL, win rate, avg hold time |
| Grouping | Create folders/groups of wallets |

**UI Mockup (Conceptual)**:
```
┌─────────────────────────────────────────────────────────────────┐
│  🐋 Wallet: 0x1234...abcd                      Label: "Whale A" │
├─────────────────────────────────────────────────────────────────┤
│  Holdings: $2.4M    │  PnL (30d): +$340K   │  Win Rate: 72%     │
├─────────────────────────────────────────────────────────────────┤
│  Recent Transactions                                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🟢 BUY  │ 2 min ago │ PEPE     │ $150,000 │ +3.2% since   │ │
│  │ 🔴 SELL │ 1 hr ago  │ SHIB     │ $80,000  │ realized +45% │ │
│  │ 🟢 BUY  │ 3 hr ago  │ ARB      │ $200,000 │ -1.5% since   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

### Feature 2: Signal Detection

**Goal**: Automatically detect noteworthy events and create actionable signals.

**Signal Types**:

| Signal | Trigger | Why It Matters |
|--------|---------|----------------|
| **Whale Buy** | Wallet buys > $50K | Smart money entering |
| **Whale Sell** | Wallet sells > $50K | Smart money exiting |
| **Accumulation** | Same wallet buys 3+ times in 24h | High conviction |
| **Multi-Whale** | 3+ whales buy same token in 24h | Coordinated/trending |
| **New Token Alert** | Whale buys token < 7 days old | Early opportunity |
| **CEX Outflow** | Large withdrawal from exchange | Bullish (holding) |
| **CEX Inflow** | Large deposit to exchange | Bearish (selling soon) |

**Signal Data Structure**:
```
Signal {
  id: "sig_12345"
  type: "whale_buy"
  timestamp: "2024-01-15T10:30:00Z"
  
  wallet: {
    address: "0x1234...abcd"
    label: "Whale A"
    win_rate: 72%
    historical_roi: +340%
  }
  
  token: {
    address: "0xtoken..."
    symbol: "PEPE"
    price: $0.0000012
    market_cap: $5.2M
    liquidity: $1.2M
    age: "14 days"
  }
  
  trade: {
    amount_usd: $150,000
    amount_tokens: 125,000,000,000
    price_impact: 2.3%
  }
  
  context: {
    other_whales_holding: 3
    whale_holding_pct: 12%
    price_change_24h: +45%
  }
  
  ai_summary: "High-conviction buy from top performer..."
  risk_score: "medium"
}
```

---

### Feature 3: Real-Time Alerts

**Goal**: Notify users immediately when signals match their criteria.

**Alert Channels**:
| Channel | Use Case |
|---------|----------|
| Telegram Bot | Primary, most traders use this |
| Discord Webhook | For communities |
| Email | Daily digest |
| In-App | Dashboard notifications |

**Alert Rule Configuration**:
```
Rule: "Whale Buys > $100K on New Tokens"
├── Trigger: signal.type == "whale_buy"
├── Conditions:
│   ├── signal.trade.amount_usd >= 100000
│   ├── signal.token.age <= 7 days
│   └── signal.wallet.win_rate >= 60%
├── Channels: [telegram, discord]
└── Enabled: true
```

**Telegram Alert Format**:
```
🐋 WHALE BUY ALERT

Token: $PEPE (0x1234...abcd)
Wallet: Whale A (72% win rate)
Amount: $150,000

📊 Token Stats:
• Price: $0.0000012 (+45% 24h)
• Market Cap: $5.2M
• Liquidity: $1.2M
• Age: 14 days

⚠️ Risk: Medium
💡 3 other whales holding

[View on Dashboard] [View on Etherscan]
```

---

### Feature 4: Dashboard

**Goal**: Central hub to view all data and manage settings.

**Dashboard Pages**:

| Page | Content |
|------|---------|
| **Home/Feed** | Live signal stream, filters |
| **Wallets** | Manage tracked wallets |
| **Token Explorer** | Look up any token, see who holds it |
| **Alerts** | Configure alert rules |
| **Portfolio** | (Optional) Track your own trades |
| **Settings** | API keys, notifications, subscription |

**Home Feed Wireframe**:
```
┌─────────────────────────────────────────────────────────────────┐
│  ON-CHAIN ALPHA HUNTER                    🔔 Alerts │ ⚙️ Settings │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  FILTERS: [All Signals ▼] [All Chains ▼] [Min $50K ▼] [🔍]     │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🟢 2 min ago │ WHALE BUY                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🐋 Whale A bought $150K of $PEPE                           │ │
│  │ Token: $0.0000012 (+45% 24h) │ Risk: Medium                │ │
│  │ "High-conviction buy from top performer. 3 whales hold..." │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  🔴 15 min ago │ WHALE SELL                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🐋 VC Fund X sold $500K of $ARB                             │ │
│  │ Realized: +120% │ Held for: 45 days                         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  🆕 1 hr ago │ NEW TOKEN                                        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Smart money entering new token $NEWCOIN (3 days old)        │ │
│  │ 2 whales, combined position: $80K │ Risk: High              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### Feature 5: AI Insights (Phase 2)

**Goal**: Use LLM to provide context and explanations.

**AI Use Cases**:

| Use Case | Input | Output |
|----------|-------|--------|
| **Signal Summary** | Transaction data | "Why is this interesting?" |
| **Risk Assessment** | Token data | Rug pull probability |
| **Pattern Matching** | Historical signals | "Similar to SHIB before pump" |
| **Weekly Digest** | Week's activity | AI-written newsletter |

**Example AI Prompt**:
```
Given this whale trade:
- Wallet: Known VC with 72% win rate
- Bought: $150K of PEPE (meme coin)
- Token age: 14 days
- Other whales: 3 holding
- Recent price: +45% in 24h

Generate a 2-3 sentence analysis explaining:
1. Why this trade is notable
2. What the risk factors are
3. What similar past situations looked like
```

---

## User Journeys

### Journey 1: New User Setup

```
┌─────────────────────────────────────────────────────────────────┐
│                    NEW USER ONBOARDING                           │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
   ┌─────────┐
   │ Sign Up │ ─── Email + Password
   └────┬────┘
        │
        ▼
   ┌─────────────────┐
   │ Connect Telegram│ ─── Optional but recommended
   └────────┬────────┘
        │
        ▼
   ┌─────────────────┐
   │ Add First Wallet│ ─── Suggest popular whales to follow
   └────────┬────────┘
        │
        ▼
   ┌─────────────────┐
   │ Set Alert Rules │ ─── Pre-configured defaults
   └────────┬────────┘
        │
        ▼
   ┌─────────────────┐
   │ View Live Feed  │ ─── Dashboard with real-time signals
   └─────────────────┘
```

### Journey 2: Receiving and Acting on Alert

```
1. User is doing something else (sleeping, working)
                │
                ▼
2. Whale buys $200K of new token
                │
                ▼
3. System detects, creates signal (within 30 seconds)
                │
                ▼
4. Alert sent to Telegram
                │
                ▼
5. User sees alert on phone
   ├── Checks signal details
   ├── Views token info
   ├── Decides to buy (external action)
   └── Tracks outcome in app
```

---

## Success Metrics

### Product Metrics

| Metric | Target (MVP) | How to Measure |
|--------|--------------|----------------|
| Signal Latency | < 60 seconds | Time from on-chain TX to alert |
| Alert Delivery | > 99% | Telegram delivery success |
| Daily Active Users | 100+ | Unique logins/alerts opened |
| Wallets Tracked | 1000+ | Total across all users |
| Signal Accuracy | 60%+ profitable | Track signal outcome over 24h |

### Technical Metrics

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Uptime | 99.5% | Monitoring |
| API Response Time | < 200ms | APM |
| Data Freshness | < 2 blocks behind | Compare to chain head |

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| **RPC Rate Limits** | Can't fetch data | High | Multiple providers, caching |
| **Blockchain Reorgs** | False signals | Medium | Wait 2+ confirmations |
| **Whale Detection Gaming** | False signals | Medium | Multi-factor verification |
| **Legal/Compliance** | Shutdown | Low | Clear disclaimers, no financial advice |
| **Data Costs** | High expenses | Medium | Efficient caching, tiered access |
| **Competition** | Users leave | Medium | Unique features, better UX, lower price |

---

## Next Document: Domain Knowledge Guide

The next document will cover:
- Detailed blockchain concepts
- How to read transactions
- Understanding DEX mechanics
- RPC/API usage guide
- Common patterns to detect
