package com.argus.infra.persistence.adapter;

import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.infra.persistence.entity.SignalEntity;
import com.argus.infra.persistence.repository.SignalRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.argus.domain.model.Signal;

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
                .aiNarrative(signalEntity.getAiNarrative())
                .metadata(signalEntity.getMetadata())
                .createdAt(signalEntity.getCreatedAt())
                .build();
    }
}
