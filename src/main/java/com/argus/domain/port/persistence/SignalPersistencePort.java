package com.argus.domain.port.persistence;

import com.argus.domain.model.Signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SignalPersistencePort {
    Signal save(Signal signal);

    boolean existsByTxHashAndType(String txHash, String type);

    long countDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);

    List<UUID> findDistinctWhaleBuyersByToken(String tokenAddress, LocalDateTime since);

    boolean multiWhaleSignalExistsForToken(String tokenAddress, LocalDateTime since);
}
