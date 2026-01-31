package com.argus.domain.port.persistence;

import com.argus.domain.model.SmartMoneyArchetype;
import com.argus.domain.model.WalletMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WalletMetricsPersistencePort {

    WalletMetrics save(WalletMetrics metrics);

    List<WalletMetrics> saveAll(List<WalletMetrics> metrics);

    Optional<WalletMetrics> findByWalletAddress(String walletAddress);

    List<WalletMetrics> findAll();

    Page<WalletMetrics> findAll(Pageable pageable);

    List<WalletMetrics> findByArchetype(SmartMoneyArchetype archetype);

    Page<WalletMetrics> findByArchetype(SmartMoneyArchetype archetype, Pageable pageable);

    List<WalletMetrics> findByArchetypeNotBlacklisted(SmartMoneyArchetype archetype);

    List<WalletMetrics> findAllNotBlacklisted();

    long count();

    long countByArchetype(SmartMoneyArchetype archetype);

    void deleteByWalletAddress(String walletAddress);
}
