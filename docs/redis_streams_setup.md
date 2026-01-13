# Redis Streams Setup Guide

> **Last Updated**: 2026-01-11 | **Status**: Day 15 Implementation

This document describes the Redis Streams event-driven architecture in Argus.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRODUCERS                                │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │ BlockMonitorJob │    │  WalletSync     │                     │
│  └────────┬────────┘    └────────┬────────┘                     │
│           │                      │                               │
│           └──────────┬───────────┘                               │
│                      ▼                                           │
│           RedisStreamPublisher                                   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     REDIS STREAMS                                │
│  ┌─────────────────────────┐  ┌─────────────────────────┐       │
│  │  argus:transactions     │  │  argus:signals          │       │
│  │  (new txs detected)     │  │  (trading signals)      │       │
│  └────────────┬────────────┘  └─────────────────────────┘       │
│               │                                                  │
│    ┌──────────┴──────────┐                                      │
│    ▼                     ▼                                      │
│  tx-processor-group   signal-detector-group                     │
└─────┬────────────────────┬──────────────────────────────────────┘
      │                    │
      ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        CONSUMERS                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐       │
│  │TransactionStreamConsumer│  │ SignalStreamConsumer    │       │
│  │  - Save to DB           │  │  - Send Telegram alert  │       │
│  │  - Enrich data          │  │  - Log signal           │       │
│  └─────────────────────────┘  └─────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Stream Configuration

### Stream Keys (`StreamKeys.java`)

| Constant | Value | Purpose |
|----------|-------|---------|
| `TRANSACTION_STREAM` | `argus:transactions` | New blockchain transactions |
| `SIGNAL_STREAM` | `argus:signals` | Detected trading signals |
| `TX_PROCESSOR_GROUP` | `argus:tx-processor-group` | Consumer group for tx processing |
| `SIGNAL_DETECTOR_GROUP` | `argus:signal-detector-group` | Consumer group for signal detection |

---

## Key Components

### 1. RedisStreamPublisher

Publishes events to Redis Streams.

```java
// Publish raw fields
publisher.publish("argus:transactions", Map.of(
    "txHash", "0x123...",
    "from", "0xabc...",
    "to", "0xdef..."
));

// Publish typed event (auto-serialized)
TransactionEvent event = TransactionEvent.builder()
    .txHash("0x123...")
    .from("0xabc...")
    .to("0xdef...")
    .value(new BigDecimal("1.5"))
    .build();
publisher.publishEvent(event);
```

### 2. TransactionStreamConsumer

Consumes events from `argus:transactions` stream.

- Creates consumer group on startup
- Processes each message
- Acknowledges after successful processing
- Failed messages remain pending for retry

### 3. Event DTOs

**TransactionEvent:**
```java
txHash, from, to, value, blockNumber, timestamp, category
```

**SignalEvent:**
```java
signalType, walletAddress, signalData, tokenAddress, tokenSymbol, amount, usdValue, detectedAt
```

---

## Redis CLI Commands

### Monitor Streams

```bash
# Check if stream exists
redis-cli EXISTS argus:transactions

# Get stream info
redis-cli XINFO STREAM argus:transactions

# List consumer groups
redis-cli XINFO GROUPS argus:transactions

# Check pending messages (unacknowledged)
redis-cli XPENDING argus:transactions tx-processor-group
```

### Read Messages

```bash
# Read all messages from beginning
redis-cli XREAD STREAMS argus:transactions 0

# Read last 10 messages
redis-cli XREVRANGE argus:transactions + - COUNT 10

# Read new messages (blocking)
redis-cli XREAD BLOCK 5000 STREAMS argus:transactions $
```

### Publish Test Message

```bash
redis-cli XADD argus:transactions '*' \
    txHash 0x123abc \
    from 0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045 \
    to 0x28C6c06298d514Db089934071355E5743bf21d60 \
    value 1.5 \
    category EXTERNAL
```

### Manage Consumer Groups

```bash
# Create consumer group (from beginning)
redis-cli XGROUP CREATE argus:transactions my-group 0 MKSTREAM

# Create consumer group (from latest)
redis-cli XGROUP CREATE argus:transactions my-group $ MKSTREAM

# Delete consumer group
redis-cli XGROUP DESTROY argus:transactions my-group

# Delete specific consumer
redis-cli XGROUP DELCONSUMER argus:transactions my-group consumer-1
```

### Acknowledge Messages

```bash
# ACK specific message
redis-cli XACK argus:transactions tx-processor-group 1234567890123-0

# Claim pending message (for reprocessing)
redis-cli XCLAIM argus:transactions tx-processor-group consumer-2 3600000 1234567890123-0
```

---

## Configuration

### application.properties

```properties
# Redis connection
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Stream polling timeout (optional)
# Default: 1 second
```

### RedisStreamsConfig.java

Key beans:
- `StreamMessageListenerContainer` - manages stream readers
- `Subscription` - registers consumers to streams

---

## Scaling

### Horizontal Scaling with Consumer Groups

Each consumer in a group receives **different messages** (load balanced):

```
Stream: argus:transactions
         │
    ┌────┴────┐
    ▼         ▼
Consumer-1   Consumer-2   ← Different messages, no duplicates
```

To scale:
1. Run multiple app instances
2. Each instance auto-registers with unique consumer ID (UUID)
3. Messages distributed across instances

---

## Troubleshooting

### Messages Not Being Consumed

1. Check consumer group exists:
   ```bash
   redis-cli XINFO GROUPS argus:transactions
   ```

2. Check pending messages:
   ```bash
   redis-cli XPENDING argus:transactions tx-processor-group
   ```

3. Check app logs for consumer registration

### Message Processing Failed

1. Message stays in pending list
2. Will be redelivered on next read
3. Check app logs for processing errors

### Stream Too Large

```bash
# Trim stream to last 10000 messages
redis-cli XTRIM argus:transactions MAXLEN ~ 10000

# Trim messages older than 7 days (approximate)
redis-cli XTRIM argus:transactions MINID ~ <timestamp-7-days-ago>
```

---

## File Reference

| File | Purpose |
|------|---------|
| `config/RedisStreamsConfig.java` | Spring configuration |
| `core/constant/StreamKeys.java` | Stream key constants |
| `infra/stream/StreamPublisher.java` | Publisher interface |
| `infra/stream/RedisStreamPublisher.java` | Redis implementation |
| `infra/stream/consumer/TransactionStreamConsumer.java` | Transaction consumer |
| `infra/stream/dto/TransactionEvent.java` | Transaction payload |
| `infra/stream/dto/SignalEvent.java` | Signal payload |
