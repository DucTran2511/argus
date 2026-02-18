package com.argus.infra.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.argus.domain.model.WalletStats;
import com.argus.domain.port.persistence.WalletStatsPersistencePort;
import com.argus.infra.persistence.entity.WalletStatsEntity;
import com.argus.infra.persistence.repository.WalletStatsRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletStatsPersistenceAdapter implements WalletStatsPersistencePort {
    private final WalletStatsRepository repository;

    @Override
    @Transactional
    public WalletStats save(WalletStats stats) {
        String normalizedWallet = normalizeAddress(stats.getWalletAddress());
        String normalizedToken = normalizeAddress(stats.getTokenAddress());

        Optional<WalletStatsEntity> existing = repository
                .findByWalletAddressAndTokenAddress(normalizedWallet, normalizedToken);

        WalletStatsEntity entity = existing.orElse(new WalletStatsEntity());
        updateEntity(entity, stats);

        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public List<WalletStats> saveAll(List<WalletStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            return List.of();
        }

        // FIX: Batch upsert to avoid N+1 queries
        // Step 1: Collect all unique wallet+token pairs
        List<String[]> keys = statsList.stream()
                .map(s -> new String[] {
                        normalizeAddress(s.getWalletAddress()),
                        normalizeAddress(s.getTokenAddress())
                })
                .toList();

        // Step 2: Fetch all existing entities in ONE query
        List<String> walletAddresses = keys.stream().map(k -> k[0]).distinct().toList();
        List<WalletStatsEntity> existingEntities = repository.findByWalletAddressIn(walletAddresses);

        // Step 3: Build lookup map
        Map<String, WalletStatsEntity> existingMap = existingEntities.stream()
                .collect(Collectors.toMap(
                        e -> buildKey(e.getWalletAddress(), e.getTokenAddress()),
                        Function.identity()));

        // Step 4: Prepare entities for batch save
        List<WalletStatsEntity> toSave = new ArrayList<>();
        for (WalletStats stats : statsList) {
            String key = buildKey(
                    normalizeAddress(stats.getWalletAddress()),
                    normalizeAddress(stats.getTokenAddress()));

            WalletStatsEntity entity = existingMap.getOrDefault(key, new WalletStatsEntity());
            updateEntity(entity, stats);
            toSave.add(entity);
        }

        // Step 5: Batch save (Hibernate batching configured in application.properties)
        return repository.saveAll(toSave).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WalletStats> findByWalletAddress(String walletAddress) {
        return repository.findByWalletAddress(normalizeAddress(walletAddress))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WalletStats> findByWalletAndToken(String walletAddress, String tokenAddress) {
        return repository.findByWalletAddressAndTokenAddress(
                normalizeAddress(walletAddress),
                normalizeAddress(tokenAddress))
                .map(this::toDomain);
    }

    @Override
    public long countByWallet(String walletAddress) {
        return repository.countByWalletAddress(normalizeAddress(walletAddress));
    }

    @Override
    public long countProfitableByWallet(String walletAddress) {
        return repository.countByWalletAddressAndIsProfitableTrue(normalizeAddress(walletAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveWalletAddresses(int page, int size) {
        return repository.findDistinctWalletAddresses(PageRequest.of(page, size));
    }

    private String normalizeAddress(String address) {
        return address != null ? address.toLowerCase() : null;
    }

    private String buildKey(String wallet, String token) {
        return wallet + "|" + token;
    }

    private void updateEntity(WalletStatsEntity entity, WalletStats stats) {
        entity.setWalletAddress(normalizeAddress(stats.getWalletAddress()));
        entity.setTokenAddress(normalizeAddress(stats.getTokenAddress()));
        entity.setTokenSymbol(stats.getTokenSymbol());
        entity.setTotalBought(stats.getTotalBought());
        entity.setTotalSold(stats.getTotalSold());
        entity.setCostBasisUsd(stats.getCostBasisUsd());
        entity.setProceedsUsd(stats.getProceedsUsd());
        entity.setRealizedPnl(stats.getRealizedPnl());
        entity.setAvgBuyPrice(stats.getAvgBuyPrice());
        entity.setAvgSellPrice(stats.getAvgSellPrice());
        entity.setRoiPercent(stats.getRoiPercent());
        entity.setIsProfitable(stats.getIsProfitable());
        entity.setFirstTxAt(stats.getFirstTxAt());
        entity.setLastTxAt(stats.getLastTxAt());
    }

    private WalletStats toDomain(WalletStatsEntity entity) {
        return WalletStats.builder()
                .id(entity.getId())
                .walletAddress(entity.getWalletAddress())
                .tokenAddress(entity.getTokenAddress())
                .tokenSymbol(entity.getTokenSymbol())
                .totalBought(entity.getTotalBought())
                .totalSold(entity.getTotalSold())
                .costBasisUsd(entity.getCostBasisUsd())
                .proceedsUsd(entity.getProceedsUsd())
                .realizedPnl(entity.getRealizedPnl())
                .avgBuyPrice(entity.getAvgBuyPrice())
                .avgSellPrice(entity.getAvgSellPrice())
                .roiPercent(entity.getRoiPercent())
                .isProfitable(entity.getIsProfitable())
                .firstTxAt(entity.getFirstTxAt())
                .lastTxAt(entity.getLastTxAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}