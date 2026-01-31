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
- [X] Create `Wallet` entity
- [X] Create `Transaction` entity
- [X] Create `Token` entity
- [X] Create `Signal` entity
- [X] Set up JPA repositories
- [X] Test with H2 for unit tests

#### Day 4 (3 hrs) - Blockchain Port & Adapter
- [X] Define `BlockchainPort` interface in `domain/port/`
- [X] Create `Web3jBlockchainAdapter` in `infra/blockchain/`
- [X] Create Alchemy/Infura account (free tier)
- [X] Configure RPC endpoint in application.yml
- [X] Implement `getLatestBlockNumber()` method
- [X] Implement `getTransactionByHash()` method
- [X] Test via Spring dependency injection

#### Day 5 (3 hrs) - Basic REST API
- [X] Create `WalletController` (CRUD endpoints)
- [X] Create `WalletService`
- [X] Implement add/remove wallet endpoints
- [X] Implement get wallet by address
- [X] Add Swagger/OpenAPI documentation
- [X] Test with Postman/curl

#### Day 6 (3 hrs) - Spring Scheduling Integration
- [X] Configure Spring @Scheduled with thread pool
- [X] Create `BlockMonitorJob` (runs every 12 seconds)
- [X] Create `JobSchedulerConfig` with @EnableScheduling
- [X] Verify job execution and logging
- [X] ~~Configure Redis Streams~~ (Moved to Day 15)

#### Day 7 (3 hrs) - Review & Refactor
- [X] Code review of week's work
- [ ] Add missing tests
- [X] Fix any bugs found
- [X] Update documentation
- [X] Plan adjustments for week 2

---

### Week 2: Blockchain Data Layer

#### Day 8 (3 hrs) - Transaction Fetching
- [X] Implement `getTransactionsByBlock(blockNumber)`
- [X] Parse transaction data (from, to, value, input)
- [X] Decode ERC20 Transfer events
- [X] Store sample transactions in DB
- [X] Test with known transaction hashes

#### Day 9 (3 hrs) - Transaction Decoding
- [X] Add DEX router ABIs (Uniswap V2/V3)
- [X] Implement swap detection logic (RouterDefinitions)
- [X] Decode ALL 6 Uniswap V2 functions:
  - [X] swapExactTokensForTokens
  - [X] swapTokensForExactTokens
  - [X] swapExactETHForTokens / swapTokensForExactETH
  - [X] swapExactTokensForETH / swapETHForExactTokens
- [X] Extract: tokenIn, tokenOut, amountIn/Out, path, recipient, deadline
- [X] Performance optimizations (static TypeReference, switch dispatch)
- [X] Create TransactionService (service layer architecture)
- [X] Integrate DexDecoder with API (Controller → Service → Decoder)
- [X] Test with real Uniswap transactions on live network (infrastructure validated)

#### Day 10 (3 hrs) - Wallet Transaction History
- [X] Implement `getWalletTransactions(address)`
- [X] Use Alchemy's `alchemy_getAssetTransfers` API
- [X] Parse and normalize response
- [X] Store wallet transaction history
- [X] Build wallet timeline view

#### Day 11 (3 hrs) - Token Price Integration
- [X] Integrate CoinGecko/DexScreener API
- [X] Create `PriceService`
- [X] Implement `getTokenPrice(address)` 
- [X] Add price caching (Redis)
- [X] Calculate USD value for transactions

#### Day 12 (3 hrs) - Enhance Block Monitoring Job
- [X] ~~Create BlockMonitorJob~~ (Already done in Day 6)
- [X] ~~Configure to run every 12 seconds~~ (Already done)
- [X] Track last processed block in Redis (avoid re-processing)
- [X] Filter transactions for tracked wallets only
- [X] Save all relevant transactions (not just samples)
- [X] Wallet address caching in Redis (5-min TTL)
- [X] Max catchup limit (100 blocks) to prevent DoS on restart
- [ ] ~~Add circuit breaker pattern~~ (Moved to Day 15 with Redis Streams)

#### Day 13 (3 hrs) - ~~Wallet Monitoring Job~~ Auto-Sync & Polish
- [x] ~~Create WalletScannerJob~~ (REMOVED - redundant with BlockMonitorJob)
- [x] Auto-sync wallet history on creation (virtual thread - Java 21)
- [x] Invalidate wallet cache on create/delete
- [x] Unit tests for BlockMonitorJob (2 tests)

#### Day 14 (3 hrs) - End of Phase 1 Review
- [X] Add USD value enrichment
- [ ] Integration testing of all components
- [X] Load test with 10 wallets
- [ ] Fix bugs and edge cases
- [X] Document API endpoints
- [X] Prepare for Phase 2

---

## Phase 2: Core Pipeline (Days 15-28)

### Week 3: Event Processing

#### Day 15 (3 hrs) - Redis Streams Setup
- [X] Configure Spring Data Redis Streams
- [X] Create streams: `argus:transactions`, `argus:signals`
- [X] Implement `StreamPublisher` for publishing events
- [X] Implement `StreamConsumer` with consumer groups
- [] Test producer/consumer flow
- [] Document Redis Streams setup

#### Day 16 (3 hrs) - Event-Driven Architecture
- [X] Publish transactions to Redis Streams on detection
- [X] Create `TransactionProcessor` consumer
- [X] Implement transaction enrichment (add prices)
- [X] Store enriched transactions
- [X] Add error handling and retry logic

#### Day 17 (3 hrs) - Whale Detection Logic
- [X] Define whale threshold ($50K+)
- [X] Create `WhaleDetectorService`
- [X] Implement `isWhaleTrade(transaction)` logic
- [X] Generate WHALE_BUY / WHALE_SELL signals
- [X] Store signals in database

#### Day 18 (3 hrs) - Signal Data Model
- [X] Enhance `Signal` entity with all fields
- [X] Add signal types enum
- [X] Create `SignalRepository` with queries
- [X] Add signal metadata (JSON field)
- [ ] Test signal creation flow

#### Day 19 (3 hrs) - Multi-Whale Detection
- [X] Track whale activity per token (24h window)
- [X] Detect when 3+ whales buy same token
- [X] Generate MULTI_WHALE signal
- [X] Add correlation logic
- [ ] Test with mock data

#### Day 20 (3 hrs) - Accumulation Pattern
- [X] Track wallet-token buy history
- [X] Detect 3+ buys without sells (72h)
- [X] Calculate total position size
- [X] Generate ACCUMULATION signal
- [ ] Test pattern detection

#### Day 21 (3 hrs) - Week 3 Review
- [ ] End-to-end pipeline test
- [ ] Monitor Kafka message flow
- [ ] Check signal accuracy
- [ ] Performance optimization
- [ ] Bug fixes

---

### Week 4: Wallet Intelligence

#### Day 22 (3 hrs) - Historical Data Import & Price Tracking
- [x] Add `price_at_tx` column to asset_transfers (V12 migration)
- [x] Fetch past 30 days of transactions for tracked wallets
- [x] Enrich with USD price at transaction time
- [x] Bulk import with batch processing
- [x] Verify data accuracy

#### Day 23 (3 hrs) - Wallet Statistics 
- [X] Create `wallet_stats` table (V13 migration)
- [X] Implement PnL calculation (realized only, Average Cost Method)
- [X] Calculate win rate (profitable trades %)
- [X] Calculate average ROI per position
- [X] Store wallet stats in DB
- [X] Add API endpoints: POST /{id}/calculate-stats, GET /{id}/stats
- [X] Add WalletStatsResponse DTO with per-token breakdown


#### Day 24 (3 hrs) - Wallet Labeling System
- [x] Create `address_labels` table (V14 migration) - Address Book pattern
- [x] Implement `AddressBookService` with CRUD + bulk import
- [x] Add domain exceptions: `LabelNotFoundException`, `MaxLabelsExceededException`, `LabelAlreadyExistsException`
- [x] Create `AddressBookController` with REST API
- [x] Import known entity labels (CSV seed data)
- [x] Add label search & filter (unified `search()` method)

#### Day 25 (3 hrs) - Smart Money Scoring
- [x] Create `wallet_metrics` table (V15 migration) with archetype classification
- [x] Implement `SmartMoneyScoringService` with waterfall archetype classification:
  - MEV_BOT → WHALE → ACCUMULATOR → HOME_RUN → SNIPER → UNKNOWN
- [x] Calculate raw metrics from transfer history (profit factor, max ROI, hold time)
- [x] Implement 3 scoring dimensions:
  - PnL Score (logarithmic scale 0-100)
  - Consistency Score (win rate + trade volume)  
  - Conviction Score (buy dominance × hold time)
- [x] Create `WalletStatsAggregator` domain service (refactored from adapter)
- [x] Fix N+1 query in `WalletStatsPersistenceAdapter.saveAll()` with batch upsert
- [x] Add address normalization and null-safety to persistence adapter


#### Day 26 (3 hrs) - Smart Money API & Scheduled Jobs
- [x] Create `SmartMoneyController` with REST endpoints:
  - `GET /api/smart-money` - List all scored wallets (paginated, filterable by archetype)
  - `GET /api/smart-money/{address}` - Get wallet metrics & archetype
  - `GET /api/smart-money/top` - Top wallets by score (configurable: pnl/consistency/conviction)
  - `POST /api/smart-money/{address}/refresh` - Manually trigger metrics recalculation
- [x] Create `SmartMoneyRefreshJob` (scheduled @Cron daily at 2AM UTC)
  - Call `SmartMoneyScoringService.refreshAllMetrics()`
  - Log summary: total processed, errors, time elapsed
- [x] Add SmartMoneyResponse DTO with tier badge (S/A/B/C)
- [x] Write API tests for all endpoints
- [x] Create `SmartMoneyApi` OpenAPI interface (`src/main/java/com/argus/api/spec/`)
- [x] Add `WalletNotFoundException` handler to `GlobalExceptionHandler`

#### Day 27 (3 hrs) - Smart Money Signal Integration
- [ ] Enhance signal generation to include wallet archetype context:
  - When SNIPER wallet buys → Generate `SNIPER_ALPHA` signal
  - When WHALE + HOME_RUN buy same token → Generate `SMART_MONEY_CONVERGENCE` signal
- [ ] Create `SmartMoneySignalEnricher` service
  - Lookup wallet metrics when processing transactions
  - Attach archetype + scores to signal metadata
- [ ] Add filtering: Optionally ignore MEV_BOT transactions from alerts
- [ ] Update signal response DTOs with smart money context

#### Day 28 (3 hrs) - Week 4 Integration & Verification
- [ ] Full pipeline integration test:
  1. Track wallet → Sync history → Calculate PnL → Score wallet → Generate signal
- [ ] Verify scoring accuracy with known wallets (find 3 real "sniper" wallets on Etherscan)
- [ ] Performance benchmark: Time to score 1000 wallets
- [ ] Create `docs/smart_money_scoring.md` documenting:
  - Archetype definitions & criteria
  - Scoring formulas explained
  - API reference
- [ ] Plan Week 5 (Alerts & Notifications)

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
