package com.argus.infra.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_stats", indexes = {
        @Index(name = "idx_wallet_stats_wallet", columnList = "wallet_address")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;
    @Column(name = "token_address", nullable = false, length = 42)
    private String tokenAddress;
    @Column(name = "token_symbol", length = 20)
    private String tokenSymbol;
    @Column(name = "total_bought", precision = 38, scale = 18)
    private BigDecimal totalBought;
    @Column(name = "total_sold", precision = 38, scale = 18)
    private BigDecimal totalSold;
    @Column(name = "cost_basis_usd", precision = 20, scale = 2)
    private BigDecimal costBasisUsd;
    @Column(name = "proceeds_usd", precision = 20, scale = 2)
    private BigDecimal proceedsUsd;
    @Column(name = "realized_pnl", precision = 20, scale = 2)
    private BigDecimal realizedPnl;
    @Column(name = "avg_buy_price", precision = 20, scale = 8)
    private BigDecimal avgBuyPrice;
    @Column(name = "avg_sell_price", precision = 20, scale = 8)
    private BigDecimal avgSellPrice;
    @Column(name = "roi_percent", precision = 10, scale = 4)
    private BigDecimal roiPercent;
    @Column(name = "is_profitable")
    private Boolean isProfitable;
    @Column(name = "first_tx_at")
    private LocalDateTime firstTxAt;
    @Column(name = "last_tx_at")
    private LocalDateTime lastTxAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}