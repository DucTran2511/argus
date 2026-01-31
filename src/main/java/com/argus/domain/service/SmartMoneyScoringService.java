package com.argus.domain.service;

import com.argus.domain.model.AssetTransfer;
import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import com.argus.domain.model.WalletStatsSummary;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;
import com.argus.domain.port.persistence.WalletStatsPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SmartMoneyScoringService {

    private final WalletStatsPersistencePort walletStatsPort;
    private final TransactionPersistencePort transactionPort;
    private final WalletMetricsPersistencePort walletMetricsPort;
    private final WalletStatsAggregator walletStatsAggregator;
    private final Clock clock;

    private static final int MEV_HOLD_TIME_SEC = 120;
    private static final BigDecimal MEV_WIN_RATE = new BigDecimal("80");
    private static final BigDecimal MEV_AVG_ROI = new BigDecimal("5");
    private static final int MEV_TRADE_COUNT_7D = 50;

    private static final BigDecimal SNIPER_WIN_RATE = new BigDecimal("70");
    private static final int SNIPER_MIN_TRADES = 5;
    private static final int SNIPER_MAX_HOLD_TIME = 86400;

    private static final BigDecimal HOME_RUN_WIN_RATE = new BigDecimal("40");
    private static final BigDecimal HOME_RUN_MAX_ROI = new BigDecimal("500");
    private static final BigDecimal HOME_RUN_PROFIT_FACTOR = new BigDecimal("2.0");

    private static final BigDecimal WHALE_POSITION_SIZE = new BigDecimal("10000");
    private static final BigDecimal WHALE_NET_PNL = new BigDecimal("100000");

    private static final int ACCUMULATOR_BUYS_PER_TOKEN = 3;
    private static final BigDecimal ACCUMULATOR_SELL_RATIO = new BigDecimal("25");
    private static final int ACCUMULATOR_HOLD_TIME_DAYS = 14;

    public Optional<WalletMetrics> calculateMetrics(String walletAddress) {
        log.info("Calculating metrics for wallet: {}", walletAddress);

        WalletStatsSummary stats = walletStatsAggregator.aggregateStats(walletAddress).orElse(null);
        if (stats == null || stats.getTotalTrades() == null || stats.getTotalTrades() == 0) {
            log.warn("No stats found for wallet: {}", walletAddress);
            return Optional.empty();
        }

        WalletMetrics metrics = WalletMetrics.builder()
                .walletAddress(walletAddress.toLowerCase())
                .build();

        List<AssetTransfer> history = transactionPort.findByWalletAddress(walletAddress.toLowerCase(), 10000, "asc");
        calculateRawMetrics(metrics, stats, history);

        classifyArchetype(metrics, stats);

        calculateScores(metrics, stats);

        LocalDateTime now = LocalDateTime.now(clock);
        metrics.setCreatedAt(now);
        metrics.setUpdatedAt(now);

        return Optional.of(walletMetricsPort.save(metrics));
    }

    private void calculateRawMetrics(WalletMetrics metrics, WalletStatsSummary stats, List<AssetTransfer> history) {
        String walletAddr = metrics.getWalletAddress().toLowerCase();

        metrics.setMaxRoiPercent(stats.getMaxRoiPercent());
        metrics.setBuyVolUsd(stats.getTotalBuyVolume());
        metrics.setSellVolUsd(stats.getTotalSellVolume());

        if (stats.getGrossLoss() == null || stats.getGrossLoss().compareTo(BigDecimal.ZERO) == 0) {
            metrics.setProfitFactor(
                    stats.getGrossProfit() != null && stats.getGrossProfit().compareTo(BigDecimal.ZERO) > 0
                            ? new BigDecimal("999")
                            : BigDecimal.ZERO);
        } else {
            metrics.setProfitFactor(stats.getGrossProfit().divide(stats.getGrossLoss(), 4, RoundingMode.HALF_UP));
        }

        long totalHoldSeconds = 0;
        int completedRoundTrips = 0;
        int tradesLast7Days = 0;
        int totalBuyTransactions = 0;

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime lastTradeTime = null;

        Map<String, List<AssetTransfer>> txByToken = history.stream()
                .filter(tx -> tx.getTokenAddress() != null)
                .collect(Collectors.groupingBy(AssetTransfer::getTokenAddress));

        for (List<AssetTransfer> tokenTxs : txByToken.values()) {
            tokenTxs.sort((a, b) -> a.getTxTimestamp().compareTo(b.getTxTimestamp()));

            LocalDateTime tokenFirstBuy = null;
            LocalDateTime tokenLastSell = null;

            for (AssetTransfer tx : tokenTxs) {
                if (lastTradeTime == null || tx.getTxTimestamp().isAfter(lastTradeTime)) {
                    lastTradeTime = tx.getTxTimestamp();
                }

                if (tx.getTxTimestamp().isAfter(sevenDaysAgo)) {
                    tradesLast7Days++;
                }

                String txTo = tx.getTo() != null ? tx.getTo().toLowerCase() : "";
                boolean isBuy = txTo.equals(walletAddr);

                if (isBuy) {
                    if (tokenFirstBuy == null)
                        tokenFirstBuy = tx.getTxTimestamp();
                    totalBuyTransactions++;
                } else {
                    tokenLastSell = tx.getTxTimestamp();
                }
            }

            if (tokenFirstBuy != null && tokenLastSell != null) {
                Duration holdDuration = Duration.between(tokenFirstBuy, tokenLastSell);
                if (!holdDuration.isNegative()) {
                    totalHoldSeconds += holdDuration.getSeconds();
                    completedRoundTrips++;
                }
            }
        }

        if (completedRoundTrips > 0) {
            metrics.setAvgHoldTimeSec((int) (totalHoldSeconds / completedRoundTrips));
        } else {
            metrics.setAvgHoldTimeSec(0);
        }

        metrics.setTradeCount7d(tradesLast7Days);
        metrics.setTradeFrequencyPerMonth(new BigDecimal(tradesLast7Days).multiply(new BigDecimal("4")));

        if (totalBuyTransactions > 0) {
            metrics.setAvgPositionSizeUsd(
                    metrics.getBuyVolUsd().divide(new BigDecimal(totalBuyTransactions), 2, RoundingMode.HALF_UP));
        } else {
            metrics.setAvgPositionSizeUsd(BigDecimal.ZERO);
        }

        metrics.setLastTradeAt(lastTradeTime != null ? lastTradeTime : now);
    }

    private void classifyArchetype(WalletMetrics metrics, WalletStatsSummary stats) {
        if (isMevBot(metrics, stats)) {
            metrics.setArchetype(SmartMoneyArchetype.MEV_BOT);
            metrics.setIsBlacklisted(true);
            return;
        }

        metrics.setIsBlacklisted(false);

        if (isWhale(metrics, stats)) {
            metrics.setArchetype(SmartMoneyArchetype.WHALE);
            return;
        }

        if (isAccumulator(metrics)) {
            metrics.setArchetype(SmartMoneyArchetype.ACCUMULATOR);
            return;
        }
        if (isHomeRun(metrics, stats)) {
            metrics.setArchetype(SmartMoneyArchetype.HOME_RUN);
            return;
        }

        if (isSniper(metrics, stats)) {
            metrics.setArchetype(SmartMoneyArchetype.SNIPER);
            return;
        }

        metrics.setArchetype(SmartMoneyArchetype.UNKNOWN);
    }

    private boolean isMevBot(WalletMetrics metrics, WalletStatsSummary stats) {
        return metrics.getAvgHoldTimeSec() != null
                && metrics.getAvgHoldTimeSec() < MEV_HOLD_TIME_SEC
                && stats.getWinRate() != null
                && stats.getWinRate().compareTo(MEV_WIN_RATE) > 0
                && stats.getAvgRoiPercent() != null
                && stats.getAvgRoiPercent().compareTo(MEV_AVG_ROI) < 0
                && metrics.getTradeCount7d() != null
                && metrics.getTradeCount7d() > MEV_TRADE_COUNT_7D;
    }

    private boolean isWhale(WalletMetrics metrics, WalletStatsSummary stats) {
        return metrics.getAvgPositionSizeUsd() != null
                && metrics.getAvgPositionSizeUsd().compareTo(WHALE_POSITION_SIZE) > 0
                && stats.getTotalPnl() != null
                && stats.getTotalPnl().compareTo(WHALE_NET_PNL) > 0
                && metrics.getAvgHoldTimeSec() != null;
    }

    private boolean isAccumulator(WalletMetrics metrics) {
        int avgHoldTimeDays = metrics.getAvgHoldTimeSec() != null
                ? metrics.getAvgHoldTimeSec() / 86400
                : 0;

        if (avgHoldTimeDays < ACCUMULATOR_HOLD_TIME_DAYS) {
            return false;
        }

        BigDecimal buyVol = metrics.getBuyVolUsd() != null ? metrics.getBuyVolUsd() : BigDecimal.ZERO;
        BigDecimal sellVol = metrics.getSellVolUsd() != null ? metrics.getSellVolUsd() : BigDecimal.ZERO;

        if (buyVol.compareTo(BigDecimal.valueOf(100)) < 0) {
            return false;
        }

        BigDecimal sellRatio = BigDecimal.ZERO;

        if (buyVol.compareTo(BigDecimal.ZERO) > 0) {
            sellRatio = sellVol.divide(buyVol, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
        }
        return sellRatio.compareTo(ACCUMULATOR_SELL_RATIO) < 0;
    }

    private boolean isHomeRun(WalletMetrics metrics, WalletStatsSummary stats) {
        return stats.getWinRate() != null
                && stats.getWinRate().compareTo(HOME_RUN_WIN_RATE) < 0
                && metrics.getMaxRoiPercent() != null
                && metrics.getMaxRoiPercent().compareTo(HOME_RUN_MAX_ROI) > 0
                && metrics.getProfitFactor() != null
                && metrics.getProfitFactor().compareTo(HOME_RUN_PROFIT_FACTOR) > 0;
    }

    private boolean isSniper(WalletMetrics metrics, WalletStatsSummary stats) {
        return stats.getWinRate() != null
                && stats.getWinRate().compareTo(SNIPER_WIN_RATE) > 0
                && stats.getTotalTrades() != null
                && stats.getTotalTrades() >= SNIPER_MIN_TRADES;
    }

    private void calculateScores(WalletMetrics metrics, WalletStatsSummary stats) {
        BigDecimal pnlScore = BigDecimal.ZERO;
        if (stats.getTotalPnl() != null && stats.getTotalPnl().compareTo(BigDecimal.ZERO) > 0) {
            double logValue = Math.log10(Math.max(1, stats.getTotalPnl().doubleValue()));
            pnlScore = BigDecimal.valueOf(logValue * 15)
                    .max(BigDecimal.ZERO)
                    .min(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        metrics.setPnlScore(pnlScore);

        BigDecimal consistencyScore = BigDecimal.ZERO;
        if (stats.getWinRate() != null && stats.getTotalTrades() != null && stats.getTotalTrades() > 0) {
            BigDecimal winRateComponent = stats.getWinRate().multiply(BigDecimal.valueOf(0.7));

            double logTrades = Math.log10(stats.getTotalTrades());
            BigDecimal tradeComponent = BigDecimal.valueOf(logTrades * 20)
                    .min(BigDecimal.valueOf(100))
                    .multiply(BigDecimal.valueOf(0.3));

            consistencyScore = winRateComponent.add(tradeComponent)
                    .min(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        metrics.setConsistencyScore(consistencyScore);

        BigDecimal convictionScore = BigDecimal.ZERO;
        if (metrics.getBuyVolUsd() != null && metrics.getSellVolUsd() != null) {
            BigDecimal totalVol = metrics.getBuyVolUsd().add(metrics.getSellVolUsd());
            if (totalVol.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal buyDominance = metrics.getBuyVolUsd().divide(totalVol, 4, RoundingMode.HALF_UP);

                int holdTimeDays = metrics.getAvgHoldTimeSec() != null ? metrics.getAvgHoldTimeSec() / 86400 : 0;
                BigDecimal holdTimeBoost = BigDecimal.valueOf(holdTimeDays * 2)
                        .min(BigDecimal.valueOf(100));

                convictionScore = buyDominance.multiply(holdTimeBoost)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        metrics.setConvictionScore(convictionScore);
    }

    public int refreshAllMetrics() {
        log.info("Starting batch refresh of wallet metrics...");

        int page = 0;
        int batchSize = 100;
        int totalUpdated = 0;
        boolean hasMoreData = true;

        while (hasMoreData) {
            List<String> batch = walletStatsPort.findActiveWalletAddresses(page, batchSize);

            if (batch == null || batch.isEmpty()) {
                hasMoreData = false;
                break;
            }

            log.debug("Processing batch {} with {} wallets", page, batch.size());

            for (String address : batch) {
                try {
                    calculateMetrics(address);
                    totalUpdated++;
                } catch (Exception e) {
                    log.error("Failed to refresh metrics for wallet: {}", address, e);
                }
            }

            page++;
        }

        log.info("Completed metric refresh. Total wallets updated: {}", totalUpdated);
        return totalUpdated;
    }
}
