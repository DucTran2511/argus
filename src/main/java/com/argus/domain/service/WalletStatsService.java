package com.argus.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.WalletStats;
import com.argus.domain.model.WalletStatsSummary;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletStatsPersistencePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WalletStatsService {
    private final TransactionPersistencePort transactionPersistencePort;
    private final WalletStatsPersistencePort walletStatsPort;

    public WalletStatsSummary calculateStats(String walletAddress) {
        log.info("Calculating stats for wallet {}", walletAddress);

        List<AssetTransfer> transfers = transactionPersistencePort.findByWalletAddress(
                walletAddress.toLowerCase(), 10000, "asc");

        List<AssetTransfer> pricedTransfers = transfers.stream()
                .filter(t -> t.getUsdValue() != null && t.getTokenAddress() != null)
                .toList();

        if (pricedTransfers.isEmpty()) {
            log.warn("No priced transfers found for wallet: {}", walletAddress);
            return WalletStatsSummary.builder()
                    .walletAddress(walletAddress)
                    .totalPnl(BigDecimal.ZERO)
                    .winRate(BigDecimal.ZERO)
                    .totalTrades(0)
                    .profitableTrades(0)
                    .avgRoiPercent(BigDecimal.ZERO)
                    .tokenStats(List.of())
                    .build();
        }

        Map<String, List<AssetTransfer>> byToken = pricedTransfers.stream()
                .collect(Collectors.groupingBy(AssetTransfer::getTokenAddress));
        List<WalletStats> tokenStats = new ArrayList<>();
        for (Map.Entry<String, List<AssetTransfer>> entry : byToken.entrySet()) {
            String tokenAddr = entry.getKey();
            List<AssetTransfer> tokenTransfers = entry.getValue();

            WalletStats stats = calculateTokenStats(walletAddress, tokenAddr, tokenTransfers);
            if (stats != null) {
                tokenStats.add(stats);
            }
        }

        walletStatsPort.saveAll(tokenStats);
        return buildSummary(walletAddress, tokenStats);
    }

    public WalletStatsSummary getStats(String walletAddress) {
        List<WalletStats> tokenStats = walletStatsPort.findByWalletAddress(walletAddress);
        return buildSummary(walletAddress, tokenStats);
    }

    private WalletStats calculateTokenStats(String walletAddress, String tokenAddress,
            List<AssetTransfer> transfers) {
        BigDecimal totalBought = BigDecimal.ZERO;
        BigDecimal totalSold = BigDecimal.ZERO;
        BigDecimal costBasisUsd = BigDecimal.ZERO;
        BigDecimal proceedsUsd = BigDecimal.ZERO;
        String tokenSymbol = null;
        LocalDateTime firstTx = null;
        LocalDateTime lastTx = null;

        for (AssetTransfer tx : transfers) {
            if (tokenSymbol == null) {
                tokenSymbol = tx.getAssetSymbol();
            }
            if (firstTx == null || tx.getTxTimestamp().isBefore(firstTx)) {
                firstTx = tx.getTxTimestamp();
            }
            if (lastTx == null || tx.getTxTimestamp().isAfter(lastTx)) {
                lastTx = tx.getTxTimestamp();
            }

            boolean isBuy = walletAddress.equalsIgnoreCase(tx.getTo());
            boolean isSell = walletAddress.equalsIgnoreCase(tx.getFrom());

            if (isBuy) {
                totalBought = totalBought.add(tx.getValue());
                costBasisUsd = costBasisUsd.add(tx.getUsdValue());
            } else if (isSell) {
                totalSold = totalSold.add(tx.getValue());
                proceedsUsd = proceedsUsd.add(tx.getUsdValue());
            }
        }

        if (totalBought.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal avgBuyPrice = costBasisUsd.divide(totalBought, 8, RoundingMode.HALF_UP);

        BigDecimal avgSellPrice = totalSold.compareTo(BigDecimal.ZERO) > 0
                ? proceedsUsd.divide(totalSold, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal costOfSold = avgBuyPrice.multiply(totalSold);

        BigDecimal realizedPnl = proceedsUsd.subtract(costOfSold);
        BigDecimal roiPercent = costOfSold.compareTo(BigDecimal.ZERO) > 0
                ? realizedPnl.divide(costOfSold, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        boolean isProfitable = realizedPnl.compareTo(BigDecimal.ZERO) > 0;

        return WalletStats.builder()
                .walletAddress(walletAddress.toLowerCase())
                .tokenAddress(tokenAddress.toLowerCase())
                .tokenSymbol(tokenSymbol)
                .totalBought(totalBought)
                .totalSold(totalSold)
                .costBasisUsd(costBasisUsd)
                .proceedsUsd(proceedsUsd)
                .realizedPnl(realizedPnl)
                .avgBuyPrice(avgBuyPrice)
                .avgSellPrice(avgSellPrice)
                .roiPercent(roiPercent)
                .isProfitable(isProfitable)
                .firstTxAt(firstTx)
                .lastTxAt(lastTx)
                .build();
    }

    private WalletStatsSummary buildSummary(String walletAddress, List<WalletStats> tokenStats) {
        BigDecimal totalPnl = tokenStats.stream()
                .map(WalletStats::getRealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTrades = tokenStats.size();

        int profitableTrades = (int) tokenStats.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsProfitable()))
                .count();
        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(profitableTrades)
                        .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        List<WalletStats> closedPositions = tokenStats.stream()
                .filter(s -> s.getTotalSold().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal avgRoi = closedPositions.isEmpty() ? BigDecimal.ZERO
                : closedPositions.stream()
                        .map(WalletStats::getRoiPercent)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(closedPositions.size()), 2, RoundingMode.HALF_UP);

        return WalletStatsSummary.builder()
                .walletAddress(walletAddress)
                .totalPnl(totalPnl)
                .winRate(winRate)
                .totalTrades(totalTrades)
                .profitableTrades(profitableTrades)
                .avgRoiPercent(avgRoi)
                .tokenStats(tokenStats)
                .build();
    }
}