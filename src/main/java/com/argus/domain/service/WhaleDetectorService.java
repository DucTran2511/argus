package com.argus.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.domain.model.Signal;
import com.argus.domain.model.SignalType;
import com.argus.domain.model.dto.WhaleDetectionRequest;

@Slf4j
@RequiredArgsConstructor
public class WhaleDetectorService {
    private final SignalPersistencePort signalPersistencePort;
    private final SmartMoneySignalEnricher smartMoneySignalEnricher;
    private final com.argus.domain.port.persistence.WalletMetricsPersistencePort walletMetricsPersistencePort;
    private final RuleMatcherService ruleMatcherService;

    private static final BigDecimal WHALE_THRESHOLD = new BigDecimal("50000");
    private static final int SIGNAL_WINDOW_MINUTES = 10;
    private static final BigDecimal CONFIDENCE_TIER_HIGH = new BigDecimal("500000");
    private static final BigDecimal CONFIDENCE_TIER_MED = new BigDecimal("100000");
    private static final BigDecimal CONFIDENCE_SCORE_HIGH = new BigDecimal("0.95");
    private static final BigDecimal CONFIDENCE_SCORE_MED = new BigDecimal("0.85");
    private static final BigDecimal CONFIDENCE_SCORE_LOW = new BigDecimal("0.70");
    private static final int ACCUMULATION_THRESHOLD = 3;
    private static final int ACCUMULATION_WINDOW_HOURS = 72;

    private static final int MULTI_WHALE_THRESHOLD = 3;
    private static final int MULTI_WHALE_WINDOW_HOURS = 24;

    public Optional<Signal> detectAndSaveWhaleSignal(WhaleDetectionRequest request) {
        if (request == null || request.getUsdValue() == null) {
            return Optional.empty();
        }

        if (isStaleTransaction(request.getTimestamp())) {
            log.debug("Skipping stale transaction: {} (timestamp: {})",
                    request.getTxHash(), request.getTimestamp());
            return Optional.empty();
        }

        if (!isWhaleTransaction(request.getUsdValue())) {
            return Optional.empty();
        }

        SignalType signalType = determineDirection(request.getFrom(), request.getTo(), request.getWalletAddress());

        if (signalPersistencePort.existsByTxHashAndType(request.getTxHash(), signalType.name())) {
            log.debug("Signal already exists: {} {}", signalType, request.getTxHash());
            return Optional.empty();
        }

        if (request.getWalletAddress() != null) {
            Optional<com.argus.domain.model.WalletMetrics> metrics = walletMetricsPersistencePort
                    .findByWalletAddress(request.getWalletAddress().toLowerCase());
            if (metrics.isPresent()
                    && metrics.get().getArchetype() == com.argus.domain.model.SmartMoneyArchetype.MEV_BOT) {
                log.debug("Ignoring signal from MEV_BOT: {}", request.getWalletAddress());
                return Optional.empty();
            }
        }

        if (signalType == SignalType.WHALE_BUY && request.getTokenAddress() != null && request.getWalletId() != null) {
            checkAccumulationPattern(request.getWalletId(), request.getTokenAddress());
        }

        BigDecimal confidence = calculateAccumulationConfidence(request.getUsdValue());

        Signal signal = Signal.builder()
                .type(signalType.name())
                .walletId(request.getWalletId())
                .tokenAddress(request.getTokenAddress())
                .tokenSymbol(request.getTokenSymbol())
                .chain("ethereum")
                .usdValue(request.getUsdValue().setScale(2, RoundingMode.HALF_UP))
                .confidenceScore(confidence)
                .txHash(request.getTxHash())
                .build();

        Signal saved = signalPersistencePort.save(signal);
        log.info("🐋 Whale signal saved: {} ${} tx={}", signalType,
                request.getUsdValue().setScale(0, RoundingMode.HALF_UP), request.getTxHash());

        ruleMatcherService.matchSignal(saved);
        smartMoneySignalEnricher.enrichAndCheckAlpha(saved, request.getWalletAddress(), request.getWalletId());

        if (signalType == SignalType.WHALE_BUY && request.getTokenAddress() != null) {
            checkAndCreateMultiWhaleSignal(request.getTokenAddress());
        }

        return Optional.of(saved);
    }

    public boolean isWhaleTransaction(BigDecimal usdValue) {
        return usdValue != null && usdValue.compareTo(WHALE_THRESHOLD) >= 0;
    }

    private boolean isStaleTransaction(LocalDateTime txTimestamp) {
        return txTimestamp == null || txTimestamp.isBefore(LocalDateTime.now().minusMinutes(SIGNAL_WINDOW_MINUTES));
    }

    private SignalType determineDirection(String from, String to, String walletAddress) {
        if (walletAddress == null) {
            return SignalType.WHALE_BUY;
        }

        if (to != null && to.equalsIgnoreCase(walletAddress)) {
            return SignalType.WHALE_BUY;
        }
        if (from != null && from.equalsIgnoreCase(walletAddress)) {
            return SignalType.WHALE_SELL;
        }
        return SignalType.WHALE_BUY;
    }

    private BigDecimal calculateAccumulationConfidence(BigDecimal usdValue) {
        if (usdValue.compareTo(CONFIDENCE_TIER_HIGH) >= 0) {
            return CONFIDENCE_SCORE_HIGH;
        }
        if (usdValue.compareTo(CONFIDENCE_TIER_MED) >= 0) {
            return CONFIDENCE_SCORE_MED;
        }
        return CONFIDENCE_SCORE_LOW;
    }

    private void checkAccumulationPattern(UUID walletId, String tokenAddress) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(ACCUMULATION_WINDOW_HOURS);

        if (signalPersistencePort.accumulationSignalExists(walletId, tokenAddress, windowStart)) {
            log.debug("ACCUMULATION signal already exists for wallet: {} token: {}", walletId, tokenAddress);
            return;
        }

        long buyCount = signalPersistencePort.countBuysByWalletAndToken(walletId, tokenAddress, windowStart);
        long sellCount = signalPersistencePort.countSellsByWalletAndToken(walletId, tokenAddress, windowStart);

        if (buyCount >= ACCUMULATION_THRESHOLD && sellCount == 0) {
            BigDecimal totalPosition = signalPersistencePort.sumBuyValueByWalletAndToken(walletId, tokenAddress,
                    windowStart);

            Signal accumulationSignal = Signal.builder()
                    .type(SignalType.ACCUMULATION.name())
                    .walletId(walletId)
                    .tokenAddress(tokenAddress)
                    .chain("ethereum")
                    .usdValue(totalPosition)
                    .confidenceScore(calculateAccumulationConfidence(totalPosition))
                    .metadata(buildAccumulationMetadata(buyCount, totalPosition))
                    .build();

            signalPersistencePort.save(accumulationSignal);
            log.info("📈 ACCUMULATION ALERT: Wallet {} buying {} x{} times (${} total) in 72h!",
                    walletId, tokenAddress, buyCount, totalPosition.setScale(0, RoundingMode.HALF_UP));
        }
    }

    private void checkAndCreateMultiWhaleSignal(String tokenAddress) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(MULTI_WHALE_WINDOW_HOURS);

        if (signalPersistencePort.multiWhaleSignalExistsForToken(tokenAddress, windowStart)) {
            log.debug("MULTI_WHALE signal already exists for token: {}", tokenAddress);
            return;
        }

        List<UUID> walletIds = signalPersistencePort.findDistinctWhaleBuyersByToken(tokenAddress, windowStart);
        long whaleCount = walletIds.size();

        if (whaleCount >= MULTI_WHALE_THRESHOLD) {

            Signal multiWhaleSignal = Signal.builder()
                    .type(SignalType.MULTI_WHALE.name())
                    .tokenAddress(tokenAddress)
                    .chain("ethereum")
                    .confidenceScore(calculateMultiWhaleConfidence(whaleCount))
                    .metadata(buildMultiWhaleMetadata(whaleCount, walletIds))
                    .build();

            signalPersistencePort.save(multiWhaleSignal);
            log.info("🐋🐋🐋 MULTI-WHALE ALERT: {} whales buying {} in 24h!",
                    whaleCount, tokenAddress);
        }
    }

    private BigDecimal calculateMultiWhaleConfidence(long whaleCount) {
        if (whaleCount >= 5)
            return CONFIDENCE_SCORE_HIGH;
        if (whaleCount >= 4)
            return CONFIDENCE_SCORE_MED;
        return CONFIDENCE_SCORE_LOW;
    }

    private String buildMultiWhaleMetadata(long whaleCount, List<UUID> walletIds) {
        String walletIdsJson = walletIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        return String.format(
                "{\"whaleCount\":%d,\"walletIds\":%s,\"windowHours\":%d}",
                whaleCount, walletIdsJson, MULTI_WHALE_WINDOW_HOURS);
    }

    private BigDecimal calculateAccumulationConfidence(long buyCount) {
        if (buyCount >= 5)
            return CONFIDENCE_SCORE_HIGH;
        if (buyCount >= 4)
            return CONFIDENCE_SCORE_MED;
        return CONFIDENCE_SCORE_LOW;
    }

    private String buildAccumulationMetadata(long buyCount, BigDecimal totalPosition) {
        return String.format(
                "{\"buyCount\":%d,\"totalPositionUsd\":%.2f,\"windowHours\":%d}",
                buyCount, totalPosition, ACCUMULATION_WINDOW_HOURS);
    }
}
