package com.argus.domain.port.cache;

import java.util.Optional;
import java.util.Set;

public interface BlockTrackingPort {
    Optional<Long> getLastProcessedBlock();

    void setLastProcessedBlock(long blockNumber);

    Set<String> getTrackedWalletAddresses();

    void cacheTrackedWalletAddresses(Set<String> addresses);

    void invalidateWalletCache();
}
