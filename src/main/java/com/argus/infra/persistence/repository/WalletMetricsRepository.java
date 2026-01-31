package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.WalletMetricsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletMetricsRepository extends JpaRepository<WalletMetricsEntity, String> {

    Page<WalletMetricsEntity> findByArchetype(WalletMetricsEntity.Archetype archetype, Pageable pageable);

    @Query("SELECT m FROM WalletMetricsEntity m WHERE m.archetype = :archetype AND (m.isBlacklisted IS NULL OR m.isBlacklisted = false)")
    List<WalletMetricsEntity> findByArchetypeAndNotBlacklisted(
            @Param("archetype") WalletMetricsEntity.Archetype archetype);

    @Query("SELECT m FROM WalletMetricsEntity m WHERE m.isBlacklisted IS NULL OR m.isBlacklisted = false")
    List<WalletMetricsEntity> findAllNotBlacklisted();

    Optional<WalletMetricsEntity> findByWalletAddress(String walletAddress);

    long countByArchetype(WalletMetricsEntity.Archetype archetype);
}
