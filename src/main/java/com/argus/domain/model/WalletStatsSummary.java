package com.argus.domain.model;

import java.math.BigDecimal;
import java.util.List;

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
public class WalletStatsSummary {
    private String walletAddress;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private Integer totalTrades;
    private Integer profitableTrades;
    private BigDecimal avgRoiPercent;
    private List<WalletStats> tokenStats;
}