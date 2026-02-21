# Argus - Agent Context

## What is Argus?
Argus is an "On-Chain Alpha Hunter". It is a crypto intelligence bot that monitors blockchain transactions, detects whale/smart-money signals (like accumulations, multi-whale convergence, or sniper buys), and sends alerts via Telegram. 

## Architecture 
- **Hexagonal Architecture (Ports & Adapters)**
- **Domain layer is PURE** — No Spring or framework dependencies whatsoever.
- **Adapters** — Ports are pure interfaces located in `domain/port/`, and adapters implement them in the `infra/` layer.
- **Event-Driven Pipeline** — Uses Redis Streams (`argus:transactions`, `argus:signals`) for decoupling transaction ingestion from signal processing.

## Tech Stack
- **Backend:** Java 21, Spring Boot 3.5.x, Maven.
- **Database:** PostgreSQL with TimescaleDB extension (for time-series data) and Flyway for migrations.
- **Cache/Events:** Redis 7 (Caching + Streams for event processing).
- **Blockchain integration:** Web3j (Ethereum RPC).
- **AI Integration:** LangChain4j (planned) via OpenAI APIs.
- **Frontend:** Next.js 15, React, TypeScript (located in `src/main/frontend/` or similar location based on current structure).

## Key Conventions and Rules
1. **Financial Precision:** All monetary values (prices, amounts, PnL) MUST use `BigDecimal`. 
2. **Wallet Addresses:** Wallet addresses are strictly lowercase, `0x`-prefixed, and checksummed.
3. **Immutability & Domain Segregation:** Domain models must NOT leak JPA annotations (`@Entity`, `@Table`). Use dedicated persistence entities in `infra/persistence/entity/` and map them.
4. **Integration Layer:** Use ports for ALL external integrations (Blockchain RPCs, AI LLMs, Telegram API).

## Key Concepts & Domain Rules 
- **Whale:** Wallets with >$1M assets or making >$50k trades.
- **Smart Money:** Wallets with >60% historical win rates and strong PnL. Score is based on PnL, Consistency, and Conviction dimensions.
- **Signals:** Generated asynchronously. Types include `WHALE_BUY`, `WHALE_SELL`, `ACCUMULATION`, `MULTI_WHALE`, `SNIPER_ALPHA`, etc.

## How to Build & Test
- Run tests: `./mvnw clean test`
- Start infrastructure locally: `docker-compose up -d` (Includes Postgres + Redis)
- Run backend locally: `./mvnw spring-boot:run`
- Note: Requires active RPC connections (e.g. Alchemy/Infura) configured in `application.yml` for real-time indexing.

## Important Reference Documentation
You should consult these files for deeper domain information before making architectural changes:
- `docs/technical_architecture.md` — Complete system design and data flow.
- `docs/domain_knowledge.md` — Crypto concepts, DEX mechanics, and signal logic.
- `docs/database_schema.md` — Current tabular layouts.
- `docs/roadmap.md` — Current project phase and future planned features.
