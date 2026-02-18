package com.argus.domain.port.persistence;

import com.argus.domain.model.Signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SignalPersistencePort {
    Signal save(Signal signal);

    boolean existsByTxHashAndType(String txHash, String type);

    List<Signal> findAll(boolean includeMev, int limit);

    long countDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);

    List<UUID> findDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);

    boolean multiWhaleSignalExistsForToken(String tokenAddress, LocalDateTime since);

    long countBuysByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);

    long countSellsByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);

    BigDecimal sumBuyValueByWalletAndToken(UUID walletId, String tokenAddress, LocalDateTime since);

    boolean accumulationSignalExists(UUID walletId, String tokenAddress, LocalDateTime since);

    List<com.argus.domain.model.SmartMoneyArchetype> findArchetypesByTokenAndCreatedAtAfter(
            String tokenAddress, LocalDateTime since, UUID currentWalletId);
}
