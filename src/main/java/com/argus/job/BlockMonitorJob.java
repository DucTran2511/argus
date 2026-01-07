package com.argus.job;

import com.argus.domain.model.Transaction;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.cache.BlockTrackingPort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockMonitorJob {

    private static final int MAX_CATCHUP_BLOCKS = 100;

    private final BlockChainPort blockChainPort;
    private final TransactionPersistencePort transactionPersistencePort;
    private final BlockTrackingPort blockTrackingPort;
    private final WalletPersistencePort walletPersistencePort;

    public void execute() {
        long startTime = System.currentTimeMillis();
        log.info("=== BlockMonitorJob started ===");

        try {
            long currentBlock = blockChainPort.getLatestBlockNumber();
            long lastProcessed = getLastProcessedBlock(currentBlock);

            if (lastProcessed >= currentBlock) {
                log.debug("No new blocks. Current: {}, Last processed: {}", currentBlock, lastProcessed);
                return;
            }

            Set<String> trackedWallets = getTrackedWalletAddresses();
            if (trackedWallets.isEmpty()) {
                log.debug("No tracked wallets. Updating cursor to current block.");
                blockTrackingPort.setLastProcessedBlock(currentBlock);
                return;
            }

            log.info("Processing blocks {} to {} ({} blocks), tracking {} wallets",
                    lastProcessed + 1, currentBlock, currentBlock - lastProcessed, trackedWallets.size());

            int totalSaved = 0;
            for (long blockNum = lastProcessed + 1; blockNum <= currentBlock; blockNum++) {
                int saved = processBlock(blockNum, trackedWallets);
                totalSaved += saved;
                blockTrackingPort.setLastProcessedBlock(blockNum);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== BlockMonitorJob completed in {}ms. Saved {} transactions ===", elapsed, totalSaved);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("BlockMonitorJob failed after {}ms: {}", elapsed, e.getMessage(), e);
        }
    }

    private long getLastProcessedBlock(long currentBlock) {
        return blockTrackingPort.getLastProcessedBlock()
                .map(last -> {
                    long gap = currentBlock - last;
                    if (gap > MAX_CATCHUP_BLOCKS) {
                        log.warn("Gap too large ({} blocks). Skipping to recent. Was: {}, Now: {}",
                                gap, last, currentBlock - MAX_CATCHUP_BLOCKS);
                        return currentBlock - MAX_CATCHUP_BLOCKS;
                    }
                    return last;
                })
                .orElseGet(() -> {
                    log.info("No cursor found. Starting from current block: {}", currentBlock);
                    return currentBlock;
                });
    }

    private Set<String> getTrackedWalletAddresses() {
        Set<String> cached = blockTrackingPort.getTrackedWalletAddresses();
        if (!cached.isEmpty()) {
            return cached;
        }

        log.debug("Cache miss - fetching wallet addresses from DB");
        Set<String> addresses = walletPersistencePort.getAllAddresses();
        if (!addresses.isEmpty()) {
            blockTrackingPort.cacheTrackedWalletAddresses(addresses);
        }
        return addresses;
    }

    private int processBlock(long blockNum, Set<String> trackedWallets) {
        Set<String> lowerCaseAddresses = trackedWallets.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Transaction> transactions = blockChainPort.getTransactionsByBlock(blockNum);
        if (transactions.isEmpty()) {
            return 0;
        }

        int saved = 0;
        for (Transaction tx : transactions) {
            if (isRelevantTransaction(tx, lowerCaseAddresses)) {
                try {
                    transactionPersistencePort.save(tx);
                    saved++;
                } catch (Exception e) {
                    log.debug("Skipped duplicate or failed tx: {}", tx.getTxHash());
                }
            }
        }

        if (saved > 0) {
            log.debug("Block {}: saved {}/{} transactions", blockNum, saved, transactions.size());
        }
        return saved;
    }

    private boolean isRelevantTransaction(Transaction tx, Set<String> lowerWallets) {
        String from = tx.getFrom() != null ? tx.getFrom().toLowerCase() : "";
        String to = tx.getTo() != null ? tx.getTo().toLowerCase() : "";

        return lowerWallets.contains(from) || lowerWallets.contains(to);
    }
}
