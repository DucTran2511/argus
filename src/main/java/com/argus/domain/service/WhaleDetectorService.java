package com.argus.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.infra.stream.dto.TransactionEvent;

import com.argus.domain.model.Signal;
import com.argus.domain.model.SignalType;

@Slf4j
@RequiredArgsConstructor
public class WhaleDetectorService {
    private final SignalPersistencePort signalPersistencePort;
    private static final BigDecimal WHALE_THRESHOLD = new BigDecimal("50000");

    public Optional<Signal> detectAndSaveWhaleSignal(TransactionEvent event, String walletAddress) {
        if (event == null || event.getUsdValue() == null) {
            return Optional.empty();
        }

        if (!isWhaleTransaction(event.getUsdValue())) {
            return Optional.empty();
        }

        SignalType signalType = determineDirection(event.getFrom(), event.getTo(), walletAddress);
        BigDecimal confidence = calculateConfidence(event.getUsdValue());

        Signal signal = Signal.builder()
                .type(signalType.name())
                .tokenAddress(event.getTokenAddress())
                .tokenSymbol(event.getTokenSymbol())
                .chain("ethereum")
                .usdValue(event.getUsdValue().setScale(2, RoundingMode.HALF_UP))
                .confidenceScore(confidence)
                .txHash(event.getTxHash())
                .build();

        Signal saved = signalPersistencePort.save(signal);
        log.info("🐋 Whale signal saved: {} ${} tx={}", signalType,
                event.getUsdValue().setScale(0, RoundingMode.HALF_UP), event.getTxHash());
        return Optional.of(saved);
    }

    public boolean isWhaleTransaction(BigDecimal usdValue) {
        return usdValue != null && usdValue.compareTo(WHALE_THRESHOLD) >= 0;
    }

    private SignalType determineDirection(String from, String to, String walletAddress) {
        if (walletAddress == null)
            return SignalType.WHALE_BUY;

        String wallet = walletAddress.toLowerCase();
        if (to != null && to.toLowerCase().equals(wallet))
            return SignalType.WHALE_BUY;
        if (from != null && from.toLowerCase().equals(wallet))
            return SignalType.WHALE_SELL;
        return SignalType.WHALE_BUY;
    }

    private BigDecimal calculateConfidence(BigDecimal usdValue) {
        if (usdValue.compareTo(new BigDecimal("500000")) >= 0)
            return new BigDecimal("0.95");
        if (usdValue.compareTo(new BigDecimal("100000")) >= 0)
            return new BigDecimal("0.85");
        return new BigDecimal("0.70");
    }

}
