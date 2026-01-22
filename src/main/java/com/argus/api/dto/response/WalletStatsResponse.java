package com.argus.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import com.argus.domain.model.WalletStatsSummary;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatsResponse {
    private String walletAddress;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private Integer totalTrades;
    private Integer profitableTrades;
    private BigDecimal avgRoiPercent;
    private List<TokenStatsDto> tokenStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenStatsDto {
        private String tokenAddress;
        private String tokenSymbol;
        private BigDecimal totalBought;
        private BigDecimal totalSold;
        private BigDecimal realizedPnl;
        private BigDecimal roiPercent;
        private Boolean isProfitable;
    }

    public static WalletStatsResponse toResponse(WalletStatsSummary summary) {
        return WalletStatsResponse.builder()
                .walletAddress(summary.getWalletAddress())
                .totalPnl(summary.getTotalPnl())
                .winRate(summary.getWinRate())
                .totalTrades(summary.getTotalTrades())
                .profitableTrades(summary.getProfitableTrades())
                .avgRoiPercent(summary.getAvgRoiPercent())
                .tokenStats(summary.getTokenStats().stream()
                        .map(s -> WalletStatsResponse.TokenStatsDto.builder()
                                .tokenAddress(s.getTokenAddress())
                                .tokenSymbol(s.getTokenSymbol())
                                .totalBought(s.getTotalBought())
                                .totalSold(s.getTotalSold())
                                .realizedPnl(s.getRealizedPnl())
                                .roiPercent(s.getRoiPercent())
                                .isProfitable(s.getIsProfitable())
                                .build())
                        .toList())
                .build();
    }
}