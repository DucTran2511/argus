package com.argus.infra.persistence.adapter;

import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;
import com.argus.infra.persistence.entity.WalletMetricsEntity;
import com.argus.infra.persistence.repository.WalletMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WalletMetricsPersistenceAdapter implements WalletMetricsPersistencePort {

    private final WalletMetricsRepository repository;

    @Override
    public WalletMetrics save(WalletMetrics metrics) {
        WalletMetricsEntity entity = toEntity(metrics);
        return toDomain(repository.save(entity));
    }

    @Override
    public List<WalletMetrics> saveAll(List<WalletMetrics> metrics) {
        List<WalletMetricsEntity> entities = metrics.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return repository.saveAll(entities).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WalletMetrics> findByWalletAddress(String walletAddress) {
        return repository.findById(walletAddress.toLowerCase())
                .map(this::toDomain);
    }

    @Override
    public List<WalletMetrics> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<WalletMetrics> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<WalletMetrics> findByArchetype(SmartMoneyArchetype archetype) {
        return repository.findByArchetype(toEntityArchetype(archetype), Pageable.unpaged())
                .getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<WalletMetrics> findByArchetype(SmartMoneyArchetype archetype, Pageable pageable) {
        return repository.findByArchetype(toEntityArchetype(archetype), pageable)
                .map(this::toDomain);
    }

    @Override
    public List<WalletMetrics> findByArchetypeNotBlacklisted(SmartMoneyArchetype archetype) {
        return repository.findByArchetypeAndNotBlacklisted(toEntityArchetype(archetype)).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletMetrics> findAllNotBlacklisted() {
        return repository.findAllNotBlacklisted().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByArchetype(SmartMoneyArchetype archetype) {
        return repository.countByArchetype(toEntityArchetype(archetype));
    }

    @Override
    public void deleteByWalletAddress(String walletAddress) {
        repository.deleteById(walletAddress.toLowerCase());
    }

    private WalletMetricsEntity toEntity(WalletMetrics domain) {
        if (domain == null)
            return null;
        return WalletMetricsEntity.builder()
                .walletAddress(domain.getWalletAddress() != null ? domain.getWalletAddress().toLowerCase() : null)
                .archetype(toEntityArchetype(domain.getArchetype()))
                .isBlacklisted(domain.getIsBlacklisted())
                .avgPositionSizeUsd(domain.getAvgPositionSizeUsd())
                .tradeFrequencyPerMonth(domain.getTradeFrequencyPerMonth())
                .tradeCount7d(domain.getTradeCount7d())
                .maxRoiPercent(domain.getMaxRoiPercent())
                .profitFactor(domain.getProfitFactor())
                .avgHoldTimeSec(domain.getAvgHoldTimeSec())
                .buyVolUsd(domain.getBuyVolUsd())
                .sellVolUsd(domain.getSellVolUsd())
                .pnlScore(domain.getPnlScore())
                .consistencyScore(domain.getConsistencyScore())
                .convictionScore(domain.getConvictionScore())
                .totalScore(domain.getTotalScore())
                .tier(domain.getTier())
                .lastTradeAt(domain.getLastTradeAt())
                .build();
    }

    private WalletMetrics toDomain(WalletMetricsEntity entity) {
        if (entity == null)
            return null;
        return WalletMetrics.builder()
                .walletAddress(entity.getWalletAddress())
                .archetype(toDomainArchetype(entity.getArchetype()))
                .isBlacklisted(entity.getIsBlacklisted())
                .avgPositionSizeUsd(entity.getAvgPositionSizeUsd())
                .tradeFrequencyPerMonth(entity.getTradeFrequencyPerMonth())
                .tradeCount7d(entity.getTradeCount7d())
                .maxRoiPercent(entity.getMaxRoiPercent())
                .profitFactor(entity.getProfitFactor())
                .avgHoldTimeSec(entity.getAvgHoldTimeSec())
                .buyVolUsd(entity.getBuyVolUsd())
                .sellVolUsd(entity.getSellVolUsd())
                .pnlScore(entity.getPnlScore())
                .consistencyScore(entity.getConsistencyScore())
                .convictionScore(entity.getConvictionScore())
                .totalScore(entity.getTotalScore())
                .tier(entity.getTier())
                .lastTradeAt(entity.getLastTradeAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WalletMetricsEntity.Archetype toEntityArchetype(SmartMoneyArchetype domain) {
        if (domain == null)
            return null;
        try {
            return WalletMetricsEntity.Archetype.valueOf(domain.name());
        } catch (IllegalArgumentException e) {
            return WalletMetricsEntity.Archetype.UNKNOWN;
        }
    }

    private SmartMoneyArchetype toDomainArchetype(WalletMetricsEntity.Archetype entity) {
        if (entity == null)
            return null;
        try {
            return SmartMoneyArchetype.valueOf(entity.name());
        } catch (IllegalArgumentException e) {
            return SmartMoneyArchetype.UNKNOWN;
        }
    }
}
