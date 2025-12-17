# Argus - Development Roadmap

> 3 hours/day development plan

---

## Overview

| Phase | Duration | Focus |
|-------|----------|-------|
| **Phase 1** | Week 1-2 (14 days) | Project Setup & Foundation |
| **Phase 2** | Week 3-4 (14 days) | Core Data Pipeline |
| **Phase 3** | Week 5-6 (14 days) | Signal Detection & Alerts |
| **Phase 4** | Week 7-8 (14 days) | Dashboard & AI Integration |

**Total: ~8 weeks (168 hours @ 3hr/day)**

---

## Phase 1: Foundation (Days 1-14)

### Week 1: Project Setup

#### Day 1 (3 hrs) - Project Initialization
- [X] Create Spring Boot project via Spring Initializr
- [X] Configure pom.xml with dependencies
- [X] Create package structure:
  - `com.argus.core/` (utilities)
  - `com.argus.domain/` (business logic + ports)
  - `com.argus.infra/` (adapters)
  - `com.argus.api/` (controllers)
  - `com.argus.job/` (scheduled jobs)
- [X] Create `application.yml` with initial config
- [X] Git init + first commit

```
Project Structure:
argus/
├── pom.xml
└── src/main/java/com/argus/
    ├── core/           # Utils, exceptions
    ├── domain/         # Business logic + Port interfaces
    │   ├── model/
    │   ├── service/
    │   └── port/       # BlockchainPort, AiPort, etc.
    ├── infra/          # Implements ports
    │   ├── blockchain/
    │   ├── ai/
    │   └── notification/
    ├── api/            # REST Controllers
    └── job/            # Scheduler jobs
```

#### Day 2 (3 hrs) - Docker & Database Setup
- [X] Create `docker-compose.yml` (PostgreSQL, Redis)
- [-] Set up TimescaleDB extension
- [X] Create initial database migration (Flyway/Liquibase)
- [X] Test database connection from Spring Boot
- [X] Document local setup in README

#### Day 3 (3 hrs) - Domain Models
- [ ] Create `Wallet` entity
- [ ] Create `Transaction` entity
- [ ] Create `Token` entity
- [ ] Create `Signal` entity
- [ ] Set up JPA repositories
- [ ] Test with H2 for unit tests

#### Day 4 (3 hrs) - Blockchain Port & Adapter
- [ ] Define `BlockchainPort` interface in `domain/port/`
- [ ] Create `Web3jBlockchainAdapter` in `infra/blockchain/`
- [ ] Create Alchemy/Infura account (free tier)
- [ ] Configure RPC endpoint in application.yml
- [ ] Implement `getLatestBlockNumber()` method
- [ ] Implement `getTransactionByHash()` method
- [ ] Test via Spring dependency injection

#### Day 5 (3 hrs) - Basic REST API
- [ ] Create `WalletController` (CRUD endpoints)
- [ ] Create `WalletService`
- [ ] Implement add/remove wallet endpoints
- [ ] Implement get wallet by address
- [ ] Add Swagger/OpenAPI documentation
- [ ] Test with Postman/curl

#### Day 6 (3 hrs) - Your Job Scheduler Integration
- [ ] Import your distributed job scheduler library
- [ ] Configure scheduler connection
- [ ] Create first test job: "hello-world-job"
- [ ] Verify job execution and logging
- [ ] Document job configuration

#### Day 7 (3 hrs) - Review & Refactor
- [ ] Code review of week's work
- [ ] Add missing tests
- [ ] Fix any bugs found
- [ ] Update documentation
- [ ] Plan adjustments for week 2

---

### Week 2: Blockchain Data Layer

#### Day 8 (3 hrs) - Transaction Fetching
- [ ] Implement `getTransactionsByBlock(blockNumber)`
- [ ] Parse transaction data (from, to, value, input)
- [ ] Decode ERC20 Transfer events
- [ ] Store sample transactions in DB
- [ ] Test with known transaction hashes

#### Day 9 (3 hrs) - Transaction Decoding
- [ ] Add DEX router ABIs (Uniswap V2/V3)
- [ ] Implement swap detection logic
- [ ] Decode `swapExactTokensForTokens` function
- [ ] Extract: tokenIn, tokenOut, amountIn, amountOut
- [ ] Test with real Uniswap transactions

#### Day 10 (3 hrs) - Wallet Transaction History
- [ ] Implement `getWalletTransactions(address)`
- [ ] Use Alchemy's `alchemy_getAssetTransfers` API
- [ ] Parse and normalize response
- [ ] Store wallet transaction history
- [ ] Build wallet timeline view

#### Day 11 (3 hrs) - Token Price Integration
- [ ] Integrate CoinGecko/DexScreener API
- [ ] Create `PriceService`
- [ ] Implement `getTokenPrice(address)` 
- [ ] Add price caching (Redis)
- [ ] Calculate USD value for transactions

#### Day 12 (3 hrs) - Block Monitoring Job
- [ ] Create `BlockMonitorJob` using your scheduler
- [ ] Configure to run every 12 seconds
- [ ] Fetch new blocks since last check
- [ ] Extract and store transactions
- [ ] Add job status logging

#### Day 13 (3 hrs) - Wallet Monitoring Job
- [ ] Create `WalletScannerJob`
- [ ] Fetch recent TXs for all tracked wallets
- [ ] Detect new transactions since last scan
- [ ] Store and flag new activity
- [ ] Configure schedule (every 1 minute)

#### Day 14 (3 hrs) - End of Phase 1 Review
- [ ] Integration testing of all components
- [ ] Load test with 10 wallets
- [ ] Fix bugs and edge cases
- [ ] Document API endpoints
- [ ] Prepare for Phase 2

---

## Phase 2: Core Pipeline (Days 15-28)

### Week 3: Event Processing

#### Day 15 (3 hrs) - Kafka Setup
- [ ] Add Kafka to docker-compose
- [ ] Configure Spring Kafka
- [ ] Create topics: `raw-transactions`, `signals`
- [ ] Test producer/consumer
- [ ] Document Kafka setup

#### Day 16 (3 hrs) - Event-Driven Architecture
- [ ] Publish transactions to Kafka on detection
- [ ] Create `TransactionProcessor` consumer
- [ ] Implement transaction enrichment (add prices)
- [ ] Store enriched transactions
- [ ] Add error handling and DLQ

#### Day 17 (3 hrs) - Whale Detection Logic
- [ ] Define whale threshold ($50K+)
- [ ] Create `WhaleDetectorService`
- [ ] Implement `isWhaleTrade(transaction)` logic
- [ ] Generate WHALE_BUY / WHALE_SELL signals
- [ ] Store signals in database

#### Day 18 (3 hrs) - Signal Data Model
- [ ] Enhance `Signal` entity with all fields
- [ ] Add signal types enum
- [ ] Create `SignalRepository` with queries
- [ ] Add signal metadata (JSON field)
- [ ] Test signal creation flow

#### Day 19 (3 hrs) - Multi-Whale Detection
- [ ] Track whale activity per token (24h window)
- [ ] Detect when 3+ whales buy same token
- [ ] Generate MULTI_WHALE signal
- [ ] Add correlation logic
- [ ] Test with mock data

#### Day 20 (3 hrs) - Accumulation Pattern
- [ ] Track wallet-token buy history
- [ ] Detect 3+ buys without sells (72h)
- [ ] Calculate total position size
- [ ] Generate ACCUMULATION signal
- [ ] Test pattern detection

#### Day 21 (3 hrs) - Week 3 Review
- [ ] End-to-end pipeline test
- [ ] Monitor Kafka message flow
- [ ] Check signal accuracy
- [ ] Performance optimization
- [ ] Bug fixes

---

### Week 4: Wallet Intelligence

#### Day 22 (3 hrs) - Wallet Statistics
- [ ] Implement PnL calculation
- [ ] Calculate win rate (profitable trades %)
- [ ] Calculate average ROI
- [ ] Store wallet stats in DB
- [ ] Schedule daily recalculation job

#### Day 23 (3 hrs) - Wallet Labeling System
- [ ] Create `WalletLabel` entity
- [ ] Implement manual labeling API
- [ ] Import known wallet labels (CSV)
- [ ] Add label search & filter
- [ ] Build label management UI (later)

#### Day 24 (3 hrs) - Smart Money Scoring
- [ ] Define smart money criteria
- [ ] Implement `calculateSmartMoneyScore(wallet)`
- [ ] Weight: win rate, ROI, consistency
- [ ] Rank wallets by score
- [ ] Auto-flag high performers

#### Day 25 (3 hrs) - Historical Data Import
- [ ] Fetch past 30 days of transactions for tracked wallets
- [ ] Bulk import and process
- [ ] Calculate historical stats
- [ ] Backfill missing price data
- [ ] Verify data accuracy

#### Day 26 (3 hrs) - Token Analysis
- [ ] Create `Token` entity with metadata
- [ ] Fetch token info (name, symbol, decimals)
- [ ] Calculate holder count
- [ ] Track whale holder percentage
- [ ] Add liquidity metrics

#### Day 27 (3 hrs) - Risk Scoring
- [ ] Define risk criteria (age, liquidity, holders)
- [ ] Implement `calculateRiskScore(token)`
- [ ] Categories: Low, Medium, High, Critical
- [ ] Add to signal output
- [ ] Test with known rug pulls

#### Day 28 (3 hrs) - Phase 2 Review
- [ ] Full pipeline integration test
- [ ] Validate signal quality
- [ ] Performance benchmarks
- [ ] Documentation update
- [ ] Plan Phase 3

---

## Phase 3: Alerts & Notifications (Days 29-42)

### Week 5: Alert System

#### Day 29 (3 hrs) - User & Auth
- [ ] Create `User` entity
- [ ] Implement basic authentication (JWT)
- [ ] Create registration/login endpoints
- [ ] Add user to wallet ownership
- [ ] Test auth flow

#### Day 30 (3 hrs) - Alert Rules
- [ ] Create `AlertRule` entity
- [ ] Define rule conditions schema (JSON)
- [ ] Implement rule CRUD API
- [ ] Add default rules for new users
- [ ] Test rule creation

#### Day 31 (3 hrs) - Rule Matching Engine
- [ ] Create `RuleMatcherService`
- [ ] Match signals against user rules
- [ ] Support conditions: type, minAmount, walletType
- [ ] Generate `Alert` when matched
- [ ] Store alerts in database

#### Day 32 (3 hrs) - Telegram Bot Setup
- [ ] Create Telegram bot via BotFather
- [ ] Add Telegram bot library
- [ ] Implement bot authentication flow
- [ ] Store user's Telegram chat ID
- [ ] Test sending message

#### Day 33 (3 hrs) - Telegram Alerts
- [ ] Create `TelegramAlertService`
- [ ] Format alert message (markdown)
- [ ] Send alerts on rule match
- [ ] Add rate limiting (max X per hour)
- [ ] Handle send failures

#### Day 34 (3 hrs) - Alert History & Status
- [ ] Track alert delivery status
- [ ] Create alert history API
- [ ] Implement read/unread status
- [ ] Add alert filtering
- [ ] Build alert stats

#### Day 35 (3 hrs) - Week 5 Review
- [ ] End-to-end alert flow test
- [ ] Test with real wallet monitoring
- [ ] Verify Telegram delivery
- [ ] Fix edge cases
- [ ] Update docs

---

### Week 6: Dashboard Foundation

#### Day 36 (3 hrs) - React Project Setup
- [ ] Initialize React + TypeScript project
- [ ] Set up routing (React Router)
- [ ] Configure API client (axios)
- [ ] Set up authentication context
- [ ] Create basic layout component

#### Day 37 (3 hrs) - Authentication UI
- [ ] Create Login page
- [ ] Create Register page
- [ ] Implement JWT storage
- [ ] Add auth guards
- [ ] Test login flow

#### Day 38 (3 hrs) - Signal Feed Page
- [ ] Create SignalFeed component
- [ ] Fetch signals from API
- [ ] Display signal cards
- [ ] Add loading/error states
- [ ] Implement auto-refresh

#### Day 39 (3 hrs) - Signal Card Component
- [ ] Design signal card UI
- [ ] Show: type, wallet, token, amount, time
- [ ] Add token logo/icon
- [ ] Link to Etherscan
- [ ] Color-code by type

#### Day 40 (3 hrs) - Wallet List Page
- [ ] Create WalletList component
- [ ] Display tracked wallets
- [ ] Add/remove wallet form
- [ ] Show wallet stats
- [ ] Add search/filter

#### Day 41 (3 hrs) - Wallet Detail Page
- [ ] Create WalletDetail component
- [ ] Show wallet info and stats
- [ ] Display transaction history
- [ ] Show holdings
- [ ] Link to signals from this wallet

#### Day 42 (3 hrs) - Phase 3 Review
- [ ] Test full user flow
- [ ] UI polish
- [ ] Fix responsive issues
- [ ] Performance check
- [ ] Plan Phase 4

---

## Phase 4: AI & Polish (Days 43-56)

### Week 7: AI Integration

#### Day 43 (3 hrs) - LangChain4j Setup
- [ ] Add LangChain4j dependency
- [ ] Configure OpenAI API key
- [ ] Create `AIService` class
- [ ] Test basic completion
- [ ] Set up prompt templates

#### Day 44 (3 hrs) - Signal Narrative Generation
- [ ] Create prompt for signal analysis
- [ ] Pass signal data to LLM
- [ ] Generate "Why is this interesting?" summary
- [ ] Store narrative with signal
- [ ] Test quality of outputs

#### Day 45 (3 hrs) - Risk Assessment AI
- [ ] Create risk analysis prompt
- [ ] Include token metrics in context
- [ ] Generate risk explanation
- [ ] Parse structured output
- [ ] Add to signal metadata

#### Day 46 (3 hrs) - AI Caching & Optimization
- [ ] Cache AI responses (Redis)
- [ ] Implement rate limiting
- [ ] Add fallback for API failures
- [ ] Track token usage/cost
- [ ] Optimize prompt length

#### Day 47 (3 hrs) - Pattern Matching
- [ ] Store historical signal outcomes
- [ ] Create embedding for signals
- [ ] Find similar past signals
- [ ] Generate "Similar to X which went up Y%"
- [ ] Test pattern matching accuracy

#### Day 48 (3 hrs) - Weekly Digest Job
- [ ] Create weekly summary job
- [ ] Aggregate week's top signals
- [ ] Generate AI summary
- [ ] Send via Telegram
- [ ] Store in database

#### Day 49 (3 hrs) - Week 7 Review
- [ ] Test all AI features
- [ ] Review narrative quality
- [ ] Cost optimization
- [ ] Performance tuning
- [ ] Bug fixes

---

### Week 8: Final Polish

#### Day 50 (3 hrs) - Dashboard Polish
- [ ] Add dark mode
- [ ] Improve typography
- [ ] Add animations
- [ ] Mobile responsive fixes
- [ ] Loading states

#### Day 51 (3 hrs) - Real-time Updates
- [ ] Set up WebSocket
- [ ] Push new signals to frontend
- [ ] Update feed in real-time
- [ ] Add notification sound/toast
- [ ] Test with multiple clients

#### Day 52 (3 hrs) - Alert Rule UI
- [ ] Create alert rule form
- [ ] Condition builder UI
- [ ] Enable/disable toggle
- [ ] Delete rule
- [ ] Test rule creation

#### Day 53 (3 hrs) - Token Search
- [ ] Create token search endpoint
- [ ] Search by symbol/address
- [ ] Show token details page
- [ ] Display whale holders
- [ ] Recent signals for token

#### Day 54 (3 hrs) - Settings Page
- [ ] Telegram connection UI
- [ ] Notification preferences
- [ ] Account settings
- [ ] Delete account
- [ ] Export data

#### Day 55 (3 hrs) - Testing & Bug Fixes
- [ ] Full regression testing
- [ ] Fix all known bugs
- [ ] Performance optimization
- [ ] Security review
- [ ] Update documentation

#### Day 56 (3 hrs) - Launch Preparation
- [ ] Deployment guide
- [ ] Environment configuration
- [ ] Final README update
- [ ] Create demo video
- [ ] 🎉 MVP COMPLETE!

---

## Daily Checklist Template

Copy this for each day:

```markdown
## Day X - [Date]

### Goals
- [ ] Task 1
- [ ] Task 2
- [ ] Task 3

### Time Log
- Start: __:__
- End: __:__
- Total: __ hrs

### Completed
- [x] ...

### Blockers
- ...

### Tomorrow
- ...
```

---

## Quick Reference

### Key Commands

```bash
# Start local services
docker-compose up -d

# Run backend
./mvnw spring-boot:run

# Run frontend
npm run dev

# View logs
docker-compose logs -f
```

### Important Links
- [Alchemy Dashboard](https://dashboard.alchemy.com)
- [Etherscan](https://etherscan.io)
- [Uniswap Info](https://info.uniswap.org)
- [CoinGecko API](https://www.coingecko.com/api/documentation)
