package com.argus.infra.persistence.adapter;

import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.infra.persistence.entity.SignalEntity;
import com.argus.infra.persistence.repository.SignalRepository;
import com.argus.domain.model.SmartMoneyArchetype;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;

import com.argus.domain.model.Signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignalPersistenceAdapter implements SignalPersistencePort {

    private final SignalRepository signalRepository;

    @Override
    @Transactional
    public Signal save(Signal signal) {
        validateSignal(signal);
        SignalEntity signalEntity = toEntity(signal);
        SignalEntity saved = signalRepository.save(signalEntity);
        return toDomain(saved);
    }

    @Override
    public List<Signal> findAll(boolean includeMev, int limit) {
        return signalRepository.findAllSignals(includeMev, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTxHashAndType(String txHash, String type) {
        return signalRepository.existsByTxHashAndType(txHash, type);
    }

    @Override
    public long countDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since) {
        return signalRepository.countDistinctWalletsByTokenAndTypeAfter(
                tokenAddress, "WHALE_BUY", since);
    }

    @Override
    public List<UUID> findDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since) {
        return signalRepository.findDistinctWalletIdsByTokenAndTypeAfter(
                tokenAddress, "WHALE_BUY", since);
    }

    @Override
    public boolean multiWhaleSignalExistsForToken(String tokenAddress, LocalDateTime since) {
        return signalRepository.existsByTokenAddressAndTypeAndCreatedAtAfter(
                tokenAddress, "MULTI_WHALE", since);
    }

    @Override
    public long countBuysByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since) {
        return signalRepository.countByWalletAndTokenAndTypeAfter(
                walletId, tokenAddress, "WHALE_BUY", since);
    }

    @Override
    public long countSellsByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since) {
        return signalRepository.countByWalletAndTokenAndTypeAfter(
                walletId, tokenAddress, "WHALE_SELL", since);
    }

    @Override
    public BigDecimal sumBuyValueByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since) {
        return signalRepository.sumUsdValueByWalletAndTokenAndTypeAfter(
                walletId, tokenAddress, "WHALE_BUY", since);
    }

    @Override
    public boolean accumulationSignalExists(UUID walletId, String tokenAddress, LocalDateTime since) {
        return signalRepository.existsByWalletIdAndTokenAddressAndTypeAndCreatedAtAfter(
                walletId, tokenAddress, "ACCUMULATION", since);
    }

    @Override
    public List<SmartMoneyArchetype> findArchetypesByTokenAndCreatedAtAfter(
            String tokenAddress, LocalDateTime since, UUID currentWalletId) {
        return signalRepository.findArchetypesByTokenAndCreatedAtAfter(tokenAddress, since, currentWalletId).stream()
                .map(this::toDomainArchetype)
                .toList();
    }

    private SmartMoneyArchetype toDomainArchetype(
            com.argus.infra.persistence.entity.WalletMetricsEntity.Archetype archetype) {
        if (archetype == null)
            return SmartMoneyArchetype.UNKNOWN;
        return SmartMoneyArchetype.valueOf(archetype.name());
    }

    private void validateSignal(Signal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
    }

    private SignalEntity toEntity(Signal signal) {
        return SignalEntity.builder()
                .id(signal.getId())
                .type(signal.getType())
                .walletId(signal.getWalletId())
                .tokenAddress(signal.getTokenAddress())
                .tokenSymbol(signal.getTokenSymbol())
                .chain(signal.getChain())
                .usdValue(signal.getUsdValue())
                .confidenceScore(signal.getConfidenceScore())
                .txHash(signal.getTxHash())
                .aiNarrative(signal.getAiNarrative())
                .metadata(signal.getMetadata())
                .createdAt(signal.getCreatedAt())
                .build();
    }

    private Signal toDomain(SignalEntity signalEntity) {
        return Signal.builder()
                .id(signalEntity.getId())
                .type(signalEntity.getType())
                .walletId(signalEntity.getWalletId())
                .tokenAddress(signalEntity.getTokenAddress())
                .tokenSymbol(signalEntity.getTokenSymbol())
                .chain(signalEntity.getChain())
                .usdValue(signalEntity.getUsdValue())
                .confidenceScore(signalEntity.getConfidenceScore())
                .txHash(signalEntity.getTxHash())
                .aiNarrative(signalEntity.getAiNarrative())
                .metadata(signalEntity.getMetadata())
                .createdAt(signalEntity.getCreatedAt())
                .build();
    }
}
