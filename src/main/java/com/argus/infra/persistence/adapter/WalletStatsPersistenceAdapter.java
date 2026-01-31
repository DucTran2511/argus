package com.argus.infra.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import com.argus.domain.model.WalletStats;
import com.argus.domain.model.WalletStatsSummary;
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

    @Override
    public WalletStatsSummary getStatsAggregated(String walletAddress) {
        List<WalletStats> tokenStats = findByWalletAddress(walletAddress);

        if (tokenStats.isEmpty()) {
            return null;
        }

        BigDecimal totalPnl = BigDecimal.ZERO;
        int profitableTrades = 0;
        BigDecimal roiSum = BigDecimal.ZERO;
        int closedPositionCount = 0;

        for (WalletStats stats : tokenStats) {
            if (stats.getRealizedPnl() != null) {
                totalPnl = totalPnl.add(stats.getRealizedPnl());
            }

            if (Boolean.TRUE.equals(stats.getIsProfitable())) {
                profitableTrades++;
            }

            if (stats.getTotalSold() != null && stats.getTotalSold().compareTo(BigDecimal.ZERO) > 0) {
                if (stats.getRoiPercent() != null) {
                    roiSum = roiSum.add(stats.getRoiPercent());
                }
                closedPositionCount++;
            }
        }

        int totalTrades = tokenStats.size();

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(profitableTrades)
                        .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal avgRoi = closedPositionCount > 0
                ? roiSum.divide(BigDecimal.valueOf(closedPositionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return WalletStatsSummary.builder()
                .walletAddress(walletAddress.toLowerCase())
                .totalPnl(totalPnl)
                .winRate(winRate)
                .totalTrades(totalTrades)
                .profitableTrades(profitableTrades)
                .avgRoiPercent(avgRoi)
                .tokenStats(tokenStats)
                .build();
    }

    @Override
    public List<String> findActiveWalletAddresses(int page, int size) {
        return repository.findDistinctWalletAddresses(PageRequest.of(page, size));
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