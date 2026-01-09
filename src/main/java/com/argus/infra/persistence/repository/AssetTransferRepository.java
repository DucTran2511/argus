package com.argus.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.argus.infra.persistence.entity.AssetTransferEntity;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDateTime;

public interface AssetTransferRepository extends JpaRepository<AssetTransferEntity, Long> {

    List<AssetTransferEntity> findByWalletAddressOrderByTxTimestampDesc(
            String walletAddress,
            Pageable pageable);

    List<AssetTransferEntity> findByWalletAddressAndTxTimestampBetween(
            String walletAddress,
            LocalDateTime start,
            LocalDateTime end);

    long countByWalletAddress(String walletAddress);

    boolean existsByTxHashAndWalletAddressAndCategory(
            String txHash,
            String walletAddress,
            String category);
}
