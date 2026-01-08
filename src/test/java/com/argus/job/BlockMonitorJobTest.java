package com.argus.job;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.cache.BlockTrackingPort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletPersistencePort;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class BlockMonitorJobTest {

    @Mock
    BlockChainPort blockChainPort;
    @Mock
    TransactionPersistencePort transactionPersistencePort;
    @Mock
    BlockTrackingPort blockTrackingPort;
    @Mock
    WalletPersistencePort walletPersistencePort;

    @InjectMocks
    BlockMonitorJob blockMonitorJob;

    @Test
    void execute_noNewBlocks_skips() {
        when(blockChainPort.getLatestBlockNumber()).thenReturn(100L);
        when(blockTrackingPort.getLastProcessedBlock()).thenReturn(Optional.of(100L));

        blockMonitorJob.execute();

        verify(blockChainPort, never()).getTransactionsByBlock(anyLong());
    }

    @Test
    void execute_noTrackedWallets_updatesCursor() {
        when(blockChainPort.getLatestBlockNumber()).thenReturn(101L);
        when(blockTrackingPort.getLastProcessedBlock()).thenReturn(Optional.of(100L));
        when(blockTrackingPort.getTrackedWalletAddresses()).thenReturn(Set.of());
        when(walletPersistencePort.getAllAddresses()).thenReturn(Set.of());

        blockMonitorJob.execute();

        verify(blockTrackingPort).setLastProcessedBlock(101L);
    }
}
