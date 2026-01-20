package com.argus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletStats {
    private Long id;
    private String walletAddress;
    private String tokenAddress;
    private String tokenSymbol;

    private BigDecimal totalBought;
    private BigDecimal totalSold;
    private BigDecimal costBasisUsd;
    private BigDecimal proceedsUsd;

    private BigDecimal realizedPnl;
    private BigDecimal avgBuyPrice;
    private BigDecimal avgSellPrice;
    private BigDecimal roiPercent;
    private Boolean isProfitable;

    private LocalDateTime firstTxAt;
    private LocalDateTime lastTxAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}