# Argus - Feature Planning (Post Phase 1 & 2)

> Analysis Date: Feb 26, 2026

---

## What's Already Built (Confirmed via Codebase Scan)

| Component | Status | Details |
|-----------|--------|---------|
| **Blockchain Monitoring** | ✅ Complete | `BlockMonitorJob` (12s polling), `Web3jBlockchainAdapter`, block tracking via Redis |
| **Transaction Pipeline** | ✅ Complete | Redis Streams → `TransactionStreamConsumer` → `WhaleDetectorService` → `SmartMoneySignalEnricher` |
| **DEX Decoding** | ✅ Complete | All 6 Uniswap V2 router functions decoded |
| **Signal Detection** | ✅ Complete | 7 signal types: `WHALE_BUY`, `WHALE_SELL`, `MULTI_WHALE`, `ACCUMULATION`, `SNIPER_ALPHA`, `SMART_MONEY_CONVERGENCE`, `HIGH_CONVICTION_BET` |
| **Smart Money Scoring** | ✅ Complete | Archetype classification (MEV_BOT, WHALE, SNIPER, etc.), 3-dimension scoring, tier system (S/A/B/C) |
| **Wallet Management** | ✅ Complete | User-scoped CRUD, auto-sync on create, PnL/win-rate stats |
| **Address Book** | ✅ Complete | Labels, CSV seed import, search/filter |
| **Auth** | ✅ Complete | Supabase JWT (ES256), user auto-provisioning, ownership enforcement |
| **REST APIs** | ✅ Complete | 6 controllers with OpenAPI specs: Wallet, Transaction, Signal, SmartMoney, AddressBook, Price |
| **Scheduled Jobs** | ✅ Complete | `BlockMonitorJob`, `PriceRetryJob`, `SmartMoneyRefreshJob` (daily 2AM) |

*Note: The frontend dashboard has been built in a separate workspace, and AI integration is no longer planned.*

---

## What's NOT Built Yet (Missing Features to Plan For)

### 1. 🔔 Alert Rules System — ✅ COMPLETE (`feat/alert-rules` branch)
> Implemented Feb 26, 2026. 27 new files + 4 modified, with TDD test suite.

**Development Checklist:**
- [x] Create `AlertRule` domain model
- [x] Create `Alert` domain model (and new `alerts` database table — V21 migration)
- [x] Implement `AlertRulePersistenceAdapter` and repository (JSONB via Jackson)
- [x] Implement `AlertRuleService` (CRUD for user rules + default rule provisioning)
- [x] Implement `RuleMatcherService` (pattern match a detected Signal against User rules)
  - Conditions: `signalType`, `minAmountUsd`, `walletArchetype`, `tokenAddresses`, `chains` — all AND-combined
- [x] Create `AlertRuleController` + `AlertController` for REST API exposure
- [x] Wire `RuleMatcherService` into `WhaleDetectorService` signal pipeline
- [x] Add `AlertService` (hexagonal pattern — controller→service→port)
- [x] Consolidate `GlobalExceptionHandler` to single `DomainException` handler
- [x] TDD test suite: `AlertConditionsTest`, `RuleMatcherServiceTest`, `AlertRuleControllerTest`, `AlertControllerTest`

### 2. 📱 Telegram Bot Notifications (High Priority)
> **Context:** The `User` model already has the `telegramChatId` field, and `application.properties` includes `argus.telegram.enabled=false`. The actual bot logic is missing.

**Development Checklist:**
- [ ] Register bot via Telegram's @BotFather
- [ ] Add `TelegramConfig.java` for bot token and rate limit settings
- [ ] Create `TelegramBotAdapter.java` for outbound messaging
- [ ] Implement `NotificationService.java` to route matched `Alert`s to Telegram
- [ ] Set up a webhook or polling bot endpoint to receive user commands (e.g., `/start` to link their account)

### 3. 🌐 Multi-Chain Support (Medium Priority)
> **Context:** The `chain` field exists on main entities (Wallet, Signal, Transaction), but only Ethereum (`mainnet`) is actively wired in.

**Development Checklist:**
- [ ] Support L2 RPC providers (e.g., Base, Arbitrum)
- [ ] Add network-specific router ABIs (e.g., Aerodrome on Base, Uniswap V3)
- [ ] Implement chain-aware price tracking mechanisms
- [ ] Deploy multiple `BlockMonitorJob` listeners per supported chain

### 4. 📊 New Signal Types (Medium Priority)
> **Context:** While complex signals like Accumulation and Convergence are built, some PRD signals are missing.

**Development Checklist:**
- [ ] `NEW_TOKEN_ALERT`: Detect when a whale buys a token less than 7 days old
- [ ] `CEX_OUTFLOW`: Large withdrawals from centralized exchanges
- [ ] `CEX_INFLOW`: Large deposits into exchanges (bearish / selling indicator)
- [ ] `DISTRIBUTION`: Reverse logic of `ACCUMULATION` for slow selling
- [ ] `FRESH_WALLET`: Detect funds sent to a new wallet immediately buying a token

---

## Recommended Order of Execution

1. **Fix Failing Tests (Tech Debt):** 4 errors currently in `BlockchainPortIntegrationTest` need addressing prior to any major pipeline changes.
2. **Build the Alert Rules Engine:** Since the DB table is already there, building the rules matcher unlocks real-time intelligence for the user.
3. **Implement Telegram Integration:** Plugs right into the Rules Engine so the user gets actual push notifications.
4. **Expand Signals & Chains:** Broaden the alpha detection surface.


┌─────────────────────────────────────────────────────────────┐
│  1.  Fix 4 failing tests                        (1 hr)     │
│  2.  Alert Rules System (model → service → API) (9 hrs)    │
│  3.  Telegram Bot Integration                   (6 hrs)    │
│  4.  Wire alerts into signal pipeline           (3 hrs)    │
│  5.  New signal types (NEW_TOKEN, DISTRIBUTION) (3 hrs)    │
│  6.  Multi-chain support (Base first)           (9 hrs)    │
│  7.  Backfill missing tests                     (ongoing)  │
└─────────────────────────────────────────────────────────────┘
