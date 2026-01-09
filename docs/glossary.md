# On-Chain Alpha Hunter - Glossary

Quick reference for all crypto and blockchain terms used in this project.

---

## Blockchain Basics

| Term | Definition |
|------|------------|
| **Blockchain** | Public, distributed ledger that records all transactions |
| **Block** | Batch of transactions; Ethereum creates one every ~12 seconds |
| **Transaction (TX)** | Single action recorded on blockchain (transfer, swap, etc.) |
| **TX Hash** | Unique identifier for a transaction (e.g., `0x123...abc`) |
| **Gas** | Fee paid to execute transactions, priced in gwei |
| **Gwei** | Unit of ETH (1 gwei = 0.000000001 ETH) |
| **Smart Contract** | Code that runs on blockchain; tokens and DEXes are contracts |
| **EVM** | Ethereum Virtual Machine; runs smart contracts |
| **L1** | Layer 1 blockchain (Ethereum, Solana) |
| **L2** | Layer 2; cheaper chains built on L1 (Base, Arbitrum) |
| **Confirmation** | Number of blocks since TX was included |
| **Reorg** | Block reorganization; recent blocks can change |

---

## Wallet & Address Terms

| Term | Definition |
|------|------------|
| **Wallet** | Blockchain address that holds assets (e.g., `0x1234...abcd`) |
| **EOA** | Externally Owned Account; regular user wallet |
| **Contract Address** | Address of a deployed smart contract |
| **Hot Wallet** | Connected to internet; used for trading |
| **Cold Wallet** | Offline storage; for holding |
| **Fresh Wallet** | Newly created with few transactions |
| **Whale** | Wallet with large holdings (>$1M) |
| **Smart Money** | Wallets with consistent profitable trades |

---

## Trading Terms

| Term | Definition |
|------|------------|
| **DEX** | Decentralized Exchange (Uniswap, Curve) |
| **CEX** | Centralized Exchange (Binance, Coinbase) |
| **Swap** | Exchange one token for another on DEX |
| **Slippage** | Price difference between order and execution |
| **Price Impact** | How much your trade moves the price |
| **PnL** | Profit and Loss |
| **Realized PnL** | Profit from sold positions |
| **Unrealized PnL** | Profit on current holdings (paper gains) |
| **ROI** | Return on Investment (%) |
| **Win Rate** | % of trades that were profitable |
| **HODL** | Hold long-term (meme: "Hold On for Dear Life") |
| **Ape In** | Buy quickly without much research |
| **Degen** | High-risk trader (degenerate gambler) |

---

## DeFi Terms

| Term | Definition |
|------|------------|
| **DeFi** | Decentralized Finance |
| **Liquidity Pool (LP)** | Reserve of two tokens that enables swaps |
| **LP Token** | Proof of providing liquidity |
| **AMM** | Automated Market Maker (how DEXes price tokens) |
| **TVL** | Total Value Locked (assets in a protocol) |
| **Yield** | Return from staking/providing liquidity |
| **APY** | Annual Percentage Yield |
| **Impermanent Loss** | Loss from providing LP vs just holding |
| **Farming** | Earning rewards by providing liquidity |
| **Staking** | Locking tokens to earn rewards |

---

## Token Terms

| Term | Definition |
|------|------------|
| **ERC20** | Standard for fungible tokens on Ethereum |
| **Token Address** | Contract address of the token |
| **Market Cap** | Total value of all tokens (price × supply) |
| **Circulating Supply** | Tokens currently available |
| **Total Supply** | All tokens that exist |
| **Meme Coin** | Token with no utility, driven by community/hype |
| **Liquidity** | How easily you can buy/sell |
| **Rug Pull** | Scam where dev removes liquidity |
| **Honeypot** | Token that can't be sold |
| **Airdrop** | Free tokens distributed to wallets |

---

## Analysis Terms

| Term | Definition |
|------|------------|
| **On-Chain** | Data directly from blockchain |
| **Off-Chain** | Data from external sources |
| **Alpha** | Information that gives trading advantage |
| **Accumulation** | Buying slowly over time |
| **Distribution** | Selling slowly over time |
| **Insider** | Person with non-public information |
| **Front-running** | Trading before someone else's published TX |
| **MEV** | Maximal Extractable Value; profit from TX ordering |
| **Sandwich Attack** | MEV bot buys before & sells after your trade |

---

## Technical Terms

| Term | Definition |
|------|------------|
| **RPC** | Remote Procedure Call; API to blockchain |
| **ABI** | Application Binary Interface; contract function definitions |
| **Event/Log** | Record emitted by smart contract |
| **Subgraph** | Pre-indexed blockchain data (The Graph) |
| **WebSocket** | Real-time connection for live data |
| **Mempool** | Pending transactions waiting to be included |
| **Block Explorer** | Website to view blockchain data (Etherscan) |
| **Web3j** | Java library for Ethereum |

---

## Signal Types (Our System)

| Signal | Meaning |
|--------|---------|
| **WHALE_BUY** | Large wallet purchased token (>$50k) |
| **WHALE_SELL** | Large wallet sold token (>$50k) |
| **ACCUMULATION** | Wallet buying repeatedly without selling |
| **MULTI_WHALE** | Multiple whales buying same token |
| **NEW_TOKEN** | Smart money buying token < 7 days old |
| **CEX_OUTFLOW** | Large withdrawal from exchange (bullish) |
| **CEX_INFLOW** | Large deposit to exchange (bearish) |
| **INSIDER_SUSPECT** | Possible insider activity detected |
