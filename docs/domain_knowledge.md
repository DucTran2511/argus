# On-Chain Alpha Hunter - Domain Knowledge Guide

A practical guide for developers new to blockchain/crypto who want to build on-chain monitoring tools.

---

## 1. Blockchain Fundamentals

### What is a Blockchain?

A blockchain is a public database where every transaction is recorded and visible to everyone. Think of it as a **public spreadsheet** that anyone can read but follows strict rules for writing.

```
Traditional Database          vs          Blockchain
─────────────────────                     ──────────
Private (company owns)                    Public (everyone sees)
Can be edited                            Cannot be changed
Trust the company                        Trust the math
```

### Key Properties for Our Tool

| Property | What It Means | Why It Matters |
|----------|---------------|----------------|
| **Public** | All transactions visible | We can see whale activity |
| **Immutable** | Cannot be changed | Reliable historical data |
| **Real-time** | New blocks every ~12s | Near-instant signals |
| **Pseudonymous** | Addresses, not names | We label wallets ourselves |

### Ethereum vs Other Chains

| Chain | Block Time | Focus | Our Support |
|-------|------------|-------|-------------|
| **Ethereum** | ~12 sec | DeFi, NFTs, most liquidity | MVP (start here) |
| **Base** | ~2 sec | Cheap, growing fast | Phase 2 |
| **Solana** | ~0.4 sec | Memecoins, speed | Phase 2 |
| **Arbitrum** | ~0.25 sec | Ethereum L2, DeFi | Phase 3 |

---

## 2. Anatomy of a Transaction

Every blockchain transaction has these parts:

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRANSACTION STRUCTURE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Transaction Hash: 0x123abc...def789                            │
│  ├── Unique identifier for this transaction                    │
│  └── Use this to look up on Etherscan                          │
│                                                                 │
│  From: 0xSender...Address                                       │
│  ├── Who initiated the transaction                             │
│  └── This is the wallet we track                               │
│                                                                 │
│  To: 0xReceiver...Address                                       │
│  ├── Where the transaction goes                                │
│  └── Often a smart contract (DEX router)                       │
│                                                                 │
│  Value: 1.5 ETH                                                 │
│  └── Native currency sent (if any)                             │
│                                                                 │
│  Data: 0x38ed1739...                                            │
│  ├── Encoded function call                                     │
│  └── Contains: function name + parameters                      │
│                                                                 │
│  Gas Used: 150,000                                              │
│  Gas Price: 20 gwei                                             │
│  └── Transaction cost = Gas Used × Gas Price                   │
│                                                                 │
│  Block: 18,500,000                                              │
│  └── Which block included this TX                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Transaction Types We Care About

| Type | How to Identify | Signal |
|------|-----------------|--------|
| **Token Swap** | Calls DEX router (Uniswap, etc.) | Buy/Sell signal |
| **Token Transfer** | Simple ERC20 transfer | Movement, possible sell prep |
| **LP Add** | addLiquidity function | Providing liquidity |
| **LP Remove** | removeLiquidity function | Exiting liquidity |
| **ETH Transfer** | Value > 0, no data | Funding or moving funds |
| **Contract Deploy** | To address is empty | New token creation |

---

## 3. How DEXes Work

### What is a DEX?

A DEX (Decentralized Exchange) is a smart contract that lets people swap tokens without a middleman.

**Uniswap Example**:
```
┌─────────────────────────────────────────────────────────────────┐
│                      UNISWAP SWAP                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User wants: Swap 1 ETH for PEPE tokens                         │
│                                                                 │
│       ┌─────────┐          ┌─────────────────┐                  │
│       │  User   │ ──1 ETH──▶│  Liquidity Pool │                  │
│       │  Wallet │ ◀─PEPE───│  (ETH + PEPE)   │                  │
│       └─────────┘          └─────────────────┘                  │
│                                                                 │
│  How price is determined:                                       │
│  x × y = k (constant product formula)                           │
│                                                                 │
│  Pool before: 100 ETH × 1,000,000 PEPE = 100,000,000           │
│  User adds 1 ETH, pool must maintain k                          │
│  Pool after: 101 ETH × 990,099 PEPE = 100,000,000              │
│  User receives: 9,901 PEPE                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Key DEX Concepts

| Concept | Explanation | Why It Matters |
|---------|-------------|----------------|
| **Liquidity Pool** | Reserve of two tokens | More liquidity = easier to trade |
| **Price Impact** | How much trade moves price | Large trades = bigger impact |
| **Slippage** | Difference from expected price | High slippage = less profit |
| **LP Tokens** | Proof of providing liquidity | Track liquidity providers |

### DEX Router Addresses (Ethereum)

```java
// Common DEX routers we need to monitor
public static final String UNISWAP_V2_ROUTER = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D";
public static final String UNISWAP_V3_ROUTER = "0xE592427A0AEce92De3Edee1F18E0157C05861564";
public static final String SUSHISWAP_ROUTER = "0xd9e1cE17f2641f24aE83637ab66a2cca9C378B9F";
public static final String ONEINCH_ROUTER = "0x1111111254EEB25477B68fb85Ed929f73A960582";
```

---

## 4. Understanding Wallet Behavior

### Wallet Categories

| Category | Characteristics | Detection Method |
|----------|-----------------|------------------|
| **Whale** | >$1M assets, large trades | Balance check |
| **VC/Fund** | Known addresses, long holds | Label database |
| **Smart Money** | Consistent profits, good timing | PnL calculation |
| **MEV Bot** | Sandwich attacks, fast TXs | Pattern detection |
| **Fresh Wallet** | New, receives from known | Age + connection |
| **Insider** | Early buys, connected to devs | Graph analysis |

### Wallet Analysis Metrics

```
┌─────────────────────────────────────────────────────────────────┐
│                    WALLET SCORECARD                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Address: 0x1234...abcd                                         │
│                                                                 │
│  PERFORMANCE METRICS                                            │
│  ├── Total PnL: +$1,250,000                                    │
│  ├── Win Rate: 72% (36/50 profitable trades)                   │
│  ├── Average ROI: +45% per trade                               │
│  └── Best Trade: +2,400% on $PEPE                              │
│                                                                 │
│  BEHAVIOR METRICS                                               │
│  ├── Average Hold Time: 14 days                                │
│  ├── Trade Frequency: 3.2 trades/week                          │
│  ├── Preferred DEX: Uniswap (85%)                              │
│  └── Active Hours: 14:00-22:00 UTC                             │
│                                                                 │
│  RISK INDICATORS                                                │
│  ├── Insider Connection: None detected                         │
│  ├── Rug Involvement: 0                                        │
│  └── MEV Activity: None                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Calculating Wallet PnL

```java
// Simplified PnL calculation for a token position
public class WalletPnL {
    
    public BigDecimal calculateRealizedPnL(List<Trade> trades) {
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalProceeds = BigDecimal.ZERO;
        BigDecimal tokenBalance = BigDecimal.ZERO;
        
        for (Trade trade : trades) {
            if (trade.isBuy()) {
                // Add to cost basis
                totalCostBasis = totalCostBasis.add(trade.getUsdValue());
                tokenBalance = tokenBalance.add(trade.getTokenAmount());
            } else {
                // Calculate sale proceeds
                totalProceeds = totalProceeds.add(trade.getUsdValue());
                tokenBalance = tokenBalance.subtract(trade.getTokenAmount());
            }
        }
        
        return totalProceeds.subtract(totalCostBasis);
    }
}
```

---

## 5. Data Sources & APIs

### RPC Providers

RPC (Remote Procedure Call) providers give you access to blockchain data.

| Provider | Free Tier | Rate Limit | Best For |
|----------|-----------|------------|----------|
| **Alchemy** | 300M compute units/mo | 330 req/sec | Production |
| **Infura** | 100k req/day | 10 req/sec | Starting out |
| **QuickNode** | 10M credits/mo | varies | WebSocket |
| **Ankr** | Limited free | 30 req/sec | Budget option |

### Essential RPC Methods

```java
// Methods we'll use frequently
public interface EthereumRPC {
    
    // Get latest block number
    BigInteger eth_blockNumber();
    
    // Get block with transactions
    Block eth_getBlockByNumber(BigInteger blockNum, boolean fullTx);
    
    // Get transaction details
    Transaction eth_getTransactionByHash(String txHash);
    
    // Get transaction receipt (logs, status)
    TransactionReceipt eth_getTransactionReceipt(String txHash);
    
    // Get token balance
    BigInteger eth_call(CallObject call, String blockNumber);
    
    // Subscribe to new blocks (WebSocket)
    Subscription eth_subscribe("newHeads");
    
    // Subscribe to logs (WebSocket)
    Subscription eth_subscribe("logs", FilterObject filter);
}
```

### Price & Market Data APIs

| API | Data | Free Tier |
|-----|------|-----------|
| **CoinGecko** | Prices, market cap | 10-30 req/min |
| **DexScreener** | DEX prices, pairs | 300 req/min |
| **GeckoTerminal** | Real-time DEX data | Generous |
| **Moralis** | Wallet, token data | 25k req/mo |

### The Graph (Subgraphs)

Pre-indexed blockchain data - much faster than raw RPC.

```graphql
# Example: Get recent swaps on Uniswap V3
query RecentSwaps($first: Int!, $minUSD: BigDecimal!) {
  swaps(
    first: $first
    orderBy: timestamp
    orderDirection: desc
    where: { amountUSD_gte: $minUSD }
  ) {
    id
    timestamp
    sender
    recipient
    amount0
    amount1
    amountUSD
    token0 { symbol }
    token1 { symbol }
  }
}
```

---

## 6. Detection Patterns

### Pattern 1: Whale Accumulation

```
DEFINITION: Same wallet buying a token 3+ times over multiple hours/days
           without selling, with increasing position size.

DETECTION LOGIC:
1. Track all buys for each wallet-token pair
2. If buys >= 3 in 72 hours with no sells
3. If total position > $50k
4. Generate ACCUMULATION signal

EXAMPLE:
Day 1: Whale buys $20k of TOKEN
Day 2: Whale buys $30k of TOKEN  
Day 3: Whale buys $50k of TOKEN
→ ACCUMULATION DETECTED ($100k total)
```

### Pattern 2: Multi-Whale Convergence

```
DEFINITION: 3+ unrelated whale wallets buying same token within 24 hours.

DETECTION LOGIC:
1. For each token, track unique whale buyers
2. If 3+ different whales buy within 24h window
3. Verify wallets are not connected (cluster check)
4. Generate MULTI_WHALE signal

WHY IT MATTERS: 
Multiple smart money actors independently discovering same opportunity
suggests strong conviction.
```

### Pattern 3: Insider Detection

```
DEFINITION: Wallet receives tokens before public announcement or 
           immediately after contract deployment.

DETECTION LOGIC:
1. Track new token deployments
2. Monitor first N buyers (within 10 blocks)
3. Check wallet history:
   - New wallet? (< 10 txs)
   - Connected to deployer?
   - Received tokens directly from deployer?
4. Flag as potential INSIDER

RED FLAGS:
- Wallet created same day as token
- Direct transfer from deployer
- No prior trading history
```

### Pattern 4: Exit Signals

```
DEFINITION: Smart money starting to reduce positions.

DETECTION LOGIC:
1. Track wallets with > 60% historical win rate
2. Monitor for sells after profitable hold
3. Calculate: % of position sold, timing vs price peak
4. Generate EXIT_SIGNAL if major holder reducing

GRADATIONS:
- Partial exit (< 30%): Profit taking, may re-enter
- Major exit (30-70%): Reducing conviction
- Full exit (> 70%): Lost conviction, bearish
```

---

## 7. Technical Implementation Notes

### Web3j Usage (Java)

```java
// Connect to Ethereum
Web3j web3 = Web3j.build(new HttpService("https://eth-mainnet.alchemyapi.io/v2/YOUR_KEY"));

// Get latest block
EthBlockNumber blockNumber = web3.ethBlockNumber().send();

// Subscribe to new transactions (WebSocket required)
web3.transactionFlowable().subscribe(tx -> {
    // Process each transaction
    String from = tx.getFrom();
    String to = tx.getTo();
    BigInteger value = tx.getValue();
    
    // Check if this is a tracked wallet
    if (trackedWallets.contains(from)) {
        processWhaleTransaction(tx);
    }
});

// Decode swap data
Function function = new Function(
    "swapExactTokensForTokens",
    Arrays.asList(), // inputs (empty for decoding)
    Arrays.asList(
        new TypeReference<Uint256>() {},  // amountIn
        new TypeReference<Uint256>() {},  // amountOutMin
        new TypeReference<DynamicArray<Address>>() {}, // path
        new TypeReference<Address>() {},  // to
        new TypeReference<Uint256>() {}   // deadline
    )
);
List<Type> decoded = FunctionReturnDecoder.decode(tx.getInput(), function.getOutputParameters());
```

### Event Log Monitoring

```java
// Monitor ERC20 Transfer events
EthFilter transferFilter = new EthFilter(
    DefaultBlockParameterName.LATEST,
    DefaultBlockParameterName.LATEST,
    tokenAddress
);
transferFilter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));

web3.ethLogFlowable(transferFilter).subscribe(log -> {
    Address from = (Address) FunctionReturnDecoder.decodeIndexedValue(
        log.getTopics().get(1), new TypeReference<Address>() {});
    Address to = (Address) FunctionReturnDecoder.decodeIndexedValue(
        log.getTopics().get(2), new TypeReference<Address>() {});
    Uint256 value = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
        log.getData(), new TypeReference<Uint256>() {});
    
    // Process transfer
});
```

---

## 8. Common Pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| **Rate Limits** | Get blocked by RPC provider | Implement backoff, use multiple providers |
| **Reorgs** | Block reverted, TX disappeared | Wait for confirmations, handle reorg events |
| **False Positives** | Not all whale buys are good | Track outcomes, improve filters |
| **Gas Spikes** | Miss transactions during congestion | Use WebSocket, not polling |
| **Stale Prices** | Incorrect USD value calculation | Use DEX price at TX block, not current |
| **Wallet Clustering** | Same person, multiple wallets | Implement cluster detection |

---

## Quick Reference Cheat Sheet

```
ADDRESSES:
ETH (native):     0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE
WETH:             0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2
USDC:             0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48
USDT:             0xdAC17F958D2ee523a2206206994597C13D831ec7

COMMON FUNCTION SELECTORS:
0x38ed1739 - swapExactTokensForTokens (Uniswap V2)
0x18cbafe5 - swapExactTokensForETH (Uniswap V2)
0xa9059cbb - transfer (ERC20)
0x23b872dd - transferFrom (ERC20)

EXPLORERS:
Ethereum: https://etherscan.io/tx/{hash}
Base:     https://basescan.org/tx/{hash}
Arbitrum: https://arbiscan.io/tx/{hash}

1 ETH = 10^18 wei
1 gwei = 10^9 wei
```
