package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    Optional<WalletEntity> findByAddress(String address);

    List<WalletEntity> findByChain(String chain);

    List<WalletEntity> findByType(String type);

    List<WalletEntity> findByChainAndType(String chain, String type);

    boolean existsByAddress(String address);

    List<WalletEntity> findByTypeOrderByTotalPnlDesc(String type);

    @Query("SELECT w FROM WalletEntity w WHERE w.winRate > :minWinRate AND w.totalPnl > 0 ORDER BY w.totalPnl DESC")
    List<WalletEntity> findTopPerformers(Double minWinRate);
}
