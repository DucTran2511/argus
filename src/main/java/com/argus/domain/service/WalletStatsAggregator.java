package com.argus.domain.service;

import com.argus.domain.model.WalletStats;
import com.argus.domain.model.WalletStatsSummary;
import com.argus.domain.port.persistence.WalletStatsPersistencePort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import java.util.Optional;

@RequiredArgsConstructor
public class WalletStatsAggregator {

    private final WalletStatsPersistencePort walletStatsPort;

    public Optional<WalletStatsSummary> aggregateStats(String walletAddress) {
        List<WalletStats> tokenStats = walletStatsPort.findByWalletAddress(walletAddress);

        BigDecimal totalPnl = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        BigDecimal totalBuyVol = BigDecimal.ZERO;
        BigDecimal totalSellVol = BigDecimal.ZERO;

        BigDecimal maxRoi = BigDecimal.ZERO;

        int profitableTrades = 0;
        BigDecimal roiSum = BigDecimal.ZERO;
        int closedPositionCount = 0;

        for (WalletStats stats : tokenStats) {
            if (stats.getTotalBought() != null) {
                totalBuyVol = totalBuyVol.add(stats.getTotalBought());
            }
            if (stats.getTotalSold() != null) {
                totalSellVol = totalSellVol.add(stats.getTotalSold());
            }

            if (stats.getRealizedPnl() != null) {
                BigDecimal pnl = stats.getRealizedPnl();
                totalPnl = totalPnl.add(pnl);
                if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                    grossProfit = grossProfit.add(pnl);
                } else {
                    grossLoss = grossLoss.add(pnl.abs());
                }
            }

            if (Boolean.TRUE.equals(stats.getIsProfitable())) {
                profitableTrades++;
            }

            if (stats.getTotalSold() != null && stats.getTotalSold().compareTo(BigDecimal.ZERO) > 0) {
                if (stats.getRoiPercent() != null) {
                    roiSum = roiSum.add(stats.getRoiPercent());

                    if (stats.getRoiPercent().compareTo(maxRoi) > 0) {
                        maxRoi = stats.getRoiPercent();
                    }
                }
                closedPositionCount++;
            }
        }

        int totalTrades = tokenStats.size();

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(profitableTrades)
                        .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal avgRoi = closedPositionCount > 0
                ? roiSum.divide(BigDecimal.valueOf(closedPositionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return Optional.of(WalletStatsSummary.builder()
                .walletAddress(walletAddress.toLowerCase())
                .totalPnl(totalPnl)
                .grossProfit(grossProfit)
                .grossLoss(grossLoss)
                .winRate(winRate)
                .totalTrades(totalTrades)
                .profitableTrades(profitableTrades)
                .avgRoiPercent(avgRoi)
                .maxRoiPercent(maxRoi)
                .totalBuyVolume(totalBuyVol)
                .totalSellVolume(totalSellVol)
                .tokenStats(tokenStats)
                .build());
    }
}
