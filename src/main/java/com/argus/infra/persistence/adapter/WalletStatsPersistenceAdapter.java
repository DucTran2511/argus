package com.argus.infra.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        Optional<WalletStatsEntity> existing = repository
                .findByWalletAddressAndTokenAddress(stats.getWalletAddress(), stats.getTokenAddress());

        WalletStatsEntity entity = existing.orElse(new WalletStatsEntity());
        updateEntity(entity, stats);

        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public List<WalletStats> saveAll(List<WalletStats> statsList) {
        return statsList.stream().map(this::save).toList();
    }

    @Override
    public List<WalletStats> findByWalletAddress(String walletAddress) {
        return repository.findByWalletAddress(walletAddress.toLowerCase())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WalletStats> findByWalletAndToken(String walletAddress, String tokenAddress) {
        return repository.findByWalletAddressAndTokenAddress(
                walletAddress.toLowerCase(), tokenAddress.toLowerCase())
                .map(this::toDomain);
    }

    @Override
    public long countByWallet(String walletAddress) {
        return repository.countByWalletAddress(walletAddress.toLowerCase());
    }

    @Override
    public long countProfitableByWallet(String walletAddress) {
        return repository.countByWalletAddressAndIsProfitableTrue(walletAddress.toLowerCase());
    }

    private void updateEntity(WalletStatsEntity entity, WalletStats stats) {
        entity.setWalletAddress(stats.getWalletAddress().toLowerCase());
        entity.setTokenAddress(stats.getTokenAddress().toLowerCase());
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