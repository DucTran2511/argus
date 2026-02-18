# Argus - Database Schema

> **Last Updated**: 2026-02-18

## Visual Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ARGUS DATABASE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────┐
                    │   users     │
                    │ ─────────── │
                    │ id (PK)     │
                    │ email       │
                    │ telegram_id │
                    └──────┬──────┘
                           │ 1:N
                           ▼
                    ┌─────────────┐
                    │ alert_rules │
                    └─────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           BLOCKCHAIN TRACKING                                │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌──────────────────┐
                              │     wallets      │
                              │ ──────────────── │
                              │ id (PK)          │
                              │ address (UK)     │
                              │ type, label      │
                              │ total_pnl        │
                              └────────┬─────────┘
                                       │
              ┌────────────────────────┼────────────────────────┬────────────────────┐
              │                        │                        │                    │
              ▼ 1:N                    ▼ 1:N                    ▼ 1:N                ▼ 1:N
    ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐   ┌─────────────────┐
    │  transactions   │      │ asset_transfers │      │    signals      │   │  wallet_stats   │
    │ ─────────────── │      │ ─────────────── │      │ ─────────────── │   │ ─────────────── │
    │ tx_hash         │      │ tx_hash         │      │ type            │   │ wallet_address  │
    │ from, to        │      │ wallet_address  │      │ token_address   │   │ token_address   │
    │ value, input    │      │ from, to        │      │ confidence      │   │ realized_pnl    │
    │ usd_value       │      │ value           │      │ ai_narrative    │   │ roi_percent     │
    │                 │      │ usd_value       │      │                 │   │ is_profitable   │
    │                 │      │ asset_symbol    │      │                 │   │                 │
    └─────────────────┘      └─────────────────┘      └─────────────────┘   └────────┬────────┘
         Raw TXs              Alchemy Transfers         Trading Signals        PnL Analytics
                                                                                      │
                                                                                      │ aggregates
                                                                                      ▼
                                                                            ┌─────────────────┐
                                                                            │ wallet_metrics  │
                                                                            │ ─────────────── │
                                                                            │ wallet_address  │
                                                                            │ archetype       │
                                                                            │ is_blacklisted  │
                                                                            │ pnl_score       │
                                                                            │ consistency     │
                                                                            │ conviction      │
                                                                            └─────────────────┘
                                                                             Smart Money Scores

┌─────────────────────────────────────────────────────────────────────────────┐
│                           ADDRESS BOOK (LABELS)                              │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────┐                     ┌──────────────────┐
    │ address_labels  │                     │     tokens       │
    │ ─────────────── │                     │ ──────────────── │
    │ address         │                     │ address (PK)     │
    │ label           │                     │ symbol, name     │
    │ category        │                     │ liquidity        │
    │ source          │                     │ risk_score       │
    └─────────────────┘                     └──────────────────┘
       Any Address                             Token Metadata
```

---

## Tables

| Table | Purpose | Migration |
|-------|---------|-----------|
| `wallets` | Tracked blockchain addresses | V1 |
| `tokens` | Token metadata (symbol, liquidity) | V2 |
| `transactions` | Raw blockchain transactions | V3 |
| `signals` | Detected trading signals | V4 |
| `users` | Application users | V5 |
| `alert_rules` | User notification rules | - |
| `asset_transfers` | Normalized wallet transfers (Alchemy) | V6, V10 |
| `wallet_stats` | Per-token PnL analytics (Average Cost Method) | V13 |
| `address_labels` | Address Book - labels for any address (max 5) | V14 |
| `wallet_metrics` | Smart Money scoring & archetype classification | V15, V16 |

---

## Relationships

| From | To | Type |
|------|----|------|
| `wallets` | `transactions` | 1:N |
| `wallets` | `asset_transfers` | 1:N (via wallet_address) |
| `wallets` | `signals` | 1:N |
| `wallets` | `wallet_stats` | 1:N (via wallet_address) |
| `wallet_stats` | `wallet_metrics` | N:1 (aggregated per wallet) |
| `users` | `alert_rules` | 1:N |

---

## Indexes

| Table | Index | Columns |
|-------|-------|---------|
| wallets | idx_wallets_address | address |
| wallets | idx_wallets_type | type |
| transactions | idx_transactions_wallet | wallet_id |
| transactions | idx_transactions_timestamp | tx_timestamp |
| signals | idx_signals_type | type |
| signals | idx_signals_created | created_at |
| asset_transfers | idx_wallet_address | wallet_address |
| asset_transfers | idx_tx_timestamp | tx_timestamp |
| asset_transfers | idx_tx_hash | tx_hash |
| wallet_metrics | idx_wm_archetype | archetype |
| wallet_metrics | idx_wm_pnl_score | pnl_score DESC |
| wallet_metrics | idx_wm_consistency | consistency_score DESC |
| wallet_metrics | idx_wm_conviction | conviction_score DESC |
| wallet_metrics | idx_wm_tier | tier |
| wallet_metrics | idx_wm_total_score | total_score DESC |
| signals | idx_signals_token_created | token_address, created_at DESC |

---

## Constraints

| Table | Constraint | Columns | Purpose |
|-------|------------|---------|---------|
| asset_transfers | uq_asset_transfer | tx_hash, wallet_address, category, log_index | Prevent duplicate transfers |
| wallet_metrics | pk_wallet_metrics | wallet_address | One metrics record per wallet |

---

## V16 Migration: total_score & tier

Added composite scoring and tier classification:
- `total_score`: Weighted combination of pnl_score (50%), consistency_score (30%), conviction_score (20%)
- `tier`: Letter grade (S/A/B/C) based on total_score thresholds

## V17 Migration: Signal Convergence Index

Added composite index for efficient convergence detection:
- `idx_signals_token_created(token_address, created_at DESC)` — Supports `SmartMoneySignalEnricher` lookups to find which archetypes bought the same token within a 12-hour window.
