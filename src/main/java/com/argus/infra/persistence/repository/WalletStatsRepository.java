package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.WalletStatsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WalletStatsRepository extends JpaRepository<WalletStatsEntity, Long> {

    List<WalletStatsEntity> findByWalletAddress(String walletAddress);

    List<WalletStatsEntity> findByWalletAddressIn(List<String> walletAddresses);

    Optional<WalletStatsEntity> findByWalletAddressAndTokenAddress(
            String walletAddress, String tokenAddress);

    List<WalletStatsEntity> findByWalletAddressAndIsProfitableTrue(String walletAddress);

    long countByWalletAddress(String walletAddress);

    long countByWalletAddressAndIsProfitableTrue(String walletAddress);

    @Query("SELECT DISTINCT ws.walletAddress FROM WalletStatsEntity ws ORDER BY ws.walletAddress")
    List<String> findDistinctWalletAddresses(Pageable pageable);
}