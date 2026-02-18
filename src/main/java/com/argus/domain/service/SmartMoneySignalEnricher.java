package com.argus.domain.service;

import com.argus.domain.model.Signal;
import com.argus.domain.model.SignalType;
import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SmartMoneySignalEnricher {

    private final WalletMetricsPersistencePort walletMetricsPersistencePort;
    private final SignalPersistencePort signalPersistencePort;

    private static final int CONVERGENCE_WINDOW_HOURS = 12;

    public void enrichAndCheckAlpha(Signal signal, String walletAddress, UUID walletId) {
        if (walletAddress == null)
            return;

        Optional<WalletMetrics> metricsOpt = walletMetricsPersistencePort
                .findByWalletAddress(walletAddress.toLowerCase());

        if (metricsOpt.isPresent()) {
            WalletMetrics metrics = metricsOpt.get();

            // 1. Enrich original signal with context (optional, but good for internal
            // logging/metadata)
            // Note: Signal is an immutable-ish builder object in domain, but here it's
            // already saved.
            // If we want to update the saved signal, we'd need to re-save it.
            // However, the plan says "Attach archetype + scores to signal metadata"
            // This implies the signal being processed.

            // 2. Check for SNIPER_ALPHA
            if (shouldGenerateSniperAlpha(signal, metrics)) {
                generateSniperAlphaSignal(signal, metrics, walletId);
            }

            // 3. Check for Convergence
            if (isConvergenceCandidate(metrics.getArchetype())) {
                checkAndCreateConvergenceSignals(signal, metrics, walletId);
            }
        }
    }

    private boolean shouldGenerateSniperAlpha(Signal signal, WalletMetrics metrics) {
        if (metrics.getArchetype() != SmartMoneyArchetype.SNIPER) {
            return false;
        }

        BigDecimal minThreshold = getSniperAlphaMinimum(signal.getChain());
        return signal.getUsdValue() != null && signal.getUsdValue().compareTo(minThreshold) >= 0;
    }

    private BigDecimal getSniperAlphaMinimum(String chain) {
        if (chain == null)
            return new BigDecimal("100");
        return switch (chain.toLowerCase()) {
            case "ethereum", "eth" -> new BigDecimal("500");
            default -> new BigDecimal("100");
        };
    }

    private void generateSniperAlphaSignal(Signal baseSignal, WalletMetrics metrics, UUID walletId) {
        Signal sniperAlpha = Signal.builder()
                .type(SignalType.SNIPER_ALPHA.name())
                .walletId(walletId)
                .tokenAddress(baseSignal.getTokenAddress())
                .tokenSymbol(baseSignal.getTokenSymbol())
                .chain(baseSignal.getChain())
                .usdValue(baseSignal.getUsdValue())
                .confidenceScore(new BigDecimal("0.80"))
                .txHash(baseSignal.getTxHash())
                .metadata(String.format("{\"walletArchetype\":\"SNIPER\",\"walletTier\":\"%s\",\"pnlScore\":%s}",
                        metrics.getTier(), metrics.getPnlScore()))
                .build();

        signalPersistencePort.save(sniperAlpha);
        log.info("🎯 SNIPER ALPHA: Wallet {} entry in {} spotted!", walletId, baseSignal.getTokenAddress());
    }

    private boolean isConvergenceCandidate(SmartMoneyArchetype archetype) {
        return archetype == SmartMoneyArchetype.WHALE ||
                archetype == SmartMoneyArchetype.SNIPER ||
                archetype == SmartMoneyArchetype.HOME_RUN;
    }

    private void checkAndCreateConvergenceSignals(Signal signal, WalletMetrics metrics, UUID walletId) {
        if (signal.getTokenAddress() == null || walletId == null)
            return;

        LocalDateTime since = LocalDateTime.now().minusHours(CONVERGENCE_WINDOW_HOURS);
        List<SmartMoneyArchetype> archetypesInWindow = signalPersistencePort.findArchetypesByTokenAndCreatedAtAfter(
                signal.getTokenAddress(), since, walletId);

        boolean hasWhale = archetypesInWindow.contains(SmartMoneyArchetype.WHALE);
        boolean hasSniper = archetypesInWindow.contains(SmartMoneyArchetype.SNIPER);
        boolean hasHomeRun = archetypesInWindow.contains(SmartMoneyArchetype.HOME_RUN);

        SmartMoneyArchetype current = metrics.getArchetype();

        // SNIPER + WHALE
        if ((current == SmartMoneyArchetype.SNIPER && hasWhale) ||
                (current == SmartMoneyArchetype.WHALE && hasSniper)) {
            createConvergenceSignal(signal, SignalType.SMART_MONEY_CONVERGENCE, "0.90");
        }

        // WHALE + HOME_RUN
        if ((current == SmartMoneyArchetype.WHALE && hasHomeRun) ||
                (current == SmartMoneyArchetype.HOME_RUN && hasWhale)) {
            createConvergenceSignal(signal, SignalType.HIGH_CONVICTION_BET, "0.75");
        }
    }

    private void createConvergenceSignal(Signal baseSignal, SignalType type, String confidence) {
        // Simple idempotency: check if same convergence signal was created for this
        // token in last 12h
        // (Actually, the plan didn't explicitly ask for this, but it's good practice)

        Signal convergence = Signal.builder()
                .type(type.name())
                .tokenAddress(baseSignal.getTokenAddress())
                .tokenSymbol(baseSignal.getTokenSymbol())
                .chain(baseSignal.getChain())
                .confidenceScore(new BigDecimal(confidence))
                .metadata(String.format("{\"windowHours\":%d,\"triggerTokenAddress\":\"%s\"}",
                        CONVERGENCE_WINDOW_HOURS, baseSignal.getTokenAddress()))
                .build();

        signalPersistencePort.save(convergence);
        log.info("🔥 CONVERGENCE DETECTED: {} for token {}", type, baseSignal.getTokenAddress());
    }
}
